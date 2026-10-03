package com.lendup.prestamos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="RescheduleRequest",description="Campos admitidos por reschedule; campos ausentes en operaciones de actualización se conservan.")
public record RescheduleRequest(
  @Schema(description="fecha_propuesta_en", example="2026-10-09T18:00:00", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String fecha_propuesta_en
) {}
