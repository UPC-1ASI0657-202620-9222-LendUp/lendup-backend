package com.lendup.evidencias.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="AnalyzeRequest",description="Campos admitidos por analyze; campos ausentes en operaciones de actualización se conservan.")
public record AnalyzeRequest(
  @Schema(description="evidencia_inicial_id", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String evidencia_inicial_id,
  @Schema(description="evidencia_final_id", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String evidencia_final_id,
  @Schema(description="incidencia_id") String incidencia_id
) {}
