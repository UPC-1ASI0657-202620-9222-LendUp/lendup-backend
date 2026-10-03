package com.lendup.pagos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="GuaranteeRequest",description="Campos admitidos por guarantee; campos ausentes en operaciones de actualización se conservan.")
public record GuaranteeRequest(
  @Schema(description="prestamo_id", example="UUID_PRESTAMO", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String prestamo_id,
  @Schema(description="medio_pago_seleccionado") String medio_pago_seleccionado,
  @Schema(description="clave_idempotencia", example="pago-UUID_UNICO") String clave_idempotencia
) {}
