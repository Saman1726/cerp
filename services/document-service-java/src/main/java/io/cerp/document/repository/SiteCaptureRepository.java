package io.cerp.document.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cerp.document.domain.SiteCapture;

public interface SiteCaptureRepository extends JpaRepository<SiteCapture, UUID> {
}
