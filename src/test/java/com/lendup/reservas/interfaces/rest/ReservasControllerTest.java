package com.lendup.reservas.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.lendup.reservas.application.ReservasApplicationService;
import com.lendup.shared.ApiErrors;
import com.lendup.shared.FirebaseSecurity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/** Nivel 4: contrato HTTP del módulo de Reservas (códigos de estado, validación @Valid y forma del JSON). */
@WebMvcTest(ReservasController.class)
@Import({FirebaseSecurity.class, ApiErrors.class})
@DisplayName("Reservas - contrato de la API REST")
class ReservasControllerTest {

  private static final String AUTH = "Bearer token-valido";

  @Autowired MockMvc mvc;
  @MockitoBean FirebaseAuth firebaseAuth;
  @MockitoBean ReservasApplicationService service;

  @BeforeEach
  void setUp() throws Exception {
    var token = Mockito.mock(FirebaseToken.class);
    when(token.getUid()).thenReturn("uid-1");
    when(firebaseAuth.verifyIdToken("token-valido")).thenReturn(token);
  }

  @Test
  @DisplayName("POST /solicitudes con cuerpo válido responde 201 con la solicitud creada")
  void creaSolicitud201() throws Exception {
    // Arrange
    var created = Map.<String, Object>of("id", "sol-1", "estado", "PENDIENTE", "publicacion_id", "pub-1");
    when(service.execute(eq("createRequest"), any(), any(), any())).thenReturn(created);
    var json = "{\"publicacion_id\":\"pub-1\",\"desde\":\"2026-10-05T09:00:00\",\"hasta\":\"2026-10-07T18:00:00\"}";
    // Act
    var result = mvc.perform(post("/api/v1/solicitudes").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(json));
    // Assert
    result.andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("sol-1"))
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.publicacion_id").value("pub-1"));
    @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
    verify(service).execute(eq("createRequest"), any(), body.capture(), any());
    assertThat(body.getValue()).containsEntry("publicacion_id", "pub-1").containsEntry("desde", "2026-10-05T09:00:00");
  }

  @Test
  @DisplayName("POST /solicitudes con campos obligatorios en blanco responde 400 y no llega al servicio")
  void creaSolicitud400() throws Exception {
    // Arrange
    var json = "{\"publicacion_id\":\"\",\"desde\":\"2026-10-05T09:00:00\",\"hasta\":\"\"}";
    // Act
    var result = mvc.perform(post("/api/v1/solicitudes").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(json));
    // Assert
    result.andExpect(status().isBadRequest());
    verify(service, never()).execute(any(), any(), any(), any());
  }

  @Test
  @DisplayName("POST /solicitudes con cuerpo vacío o JSON malformado responde 400")
  void creaSolicitudCuerpoInvalido() throws Exception {
    // Arrange
    var vacio = "{}";
    var malformado = "{no es json";
    // Act + Assert
    mvc.perform(post("/api/v1/solicitudes").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(vacio))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/solicitudes").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(malformado))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /solicitudes/{id}/aceptacion responde 201 con la reserva confirmada")
  void aceptaSolicitud201() throws Exception {
    // Arrange
    when(service.execute(eq("acceptRequest"), eq(Map.of("id", "sol-1")), any(), any()))
        .thenReturn(Map.of("id", "res-1", "estado", "CONFIRMADA", "solicitud_id", "sol-1"));
    // Act
    var result = mvc.perform(post("/api/v1/solicitudes/sol-1/aceptacion").header("Authorization", AUTH));
    // Assert
    result.andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
        .andExpect(jsonPath("$.solicitud_id").value("sol-1"));
  }

  @Test
  @DisplayName("un 409 del servicio se expone con la estructura {status, error, message}")
  void conflicto409() throws Exception {
    // Arrange
    when(service.execute(eq("acceptRequest"), any(), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Periodo ya reservado"));
    // Act
    var result = mvc.perform(post("/api/v1/solicitudes/sol-2/aceptacion").header("Authorization", AUTH));
    // Assert
    result.andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("SOLICITUD_INVALIDA"))
        .andExpect(jsonPath("$.message").value("Periodo ya reservado"));
  }

  @Test
  @DisplayName("403 y 404 del servicio se propagan con su código HTTP")
  void prohibidoYNoEncontrado() throws Exception {
    // Arrange
    when(service.execute(eq("acceptRequest"), eq(Map.of("id", "ajena")), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Operación no permitida"));
    when(service.execute(eq("request"), eq(Map.of("id", "nada")), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"));
    // Act + Assert
    mvc.perform(post("/api/v1/solicitudes/ajena/aceptacion").header("Authorization", AUTH))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    mvc.perform(get("/api/v1/solicitudes/nada").header("Authorization", AUTH))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("No encontrado"));
  }

  @Test
  @DisplayName("una violación de unicidad en base de datos responde 409 CONFLICTO_DATOS")
  void unicidad409() throws Exception {
    // Arrange
    when(service.execute(eq("acceptRequest"), any(), any(), any())).thenThrow(new DuplicateKeyException("dup"));
    // Act
    var result = mvc.perform(post("/api/v1/solicitudes/sol-3/aceptacion").header("Authorization", AUTH));
    // Assert
    result.andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICTO_DATOS"));
  }

  @Test
  @DisplayName("POST /solicitudes/{id}/rechazo responde 200 y reenvía el motivo")
  void rechazaSolicitud200() throws Exception {
    // Arrange
    when(service.execute(eq("rejectRequest"), any(), any(), any())).thenReturn(Map.of("id", "sol-1", "estado", "RECHAZADA"));
    // Act
    var result = mvc.perform(post("/api/v1/solicitudes/sol-1/rechazo").header("Authorization", AUTH)
        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo_rechazo\":\"No disponible\"}"));
    // Assert
    result.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"));
    @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
    verify(service).execute(eq("rejectRequest"), any(), body.capture(), any());
    assertThat(body.getValue()).containsEntry("motivo_rechazo", "No disponible");
  }

  @Test
  @DisplayName("GET /reservas?rol=PRESTAMISTA responde 200 con un arreglo JSON y reenvía el filtro")
  void listaReservas200() throws Exception {
    // Arrange
    when(service.execute(eq("listReservations"), any(), any(), eq(Map.of("rol", "PRESTAMISTA"))))
        .thenReturn(List.of(Map.of("id", "res-1", "estado", "CONFIRMADA"), Map.of("id", "res-2", "estado", "CANCELADA")));
    // Act
    var result = mvc.perform(get("/api/v1/reservas").param("rol", "PRESTAMISTA").header("Authorization", AUTH));
    // Assert
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("res-1"))
        .andExpect(jsonPath("$[1].estado").value("CANCELADA"));
  }
}
