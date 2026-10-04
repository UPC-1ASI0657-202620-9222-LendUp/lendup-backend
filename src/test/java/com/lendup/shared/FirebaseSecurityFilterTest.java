package com.lendup.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.lendup.pagos.application.PagosApplicationService;
import com.lendup.pagos.interfaces.rest.PagosController;
import com.lendup.reservas.application.ReservasApplicationService;
import com.lendup.reservas.interfaces.rest.ReservasController;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Nivel 3: filtro Bearer de Firebase y reglas de acceso de {@link FirebaseSecurity}, con FirebaseAuth simulado. */
@WebMvcTest(controllers = {ReservasController.class, PagosController.class})
@Import({FirebaseSecurity.class, ApiErrors.class})
@DisplayName("Seguridad - filtro de tokens Firebase")
class FirebaseSecurityFilterTest {

  private static final String PROTEGIDO = "/api/v1/solicitudes";

  @Autowired MockMvc mvc;
  @MockitoBean FirebaseAuth firebaseAuth;
  @MockitoBean ReservasApplicationService reservas;
  @MockitoBean PagosApplicationService pagos;

  @BeforeEach
  void setUp() {
    when(reservas.execute(anyString(), any(), any(), any())).thenReturn(List.of());
    when(pagos.execute(anyString(), any(), any(), any())).thenReturn(Map.of("id", "evt-1"));
  }

  @Test
  @DisplayName("401 cuando la petición no trae token")
  void sinToken() throws Exception {
    // Arrange: ninguna cabecera Authorization
    // Act
    var result = mvc.perform(get(PROTEGIDO));
    // Assert
    result.andExpect(status().isUnauthorized());
    verify(reservas, never()).execute(anyString(), any(), any(), any());
  }

  @Test
  @DisplayName("401 cuando el esquema de Authorization no es Bearer")
  void esquemaIncorrecto() throws Exception {
    // Arrange
    var cabecera = "Basic dXNlcjpwYXNz";
    // Act
    var result = mvc.perform(get(PROTEGIDO).header("Authorization", cabecera));
    // Assert
    result.andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("401 cuando Firebase rechaza el token y no se invoca el servicio")
  void tokenInvalido() throws Exception {
    // Arrange
    when(firebaseAuth.verifyIdToken("malo")).thenThrow(mock(FirebaseAuthException.class));
    // Act
    var result = mvc.perform(get(PROTEGIDO).header("Authorization", "Bearer malo"));
    // Assert
    result.andExpect(status().isUnauthorized());
    verify(reservas, never()).execute(anyString(), any(), any(), any());
  }

  @Test
  @DisplayName("el uid del token verificado queda como principal del SecurityContext")
  void uidEnSecurityContext() throws Exception {
    // Arrange
    var token = mock(FirebaseToken.class);
    when(token.getUid()).thenReturn("uid-firebase-123");
    when(firebaseAuth.verifyIdToken("bueno")).thenReturn(token);
    var principal = new AtomicReference<Object>();
    when(reservas.execute(anyString(), any(), any(), any())).thenAnswer(inv -> {
      principal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
      return List.of();
    });
    // Act
    var result = mvc.perform(get(PROTEGIDO).header("Authorization", "Bearer bueno"));
    // Assert
    result.andExpect(status().isOk());
    assertThat(principal.get()).isEqualTo("uid-firebase-123");
  }

  @Test
  @DisplayName("POST /api/v1/webhooks/mercado-pago es público (sin token)")
  void webhookPublico() throws Exception {
    // Arrange
    var json = "{\"type\":\"payment\",\"data\":{\"id\":1}}";
    // Act
    var result = mvc.perform(post("/api/v1/webhooks/mercado-pago").contentType(MediaType.APPLICATION_JSON).content(json));
    // Assert
    result.andExpect(status().isCreated());
  }

  @Test
  @DisplayName("el webhook solo es público para POST: un GET sin token recibe 401")
  void webhookSoloPost() throws Exception {
    // Arrange: GET al mismo path sin token
    // Act
    var result = mvc.perform(get("/api/v1/webhooks/mercado-pago"));
    // Assert
    result.andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("/swagger-ui.html no exige autenticación (ni 401 ni 403)")
  void swaggerPublico() throws Exception {
    // Arrange: sin token
    // Act
    var result = mvc.perform(get("/swagger-ui.html"));
    // Assert
    var code = result.andReturn().getResponse().getStatus();
    assertThat(code).isNotIn(401, 403);
  }
}
