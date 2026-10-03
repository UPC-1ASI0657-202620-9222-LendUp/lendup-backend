package com.lendup.evidencias.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="IncidentRequest",description="Campos admitidos por incident; campos ausentes en operaciones de actualización se conservan.")
public record IncidentRequest(
  @Schema(description="prestamo_id", example="UUID_PRESTAMO", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String prestamo_id,
  @Schema(description="tipo", allowableValues={"DANIO","PERDIDA","RETRASO","NO_DEVOLUCION","OTRO"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String tipo,
  @Schema(description="descripcion", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String descripcion
) {}
