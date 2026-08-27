"""Core IFC processing engine using ifcopenshell.
Handles: IFC parsing, preview, export, and Blender rendering.
File I/O is delegated to the document-service via DocumentClient.
"""

from __future__ import annotations

import io, logging, math, os, subprocess, tempfile, time, uuid
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import BinaryIO
import ifcopenshell, ifcopenshell.geom, numpy as np, trimesh
from PIL import Image, ImageDraw
from src.config import BLENDER_PATH, MAX_UPLOAD_SIZE, PREVIEW_SIZE
from src.document_client import DocumentClient
from src.models import ExportFormat, IFCMetadata
logger = logging.getLogger(__name__)


def _ensure_ifc_import():
    try:
        import ifcopenshell  # noqa: F401
        return True
    except Exception:
        return False


def _ensure_blender():
    return os.path.isfile(BLENDER_PATH) and os.access(BLENDER_PATH, os.X_OK)


def upload_ifc(file: BinaryIO, original_filename: str,
               content_type: str = "application/ifc") -> dict:
    """Upload an IFC file to document-service."""
    raw = file.read()
    if len(raw) == 0:
        raise ValueError("Uploaded file is empty")
    if len(raw) > MAX_UPLOAD_SIZE:
        raise ValueError(f"File size ({len(raw)} bytes) exceeds limit")
    file_id = str(uuid.uuid4())
    original_name = Path(original_filename).name
    stem = Path(original_name).stem or f"ifc_{file_id}"
    if not original_name.lower().endswith(".ifc"):
        original_name = f"{stem}.ifc"
    from src.config import DOCUMENT_SERVICE_URL
    client = DocumentClient(DOCUMENT_SERVICE_URL)
    try:
        file_key = f"ifc/{stem}/{file_id}/{original_name}"
        surl, okey = client.presign_upload(
            file_key=file_key, content_type=content_type)
        client.upload_to_presigned_url(
            url=surl, data=raw, content_type=content_type)
    finally:
        client.close()
    uploaded_at = datetime.now(timezone.utc)
    return {
        "file_id": file_id, "original_filename": original_name,
        "stem": stem, "content_type": content_type,
        "file_size": len(raw), "file_key": okey,
        "object_key": okey, "uploaded_at": uploaded_at.isoformat(),
        "status": "DONE",
        "preview_url": f"/api/v1/ifc/{file_id}/preview",
        "export_url": f"/api/v1/ifc/{file_id}/export",
        "blender_render_url": f"/api/v1/ifc/{file_id}/blender-render",
        "metadata_url": f"/api/v1/ifc/{file_id}/metadata",
    }
# test include: '"IfcWall", "IfcSlab", "IfcStair", "IfcRoof", "IfcColumn", "IfcBeam", "IfcWindow", "IfcDoor"'
def _open_ifc(ifc_bytes: bytes):
    """Parse raw IFC-SPF bytes into an ifcopenshell.file.

    ifcopenshell.open() only accepts a filesystem path (no file-like/bytes
    argument), and the returned ifcopenshell.file has no context-manager
    support, so files must be decoded to text and loaded via from_string().
    """
    ifc_text = ifc_bytes.decode("ISO-8859-1")
    return ifcopenshell.file.from_string(ifc_text)


def get_metadata(ifc_bytes: bytes) -> IFCMetadata:
    """Parse IFC bytes and extract structured metadata."""
    logger.info("Extracting metadata (%d bytes)", len(ifc_bytes))
    try:
        ifc_file = _open_ifc(ifc_bytes)
        proj = (ifc_file.by_type("IfcProject") or [None])[0]
        name = proj.Name or "" if proj else ""
        desc = proj.Description or "" if proj else ""
        counts: dict[str, int] = defaultdict(int)
        for ent in ifc_file:
            counts[ent.is_a()] += 1
        top = dict(sorted(counts.items(), key=lambda x: x[1], reverse=True)[:10])
        return IFCMetadata(
            file_version=ifc_file.schema, name=name, description=desc,
            units=getattr(ifc_file.units, "name", "METER"),
            total_elements=sum(counts.values()), building_name=name,
            storeys=len(ifc_file.by_type("IfcBuildingStorey")),
            elements_by_type=top)
    except Exception:
        logger.exception("Metadata extraction failed")
        raise


_GEOM_INCLUDE_TYPES = [
    "IfcWall", "IfcSlab", "IfcStair", "IfcRoof",
    "IfcColumn", "IfcBeam", "IfcWindow", "IfcDoor"]


