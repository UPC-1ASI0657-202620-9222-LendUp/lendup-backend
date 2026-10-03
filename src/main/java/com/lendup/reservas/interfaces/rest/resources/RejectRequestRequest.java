package com.lendup.reservas.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="RejectRequestRequest",description="Campos admitidos por rejectRequest; campos ausentes en operaciones de actualización se conservan.")
public record RejectRequestRequest(
  @Schema(description="motivo_rechazo") String motivo_rechazo
) {}
