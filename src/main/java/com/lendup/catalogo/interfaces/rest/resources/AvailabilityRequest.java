package com.lendup.catalogo.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="AvailabilityRequest",description="Campos admitidos por availability; campos ausentes en operaciones de actualización se conservan.")
public record AvailabilityRequest(
  @Schema(description="desde", example="2026-10-05T09:00:00", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String desde,
  @Schema(description="hasta", example="2026-10-07T18:00:00", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String hasta
) {}
