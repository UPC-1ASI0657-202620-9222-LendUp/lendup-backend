package com.lendup.prestamos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="ExtensionResponseRequest",description="Campos admitidos por extensionResponse; campos ausentes en operaciones de actualización se conservan.")
public record ExtensionResponseRequest(
  @Schema(description="estado", allowableValues={"RECHAZADA","ACEPTADA_PENDIENTE_PAGO"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String estado,
  @Schema(description="motivo_rechazo") String motivo_rechazo
) {}
