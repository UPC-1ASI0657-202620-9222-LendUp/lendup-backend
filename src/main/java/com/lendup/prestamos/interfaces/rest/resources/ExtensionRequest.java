package com.lendup.prestamos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="ExtensionRequest",description="Campos admitidos por extension; campos ausentes en operaciones de actualización se conservan.")
public record ExtensionRequest(
  @Schema(description="fecha_propuesta_en", example="2026-10-09T18:00:00", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String fecha_propuesta_en,
  @Schema(description="costo_adicional", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull java.math.BigDecimal costo_adicional,
  @Schema(description="moneda", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String moneda
) {}