def _iter_meshes(ifc_file) -> list["trimesh.Trimesh"]:
    """Iterate building-element geometry and return a list of trimeshes.

    ifcopenshell.geom exposes shape generation via the module-level
    iterate()/iterator() generator API (there is no create_geometry()
    function on this ifcopenshell version). Each yielded shape carries
    flat vertex/face buffers on shape.geometry that are already
    triangulated.
    """
    settings = ifcopenshell.geom.settings()
    settings.set("use-world-coords", True)
    meshes: list[trimesh.Trimesh] = []
    for shape in ifcopenshell.geom.iterate(
            settings, ifc_file, include=_GEOM_INCLUDE_TYPES):
        geometry = shape.geometry
        verts = np.array(geometry.verts, dtype=float).reshape(-1, 3)
        faces = np.array(geometry.faces, dtype=int).reshape(-1, 3)
        if len(verts) == 0 or len(faces) == 0:
            continue
        meshes.append(trimesh.Trimesh(
            vertices=verts, faces=faces, process=False))
    return meshes


def generate_preview(ifc_bytes: bytes,
                     target_size: tuple[int, int] | None = None) -> bytes:
    """Generate a 2D preview PNG from IFC geometry."""
    target_size = target_size or PREVIEW_SIZE
    try:
        ifc_file = _open_ifc(ifc_bytes)
        meshes = _iter_meshes(ifc_file)
        if not meshes:
            return _placeholder_preview(target_size)
        av = np.vstack([m.vertices for m in meshes])
        if len(av) < 2:
            return _placeholder_preview(target_size)
        xs, ys, zs = av[:, 0], av[:, 1], av[:, 2]
        canvas = Image.new("RGB", target_size, "white")
        draw = ImageDraw.Draw(canvas)
        pad = 8
        pw = target_size[0] - 2 * pad
        ph = target_size[1] - 2 * pad
        def project(pt):
            x, y = pt
            sx = max(pad, min(target_size[0] - 1 - pad,
                int(pad + (x - xs.min()) / (xs.max() - xs.min() or 1) * pw)))
            sy = max(pad, min(target_size[1] - 1 - pad,
                int(pad + (y - ys.min()) / (ys.max() - ys.min() or 1) * ph)))
            return sx, sy
        for mesh in meshes:
            v = mesh.vertices
            proj = [project((v[i, 0], v[i, 1])) for i in range(len(v))]
            for j in range(len(proj) - 1):
                if (abs(proj[j+1][0] - proj[j][0]) < 200
                        and abs(proj[j+1][1] - proj[j][1]) < 200):
                    zn = (v[j, 2] - zs.min()) / (zs.max() - zs.min() or 1)
                    draw.line(list(proj[j]) + list(proj[j+1]),
                        fill=(int(100+155*zn), int(180+75*(1-zn)),
                              int(220-100*zn)), width=1)
        buf = io.BytesIO()
        canvas.save(buf, format="PNG")
        return buf.getvalue()
    except Exception:
        logger.exception("Preview failed")
        return _placeholder_preview(target_size)


