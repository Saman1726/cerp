package io.cerp.document.dto;

import jakarta.validation.constraints.NotBlank;

public record DownloadForServerSideRequest(
    @NotBlank String objectKey
) {}