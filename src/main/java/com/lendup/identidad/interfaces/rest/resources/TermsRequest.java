package com.lendup.identidad.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="TermsRequest",description="Campos admitidos por terms; campos ausentes en operaciones de actualización se conservan.")
public record TermsRequest(
  @Schema(description="version_terminos_aceptada", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String version_terminos_aceptada,
  @Schema(description="version_descargo_aceptada", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String version_descargo_aceptada
) {}
