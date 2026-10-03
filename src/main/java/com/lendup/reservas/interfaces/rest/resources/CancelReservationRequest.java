package com.lendup.reservas.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="CancelReservationRequest",description="Campos admitidos por cancelReservation; campos ausentes en operaciones de actualización se conservan.")
public record CancelReservationRequest(
  @Schema(description="motivo_cancelacion") String motivo_cancelacion
) {}
