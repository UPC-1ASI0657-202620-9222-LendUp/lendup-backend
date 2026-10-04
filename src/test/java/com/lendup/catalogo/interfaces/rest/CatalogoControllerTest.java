package com.lendup.catalogo.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.lendup.catalogo.application.CatalogoApplicationService;
import com.lendup.shared.ApiErrors;
import com.lendup.shared.FirebaseSecurity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/** Nivel 4: contrato HTTP del módulo de Catálogo. */
@WebMvcTest(CatalogoController.class)
@Import({FirebaseSecurity.class, ApiErrors.class})
@DisplayName("Catálogo - contrato de la API REST")
class CatalogoControllerTest {

  private static final String AUTH = "Bearer token-valido";
  private static final String PUBLICACION = """
      {"categoria_id":"c1","titulo":"Calculadora","descripcion":"Casio","condicion_objeto":"BUENA","universidad":"UNI",
       "campus":"Central","ubicacion":"Biblioteca","lugar_intercambio":"Puerta 3","tarifa_diaria":5.50,"moneda":"PEN",
       "condiciones_uso":"a","condiciones_entrega":"b","condiciones_devolucion":"c","condiciones_cancelacion":"d"}""";

  @Autowired MockMvc mvc;
  @MockitoBean FirebaseAuth firebaseAuth;
  @MockitoBean CatalogoApplicationService service;

  @BeforeEach
  void setUp() throws Exception {
    var token = Mockito.mock(FirebaseToken.class);
    when(token.getUid()).thenReturn("uid-1");
    when(firebaseAuth.verifyIdToken("token-valido")).thenReturn(token);
  }

  @Test
  @DisplayName("POST /objetos con cuerpo válido responde 201 con la publicación creada")
  void creaPublicacion201() throws Exception {
    // Arrange
    when(service.execute(eq("createPublication"), any(), any(), any()))
        .thenReturn(Map.of("id", "pub-1", "titulo", "Calculadora", "estado", "ACTIVA"));
    // Act
    var result = mvc.perform(post("/api/v1/objetos").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(PUBLICACION));
    // Assert
    result.andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("pub-1"))
        .andExpect(jsonPath("$.estado").value("ACTIVA"));
  }

  @Test
  @DisplayName("POST /objetos sin título ni tarifa responde 400 y no llega al servicio")
  void creaPublicacion400() throws Exception {
    // Arrange
    var json = "{\"categoria_id\":\"c1\",\"descripcion\":\"Casio\"}";
    // Act
    var result = mvc.perform(post("/api/v1/objetos").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(json));
    // Assert
    result.andExpect(status().isBadRequest());
    verify(service, never()).execute(any(), any(), any(), any());
  }

  @Test
  @DisplayName("PUT /objetos/{id}/disponibilidad con periodo vacío responde 400")
  void disponibilidad400() throws Exception {
    // Arrange
    var json = "{\"desde\":\"\",\"hasta\":\"\"}";
    // Act
    var result = mvc.perform(put("/api/v1/objetos/pub-1/disponibilidad").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(json));
    // Assert
    result.andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /objetos/{id} de un no propietario responde 403 con el cuerpo de error estándar")
  void actualizaNoPropietario403() throws Exception {
    // Arrange
    when(service.execute(eq("updatePublication"), any(), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Operación no permitida"));
    // Act
    var result = mvc.perform(put("/api/v1/objetos/pub-1").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(PUBLICACION));
    // Assert
    result.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403)).andExpect(jsonPath("$.message").value("Operación no permitida"));
  }

  @Test
  @DisplayName("GET /objetos/{id} inexistente responde 404")
  void publicacionNoEncontrada404() throws Exception {
    // Arrange
    when(service.execute(eq("publication"), any(), any(), any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"));
    // Act
    var result = mvc.perform(get("/api/v1/objetos/nada").header("Authorization", AUTH));
    // Assert
    result.andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("SOLICITUD_INVALIDA"));
  }

  @Test
  @DisplayName("GET /objetos con filtros responde 200 con un arreglo JSON y reenvía los filtros")
  void buscaPublicaciones200() throws Exception {
    // Arrange
    when(service.execute(eq("listPublications"), any(), any(), eq(Map.of("nombre", "calc", "campus", "Central"))))
        .thenReturn(List.of(Map.of("id", "pub-1", "titulo", "Calculadora")));
    // Act
    var result = mvc.perform(get("/api/v1/objetos").param("nombre", "calc").param("campus", "Central").header("Authorization", AUTH));
    // Assert
    result.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("pub-1")).andExpect(jsonPath("$.length()").value(1));
  }
}
