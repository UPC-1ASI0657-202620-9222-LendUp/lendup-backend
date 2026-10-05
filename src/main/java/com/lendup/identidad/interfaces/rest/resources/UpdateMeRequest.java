package com.lendup.identidad.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="UpdateMeRequest",description="Campos admitidos por updateMe; campos ausentes en operaciones de actualización se conservan.")
public record UpdateMeRequest(
  @Schema(description="nombre") String nombre,
  @Schema(description="universidad") String universidad,
  @Schema(description="Sede opcional, texto libre") @jakarta.validation.constraints.Size(max=150) String campus,
  @Schema(description="carrera") String carrera,
  @Schema(description="ciclo") Integer ciclo,
  @Schema(description="telefono") String telefono,
  @Schema(description="foto_url") String foto_url
) {}
