package com.lendup.shared;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfiguration {
  @Bean OpenAPI lendupApi(){return new OpenAPI()
    .info(new Info().title("LendUp API").version("v1")
      .description("Base modular con persistencia MySQL; consulte README para operaciones pendientes."))
    .components(new Components().addSecuritySchemes("firebase",new SecurityScheme()
      .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("Firebase ID token")))
    .addSecurityItem(new SecurityRequirement().addList("firebase"));}
}
