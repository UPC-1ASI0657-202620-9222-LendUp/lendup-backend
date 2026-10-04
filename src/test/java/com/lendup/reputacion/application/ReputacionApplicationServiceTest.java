package com.lendup.reputacion.application;

import static com.lendup.support.Asserts.list;
import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lendup.support.TestWorld;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

@DisplayName("Reputación - servicio de aplicación")
class ReputacionApplicationServiceTest {

  private TestWorld w;
  private String lenderId;
  private String borrowerId;

  @BeforeEach
  void setUp() {
    w = new TestWorld();
    lenderId = w.usuario("lender");
    borrowerId = w.usuario("borrower");
    w.usuario("third");
  }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private Object run(String action, Map<String, String> vars, Map<String, Object> body) {
    return w.reputacion.execute(action, vars, body, Map.of());
  }

  private String loan(String estado) { return w.prestamo(lenderId, borrowerId, estado, false).get("id").toString(); }

  @Test
  @DisplayName("rating registra la calificación al otro participante de un préstamo FINALIZADO")
  void califica() {
    // Arrange
    var id = loan("FINALIZADO");
    login("borrower");
    // Act
    var rating = map(run("rating", Map.of("id", id), Map.of("puntaje", 5, "comentario", "Excelente")));
    // Assert
    assertThat(rating).containsEntry("evaluador_usuario_id", borrowerId).containsEntry("evaluado_usuario_id", lenderId)
        .containsEntry("puntaje", 5);
  }

  @Test
  @DisplayName("rating del prestamista evalúa al prestatario")
  void calificaPrestamista() {
    // Arrange
    var id = loan("FINALIZADO");
    login("lender");
    // Act
    var rating = map(run("rating", Map.of("id", id), Map.of("puntaje", 4)));
    // Assert
    assertThat(rating).containsEntry("evaluado_usuario_id", borrowerId);
  }

  @Test
  @DisplayName("409 al calificar un préstamo que no está FINALIZADO")
  void prestamoNoFinalizado() {
    // Arrange
    var id = loan("ACTIVO");
    login("borrower");
    // Act + Assert
    status(409, () -> run("rating", Map.of("id", id), Map.of("puntaje", 5)));
  }

  @Test
  @DisplayName("400 cuando el puntaje falta, es 0, es 6 o no es numérico")
  void puntajeInvalido() {
    // Arrange
    var id = loan("FINALIZADO");
    login("borrower");
    // Act + Assert
    status(400, () -> run("rating", Map.of("id", id), Map.of()));
    status(400, () -> run("rating", Map.of("id", id), Map.of("puntaje", 0)));
    status(400, () -> run("rating", Map.of("id", id), Map.of("puntaje", 6)));
    status(400, () -> run("rating", Map.of("id", id), Map.of("puntaje", "cinco")));
  }

  @Test
  @DisplayName("403 cuando un tercero califica")
  void calificaTercero() {
    // Arrange
    var id = loan("FINALIZADO");
    login("third");
    // Act + Assert
    status(403, () -> run("rating", Map.of("id", id), Map.of("puntaje", 5)));
  }

  @Test
  @DisplayName("calificar dos veces el mismo préstamo viola la unicidad (el handler lo traduce a 409)")
  void calificacionDuplicada() {
    // Arrange
    var id = loan("FINALIZADO");
    login("borrower");
    run("rating", Map.of("id", id), Map.of("puntaje", 5));
    // Act + Assert
    assertThatThrownBy(() -> run("rating", Map.of("id", id), Map.of("puntaje", 3))).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @DisplayName("reputation calcula cantidad y promedio de las calificaciones recibidas")
  void reputacion() {
    // Arrange
    var first = loan("FINALIZADO");
    var second = loan("FINALIZADO");
    login("borrower");
    run("rating", Map.of("id", first), Map.of("puntaje", 5));
    run("rating", Map.of("id", second), Map.of("puntaje", 4));
    // Act
    var result = map(run("reputation", Map.of("id", lenderId), Map.of()));
    // Assert
    assertThat(result).containsEntry("cantidad", 2).containsEntry("promedio", 4.5);
    assertThat(list(result.get("calificaciones"))).hasSize(2);
  }

  @Test
  @DisplayName("reputation sin calificaciones devuelve promedio 0")
  void sinCalificaciones() {
    // Arrange
    login("third");
    // Act
    var result = map(run("reputation", Map.of("id", lenderId), Map.of()));
    // Assert
    assertThat(result).containsEntry("cantidad", 0).containsEntry("promedio", 0.0);
  }

  @Test
  @DisplayName("400 ante una acción desconocida")
  void accionDesconocida() {
    // Arrange
    login("borrower");
    // Act + Assert
    status(400, () -> run("noExiste", Map.of(), Map.of()));
  }
}
