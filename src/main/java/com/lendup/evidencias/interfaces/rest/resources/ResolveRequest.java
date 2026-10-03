package com.lendup.evidencias.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="ResolveRequest",description="Campos admitidos por resolve; campos ausentes en operaciones de actualización se conservan.")
public record ResolveRequest(
  @Schema(description="justificacion_resolucion", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String justificacion_resolucion,
  @Schema(description="decision_garantia", allowableValues={"SIN_AFECTACION","AFECTACION_PARCIAL","AFECTACION_TOTAL"}, requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String decision_garantia,
  @Schema(description="monto_garantia_afectado", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull java.math.BigDecimal monto_garantia_afectado,
  @Schema(description="saldo_garantia_previsto", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull java.math.BigDecimal saldo_garantia_previsto,
  @Schema(description="moneda") String moneda
) {}
