"""CERP IFC Renderer API — FastAPI application."""

from __future__ import annotations

import io
import logging
import uuid
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, File, Form, HTTPException, Query, UploadFile, Request
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


def _doc_client(auth_token: Optional[str] = None, tenant_id: Optional[str] = None) -> DocumentClient:
    return DocumentClient(DOCUMENT_SERVICE_URL, auth_token, tenant_id)


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


@app.post("/api/v1/ifc/register")
def register_ifc_file(
    file_key: str = Query(..., description="S3 key of the already-uploaded IFC file"),
    project_id: str = Query(..., description="Project/site ID"),
):
    """Register an already-uploaded IFC file and return a file_id for subsequent calls.
    
    This endpoint does NOT upload the file again — the file must already exist
    at *file_key* in the document service (e.g. uploaded by the project service
    via a presigned URL). It simply generates a UUID and returns it so that
    downstream preview / render / export calls can address the file by ID.
    """
    file_id = str(uuid.uuid4())
    return {
        "file_id": file_id,
        "file_key": file_key,
        "project_id": project_id,
    }


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
def get_metadata_route(file_id: str, file_key: str = Query(...), request: Request = None):
    """Retrieve metadata extracted from the IFC file."""
    tenant_id = None
    if request:
        tenant_id = request.headers.get("X-Tenant-Id")
    # Use None for auth_token — download_internal() creates an unauthenticated
    # client for the internal server-to-server endpoint to avoid triggering the
    # document service's OAuth2 JWT filter.
    client = _doc_client(None, tenant_id)
    try:
        ifc_bytes = client.download_internal(file_key)
        metadata = get_metadata(ifc_bytes)
        return metadata.model_dump()
    except Exception:
        logger.exception("Metadata extraction failed for file_id=%s file_key=%s", file_id, file_key)
        raise HTTPException(status_code=500, detail="Metadata extraction failed")
    finally:
        client.close()


@app.get("/api/v1/ifc/{file_id}/preview")
def get_preview_route(file_id: str, file_key: str = Query(...), request: Request = None):
    """Return the 2D preview image."""
    tenant_id = None
    if request:
        tenant_id = request.headers.get("X-Tenant-Id")
    # Use None for auth_token — download_internal() creates an unauthenticated
    # client for the internal server-to-server endpoint to avoid triggering the
    # document service's OAuth2 JWT filter.
    client = _doc_client(None, tenant_id)
    try:
        ifc_bytes = client.download_internal(file_key)
        preview_data = generate_preview(ifc_bytes)
        return Response(content=preview_data, media_type="image/png")
    except Exception:
        logger.exception("Preview generation failed for file_id=%s file_key=%s", file_id, file_key)
        raise HTTPException(status_code=500, detail="Preview generation failed")
    finally:
        client.close()


@app.post("/api/v1/ifc/{file_id}/export", response_model=ExportResponse)
def request_export(
    file_id: str,
    export_req: ExportRequest,
    file_key: str = Query(...),
    request: Request = None,
):
    """Export the IFC geometry to glTF, glb, OBJ, or STL."""
    tenant_id = None
    if request:
        tenant_id = request.headers.get("X-Tenant-Id")
    # Use None for auth_token — download_internal() creates an unauthenticated
    # client for the internal server-to-server endpoint to avoid triggering the
    # document service's OAuth2 JWT filter.
    client = _doc_client(None, tenant_id)
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
        logger.exception("Export failed for file_id=%s file_key=%s", file_id, file_key)
        raise HTTPException(status_code=500, detail="Export failed")
    finally:
        client.close()


@app.post("/api/v1/ifc/{file_id}/blender-render",
          response_model=BlenderRenderResponse)
def request_blender_render(
    file_id: str,
    render_req: BlenderRenderRequest | None = None,
    file_key: str = Query(...),
    request: Request = None,
):
    """Render the IFC model with Blender and return the PNG."""
    tenant_id = None
    if request:
        tenant_id = request.headers.get("X-Tenant-Id")
    # Use None for auth_token — download_internal() creates an unauthenticated
    # client for the internal server-to-server endpoint to avoid triggering the
    # document service's OAuth2 JWT filter.
    client = _doc_client(None, tenant_id)
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
        logger.exception("Blender render unavailable for file_id=%s file_key=%s", file_id, file_key)
        raise HTTPException(status_code=503, detail=str(e))
    except Exception:
        logger.exception("Render failed for file_id=%s file_key=%s", file_id, file_key)
        raise HTTPException(status_code=500, detail="Render failed")
    finally:
        client.close()