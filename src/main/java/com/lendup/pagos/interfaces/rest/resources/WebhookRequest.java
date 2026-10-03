package com.lendup.pagos.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="WebhookRequest",description="Campos admitidos por webhook; campos ausentes en operaciones de actualización se conservan.")
public record WebhookRequest(
  @Schema(description="id") String id,
  @Schema(description="type", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String type,
  @Schema(description="data", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull java.util.Map<String,Object> data
) {}
