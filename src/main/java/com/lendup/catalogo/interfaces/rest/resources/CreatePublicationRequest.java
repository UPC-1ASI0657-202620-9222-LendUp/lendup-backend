package com.lendup.catalogo.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="CreatePublicationRequest",description="Campos admitidos por createPublication; campos ausentes en operaciones de actualización se conservan.")
public record CreatePublicationRequest(
  @Schema(description="categoria_id", example="00000000-0000-4000-8000-000000000003", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String categoria_id,
  @Schema(description="titulo", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String titulo,
  @Schema(description="descripcion", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String descripcion,
  @Schema(description="condicion_objeto", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String condicion_objeto,
  @Schema(description="universidad", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String universidad,
  @Schema(description="campus", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String campus,
  @Schema(description="ubicacion", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String ubicacion,
  @Schema(description="lugar_intercambio", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String lugar_intercambio,
  @Schema(description="latitud") java.math.BigDecimal latitud,
  @Schema(description="longitud") java.math.BigDecimal longitud,
  @Schema(description="tarifa_diaria", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull java.math.BigDecimal tarifa_diaria,
  @Schema(description="garantia_monetaria") java.math.BigDecimal garantia_monetaria,
  @Schema(description="moneda", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String moneda,
  @Schema(description="condiciones_uso", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String condiciones_uso,
  @Schema(description="condiciones_entrega", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String condiciones_entrega,
  @Schema(description="condiciones_devolucion", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String condiciones_devolucion,
  @Schema(description="condiciones_cancelacion", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String condiciones_cancelacion
) {}
