package com.lendup.reputacion.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="RatingRequest",description="Campos admitidos por rating; campos ausentes en operaciones de actualización se conservan.")
public record RatingRequest(
  @Schema(description="evaluado_usuario_id") String evaluado_usuario_id,
  @Schema(description="puntaje", example="5", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull Integer puntaje,
  @Schema(description="comentario") String comentario
) {}
