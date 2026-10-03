package com.lendup.prestamos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="RescheduleResponseRequest",description="Campos admitidos por rescheduleResponse; campos ausentes en operaciones de actualización se conservan.")
public record RescheduleResponseRequest(
  @Schema(description="estado", allowableValues={"RECHAZADA","APLICADA"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String estado,
  @Schema(description="motivo_rechazo") String motivo_rechazo
) {}
