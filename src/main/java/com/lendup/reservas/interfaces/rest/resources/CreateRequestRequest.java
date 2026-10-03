package com.lendup.reservas.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="CreateRequestRequest",description="Campos admitidos por createRequest; campos ausentes en operaciones de actualización se conservan.")
public record CreateRequestRequest(
  @Schema(description="publicacion_id", example="UUID_PUBLICACION", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String publicacion_id,
  @Schema(description="desde", example="2026-10-05T09:00:00", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String desde,
  @Schema(description="hasta", example="2026-10-07T18:00:00", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String hasta
) {}
