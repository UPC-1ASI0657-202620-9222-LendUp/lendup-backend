package com.lendup.identidad.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="VerifyRequest",description="Campos admitidos por verify; campos ausentes en operaciones de actualización se conservan.")
public record VerifyRequest(
  @Schema(description="verificacion_referencia", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String verificacion_referencia
) {}