def _placeholder_preview(size: tuple[int, int]) -> bytes:
    img = Image.new("RGB", size, color="#f0f0f0")
    draw = ImageDraw.Draw(img)
    draw.text((size[0]//2 - 40, size[1]//2 - 10),
              "No geometry", fill="#999999")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def export_3d_model(ifc_bytes: bytes, fmt: ExportFormat) -> bytes:
    """Export IFC geometry to a 3D format via trimesh."""
    logger.info("Exporting IFC -> %s (%d bytes)", fmt.value, len(ifc_bytes))
    try:
        ifc_file = _open_ifc(ifc_bytes)
        meshes = _iter_meshes(ifc_file)
        if not meshes:
            raise ValueError("No geometry found for export")
        av, af, fo = [], [], 0
        for m in meshes:
            av.append(m.vertices)
            af.append(m.faces + fo)
            fo += len(m.vertices)
        mesh = trimesh.Trimesh(
            vertices=np.vstack(av),
            faces=np.vstack(af) if af else np.array([]),
            process=False)
        buf = io.BytesIO()
        if fmt == ExportFormat.OBJ:
            mesh.export(buf, file_type="obj")
        elif fmt == ExportFormat.STL:
            mesh.export(buf, file_type="stl")
        elif fmt == ExportFormat.GLTF:
            mesh.export(buf, file_type="gltf")
        elif fmt == ExportFormat.GLB:
            mesh.export(buf, file_type="glb")
        buf.seek(0)
        return buf.read()
    except Exception:
        logger.exception("3D export failed")
        raise


def render_with_blender(
        ifc_bytes: bytes,
        resolution_x: int = 1920, resolution_y: int = 1080,
        samples: int = 256) -> bytes:
    """Render the IFC model with Blender and return PNG bytes."""
    logger.info("Blender render: %dx%d, %d samples",
                resolution_x, resolution_y, samples)
    try:
        obj_data = export_3d_model(ifc_bytes, ExportFormat.OBJ)
        blend_dir = Path(tempfile.mkdtemp(prefix="ifc_blender_"))
        obj_path = blend_dir / "model.obj"
        obj_path.write_bytes(obj_data)
        bs = blend_dir / "render.py"
        out_png = blend_dir / "render.png"
        _write_blender_script(str(bs), str(obj_path),
                              str(out_png), resolution_x, resolution_y, samples)
        cmd = [BLENDER_PATH, "--background", "--python", str(bs),
               "--", str(obj_path), str(out_png),
               str(resolution_x), str(resolution_y), str(samples)]
        result = subprocess.run(cmd, capture_output=True,
                                text=True, timeout=600)
        if result.returncode != 0:
            logger.error("Blender failed: %s", result.stderr[-1000:])
            raise RuntimeError(f"Blender failed: {result.stderr[-1000:]}")
        if not out_png.exists():
            raise FileNotFoundError("Blender did not produce output")
        try:
            blend_dir.rmdir()
        except OSError:
            pass
        return out_png.read_bytes()
    except Exception:
        logger.exception("Blender render failed")
        raise
def _write_blender_script(script_path: str, obj_path: str,
                          output_png: str,
                          res_x: int, res_y: int, n_samples: int):
    """Write a Python script that Blender executes."""
    lines = []
    Q = chr(34)
    lines.append(Q*3 + "Blender render script." + Q*3)
    lines.append("import sys")
    lines.append("import bpy")
    lines.append("")
    lines.append("def main():")
    lines.append('    args = sys.argv[sys.argv.index("--") + 1:]')
    lines.append("    obj_path, output_png = args[0], args[1]")
    lines.append("    res_x, res_y = int(args[2]), int(args[3])")
    lines.append("    n_samples = int(args[4])")
    lines.append('    bpy.ops.object.select_all(action="SELECT")')
    lines.append("    bpy.ops.object.delete()")
    lines.append("    scene = bpy.context.scene")
    lines.append('    scene.render.image_format = "PNG"')
    lines.append("    scene.render.film_transparent = True")
    lines.append("    scene.render.resolution_x = res_x")
    lines.append("    scene.render.resolution_y = res_y")
    lines.append("    scene.render.resolution_percentage = 100")
    lines.append("    scene.render.filepath = output_png")
    lines.append("    scene.render.tile_x = 256")
    lines.append("    scene.render.tile_y = 256")
    lines.append('    scene.render.engine = "CYCLES"')
    lines.append("    scene.cycles.samples = n_samples")
    lines.append("    scene.cycles.preview_samples = min(n_samples, 64)")
    lines.append('    scene.cycles.device = "CPU"')
    lines.append("    scene.cycles.filter_width = 1.5")
    lines.append("    for nm, en, rot in [")
    lines.append('        ("KeyLight", 3.0, (0.8, 0.5, 0.0)),')
    lines.append('        ("FillLight", 1.0, (-0.5, 1.0, 3.0)),')
    lines.append('        ("SkyLight", 0.5, (3.0, 0.0, 0.0)),')
    lines.append("    ]:")
    lines.append('        ld = bpy.data.lights.new(nm, "SUN")')
    lines.append("        ld.energy = en")
    lines.append('        ld.angle = 0.5 if nm != "SkyLight" else 3.14159 * 0.48')
    lines.append("        lo = bpy.data.objects.new(nm, ld)")
    lines.append("        lo.rotation_euler = rot")
    lines.append("        scene.collection.objects.link(lo)")
    lines.append("    bpy.ops.import_scene.obj(filepath=obj_path)")
    lines.append('    meshes = [o for o in scene.collection.objects if o.type == "MESH"]')
    lines.append("    if meshes:")
    lines.append("        bpy.context.view_layer.objects.active = meshes[0]")
    lines.append("        meshes[0].select_set(True)")
    lines.append("        bpy.ops.view3d.camera_to_view_selected()")
    lines.append("        for o in meshes: o.select_set(False)")
    lines.append('    print(f"Rendering {res_x}x{res_y} at {n_samples} samples")')
    lines.append("    bpy.ops.render.render(write_studio=True, animation=False)")
    lines.append('    print(f"Saved: {output_png}")')
    lines.append("main()")
    Path(script_path).write_text(chr(10).join(lines) + chr(10))