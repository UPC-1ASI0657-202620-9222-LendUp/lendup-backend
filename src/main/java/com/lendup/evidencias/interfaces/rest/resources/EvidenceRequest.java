package com.lendup.evidencias.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="EvidenceRequest",description="Campos admitidos por evidence; campos ausentes en operaciones de actualización se conservan.")
public record EvidenceRequest(
  @Schema(description="etapa", allowableValues={"ENTREGA","DEVOLUCION","INCIDENCIA"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String etapa,
  @Schema(description="tipo", allowableValues={"FOTO","VIDEO","OBSERVACION"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String tipo,
  @Schema(description="incidencia_id") String incidencia_id,
  @Schema(description="url") String url,
  @Schema(description="cloudinary_public_id") String cloudinary_public_id,
  @Schema(description="observacion") String observacion
) {}
