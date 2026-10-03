package com.lendup.catalogo.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="PublicationStateRequest",description="Campos admitidos por publicationState; campos ausentes en operaciones de actualización se conservan.")
public record PublicationStateRequest(
  @Schema(description="estado", allowableValues={"ACTIVA","PAUSADA","DADA_DE_BAJA"}) String estado
) {}
