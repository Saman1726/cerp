"""CERP IFC Renderer API — FastAPI application."""

from __future__ import annotations

import io
import logging
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, File, Form, HTTPException, Query, UploadFile
from fastapi.responses import Response

from src.config import DOCUMENT_SERVICE_URL
from src.document_client import DocumentClient
from src.ifc_handler import (
    export_3d_model,
    generate_preview,
    get_metadata,
    render_with_blender,
    upload_ifc,
)
from src.models import (
    BlenderRenderRequest,
    BlenderRenderResponse,
    ExportFormat,
    ExportRequest,
    ExportResponse,
    HealthResponse,
    UploadResponse,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="CERP IFC Renderer",
    description="Parse IFC building models, extract metadata, generate previews & 3D exports.",
    version="0.1.0",
)


def _doc_client() -> DocumentClient:
    return DocumentClient(DOCUMENT_SERVICE_URL)


@app.get("/health", response_model=HealthResponse)
def health_check():
    from src.ifc_handler import _ensure_blender, _ensure_ifc_import
    try:
        dc = _doc_client()
        dc.client.get("/api/v1/media/presign")
        dc.close()
        doc_ok = True
    except Exception:
        doc_ok = False
    return HealthResponse(
        status="healthy" if doc_ok else "degraded",
        ifcopenshell_available=_ensure_ifc_import(),
        blender_available=_ensure_blender(),
        document_service_available=doc_ok,
    )


@app.post("/api/v1/ifc/upload", response_model=UploadResponse)
async def upload_ifc_file(
    file: UploadFile = File(..., description="IFC file to upload"),
    project_id: str | None = Form(None, description="Optional project ID"),
):
    """Upload an IFC file to document-service for processing."""
    try:
        result = upload_ifc(file.file, file.filename or "model.ifc",
                            file.content_type or "application/ifc")
        return UploadResponse(
            file_id=result["file_id"],
            original_filename=result["original_filename"],
            content_type=result["content_type"],
            file_size=result["file_size"],
            uploaded_at=result["uploaded_at"],
            status=result["status"],
            preview_url=result["preview_url"],
            export_url=result["export_url"],
            blender_render_url=result["blender_render_url"],
            metadata_url=result["metadata_url"],
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception:
        logger.exception("Upload failed")
        raise HTTPException(status_code=500, detail="Upload failed")


@app.get("/api/v1/ifc/{file_id}/metadata")
def get_metadata_route(file_id: str, file_key: str = Query(...)):
    """Retrieve metadata extracted from the IFC file."""
    client = _doc_client()
    try:
        ifc_bytes = client.download_internal(file_key)
        metadata = get_metadata(ifc_bytes)
        return metadata.model_dump()
    except Exception:
        raise HTTPException(status_code=500, detail="Metadata extraction failed")
    finally:
        client.close()


@app.get("/api/v1/ifc/{file_id}/preview")
def get_preview_route(file_id: str, file_key: str = Query(...)):
    """Return the 2D preview image."""
    client = _doc_client()
    try:
        ifc_bytes = client.download_internal(file_key)
        preview_data = generate_preview(ifc_bytes)
        return Response(content=preview_data, media_type="image/png")
    except Exception:
        raise HTTPException(status_code=500, detail="Preview generation failed")
    finally:
        client.close()


@app.post("/api/v1/ifc/{file_id}/export", response_model=ExportResponse)
def request_export(
    file_id: str,
    export_req: ExportRequest,
    file_key: str = Query(...),
):
    """Export the IFC geometry to glTF, glb, OBJ, or STL."""
    client = _doc_client()
    try:
        ifc_bytes = client.download_internal(file_key)
        export_data = export_3d_model(ifc_bytes, export_req.format)
        stem = Path(export_req.file_id).stem
        out_key = f"exports/{file_id}/{stem}.{export_req.format.value}"
        surl, _ = client.presign_upload(out_key)
        client.upload_to_presigned_url(surl, export_data,
                                       "application/octet-stream")
        purl, _ = client.presign_download(out_key)
        return ExportResponse(file_id=file_id, format=export_req.format.value,
                              output_url=purl)
    except Exception:
        raise HTTPException(status_code=500, detail="Export failed")
    finally:
        client.close()


@app.post("/api/v1/ifc/{file_id}/blender-render",
          response_model=BlenderRenderResponse)
def request_blender_render(
    file_id: str,
    render_req: BlenderRenderRequest | None = None,
    file_key: str = Query(...),
):
    """Render the IFC model with Blender and return the PNG."""
    client = _doc_client()
    try:
        ifc_bytes = client.download_internal(file_key)
        rx = render_req.resolution_x if render_req else 1920
        ry = render_req.resolution_y if render_req else 1080
        sp = render_req.samples if render_req else 256
        png_data = render_with_blender(ifc_bytes, rx, ry, sp)
        out_key = f"renders/{file_id}/render.png"
        surl, _ = client.presign_upload(out_key, "image/png")
        client.upload_to_presigned_url(surl, png_data, "image/png")
        purl, _ = client.presign_download(out_key)
        return BlenderRenderResponse(file_id=file_id, resolution=(rx, ry),
                                     samples=sp, output_url=purl)
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception:
        raise HTTPException(status_code=500, detail="Render failed")
    finally:
        client.close()