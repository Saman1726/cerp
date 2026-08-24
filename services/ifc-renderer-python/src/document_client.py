"""HTTP client for the document-service API.

The document-service exposes three endpoints used by the IFC renderer:

  1. POST /api/v1/media/presign      – returns a presigned PUT URL + objectKey
  2. POST /api/v1/media/download/internal – server-side download, returns bytes
  3. POST /api/v1/media/presign-download – returns a presigned GET URL

All calls target the internal endpoint (e.g. http://document-service-java:8092)
so they never leave the Docker network.
"""

from __future__ import annotations

import logging
from typing import Optional

import httpx

logger = logging.getLogger(__name__)


class DocumentClient:
    """Thin wrapper around the document-service HTTP API."""

    def __init__(self, base_url: str, auth_token: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.auth_token = auth_token
        self._client: httpx.Client | None = None

    @property
    def client(self) -> httpx.Client:
        if self._client is None:
            headers = {}
            if self.auth_token:
                headers["Authorization"] = f"Bearer {self.auth_token}"
            self._client = httpx.Client(
                base_url=self.base_url,
                headers=headers,
                timeout=httpx.Timeout(30.0, connect=10.0),
            )
        return self._client

    def close(self) -> None:
        if self._client is not None:
            self._client.close()
            self._client = None

    # ------------------------------------------------------------------
    # 1. Presign PUT — get a URL and objectKey for storing a file
    # ------------------------------------------------------------------

    def presign_upload(
        self,
        file_key: str,
        content_type: str = "application/octet-stream",
    ) -> tuple[str, str]:
        """Request a presigned PUT URL and the resulting objectKey.

        Returns
        -------
        (server_upload_url, object_key)
        """
        response = self.client.post("/api/v1/media/presign", json={
            "fileKey": file_key,
            "contentType": content_type,
        })
        response.raise_for_status()
        data = response.json()
        return data["serverUploadUrl"], data["objectKey"]

    # ------------------------------------------------------------------
    # 2. Upload — PUT file bytes to a presigned URL
    # ------------------------------------------------------------------

    def upload_to_presigned_url(
        self, url: str, data: bytes, content_type: str = "application/octet-stream"
    ) -> None:
        """PUT *data* to the given presigned PUT URL."""
        response = self.client.put(
            url,
            content=data,
            headers={"Content-Type": content_type},
            timeout=httpx.Timeout(300.0, connect=30.0),  # 5 min for large files
        )
        response.raise_for_status()
        logger.info(
            "Uploaded %d bytes → presigned URL (HTTP %d)", len(data), response.status_code
        )

    # ------------------------------------------------------------------
    # 3. Download — server-side download via internal endpoint
    # ------------------------------------------------------------------

    def download_internal(self, object_key: str) -> bytes:
        """Download file content via the internal download endpoint."""
        response = self.client.post(
            "/api/v1/media/download/internal",
            json={"objectKey": object_key},
            timeout=httpx.Timeout(120.0, connect=10.0),
        )
        response.raise_for_status()
        return response.content

    # ------------------------------------------------------------------
    # 4. Presign GET — get a presigned download URL for a stored object
    # ------------------------------------------------------------------

    def presign_download(self, object_key: str) -> tuple[str, str]:
        """Return (presigned_get_url, expires_at) for an existing object."""
        response = self.client.post(
            "/api/v1/media/presign-download",
            json={"objectKey": object_key},
        )
        response.raise_for_status()
        data = response.json()
        return data["url"], data["expiresAt"]