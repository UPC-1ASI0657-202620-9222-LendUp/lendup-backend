package com.lendup.pagos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="PaymentRequest",description="Campos admitidos por payment; campos ausentes en operaciones de actualización se conservan.")
public record PaymentRequest(
  @Schema(description="prestamo_id", example="UUID_PRESTAMO", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String prestamo_id,
  @Schema(description="cambio_fecha_prestamo_id") String cambio_fecha_prestamo_id,
  @Schema(description="tipo", allowableValues={"PAGO_TARIFA","PAGO_EXTENSION"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String tipo,
  @Schema(description="monto", example="10.00", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull java.math.BigDecimal monto,
  @Schema(description="medio_pago_seleccionado", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String medio_pago_seleccionado,
  @Schema(description="clave_idempotencia", example="pago-UUID_UNICO", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String clave_idempotencia
) {}
