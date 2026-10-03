package com.lendup.identidad.interfaces.rest.resources;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(name="CreateUserRequest",description="Campos admitidos por createUser; campos ausentes en operaciones de actualización se conservan.")
public record CreateUserRequest(
  @Schema(description="correo_institucional", example="ana@universidad.edu.pe", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String correo_institucional,
  @Schema(description="nombre", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String nombre,
  @Schema(description="universidad", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String universidad,
  @Schema(description="campus", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String campus,
  @Schema(description="carrera", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String carrera,
  @Schema(description="ciclo", requiredMode=Schema.RequiredMode.REQUIRED) @NotNull Integer ciclo,
  @Schema(description="telefono", requiredMode=Schema.RequiredMode.REQUIRED) @NotBlank String telefono,
  @Schema(description="foto_url") String foto_url
) {}
