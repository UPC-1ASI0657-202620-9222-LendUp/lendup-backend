package com.lendup.catalogo.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="UpdatePublicationRequest",description="Campos admitidos por updatePublication; campos ausentes en operaciones de actualización se conservan.")
public record UpdatePublicationRequest(
  @Schema(description="categoria_id", example="00000000-0000-4000-8000-000000000003") String categoria_id,
  @Schema(description="titulo") String titulo,
  @Schema(description="descripcion") String descripcion,
  @Schema(description="condicion_objeto") String condicion_objeto,
  @Schema(description="universidad") String universidad,
  @Schema(description="campus") String campus,
  @Schema(description="ubicacion") String ubicacion,
  @Schema(description="lugar_intercambio") String lugar_intercambio,
  @Schema(description="latitud") java.math.BigDecimal latitud,
  @Schema(description="longitud") java.math.BigDecimal longitud,
  @Schema(description="tarifa_diaria") java.math.BigDecimal tarifa_diaria,
  @Schema(description="garantia_monetaria") java.math.BigDecimal garantia_monetaria,
  @Schema(description="moneda") String moneda,
  @Schema(description="condiciones_uso") String condiciones_uso,
  @Schema(description="condiciones_entrega") String condiciones_entrega,
  @Schema(description="condiciones_devolucion") String condiciones_devolucion,
  @Schema(description="condiciones_cancelacion") String condiciones_cancelacion
) {}
