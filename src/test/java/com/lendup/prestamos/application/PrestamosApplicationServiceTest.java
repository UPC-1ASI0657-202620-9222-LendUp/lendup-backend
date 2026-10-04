package com.lendup.prestamos.application;

import static com.lendup.support.Asserts.list;
import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.lendup.support.TestWorld;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Préstamos - servicio de aplicación")
class PrestamosApplicationServiceTest {

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

  private Object run(String action, Map<String, String> vars, Map<String, Object> body, Map<String, String> query) {
    return w.prestamos.execute(action, vars, body, query);
  }

  private Object run(String action, String loanId) { return run(action, Map.of("id", loanId), Map.of(), Map.of()); }

  private String loan(String estado) { return w.prestamo(lenderId, borrowerId, estado, false).get("id").toString(); }

  @Nested
  @DisplayName("consultas")
  class Consultas {

    @Test
    @DisplayName("listLoans y calendar devuelven solo los préstamos donde participa el usuario")
    void listaPropios() {
      // Arrange
      var id = loan("RESERVADO");
      // Act
      login("borrower");
      var propios = list(run("listLoans", Map.of(), Map.of(), Map.of()));
      var calendario = list(run("calendar", Map.of(), Map.of(), Map.of()));
      login("third");
      var ajenos = list(run("listLoans", Map.of(), Map.of(), Map.of()));
      // Assert
      assertThat(propios).extracting(l -> l.get("id")).containsExactly(id);
      assertThat(calendario).hasSize(1);
      assertThat(ajenos).isEmpty();
    }

    @Test
    @DisplayName("listLoans filtra por estado")
    void filtraPorEstado() {
      // Arrange
      loan("RESERVADO");
      loan("ACTIVO");
      login("lender");
      // Act
      var activos = list(run("listLoans", Map.of(), Map.of(), Map.of("estado", "ACTIVO")));
      // Assert
      assertThat(activos).hasSize(1).first().satisfies(l -> assertThat(l).containsEntry("estado", "ACTIVO"));
    }

    @Test
    @DisplayName("loan devuelve el préstamo a un participante, 403 a un tercero y 404 si no existe")
    void obtienePrestamo() {
      // Arrange
      var id = loan("RESERVADO");
      login("borrower");
      // Act
      var found = map(run("loan", id));
      // Assert
      assertThat(found).containsEntry("id", id);
      login("third");
      status(403, () -> run("loan", id));
      status(404, () -> run("loan", "no-existe"));
    }

    @Test
    @DisplayName("extensionQuote y paymentQuote devuelven cotización preliminar con la tarifa acordada")
    void cotizaciones() {
      // Arrange
      var id = loan("RESERVADO");
      login("borrower");
      // Act
      var extension = map(run("extensionQuote", id));
      var pago = map(run("paymentQuote", id));
      // Assert
      assertThat(extension).containsEntry("estado", "COTIZACION_PRELIMINAR").containsEntry("tarifa_diaria", new BigDecimal("5.00"));
      assertThat(pago).containsEntry("estado", "COTIZACION_PRELIMINAR").containsEntry("moneda", "PEN").containsKey("comision_proveedor");
    }
  }

  @Nested
  @DisplayName("ciclo de vida")
  class CicloDeVida {

    @Test
    @DisplayName("delivery registra la entrega cuando el pago está confirmado")
    void entrega() {
      // Arrange
      var id = loan("RESERVADO");
      w.store.update("prestamos", id, Map.of("pago_tarifa_confirmado_en", "2026-10-04T10:00:00"));
      login("lender");
      // Act
      var result = map(run("delivery", id));
      // Assert
      assertThat(result).containsEntry("estado", "ENTREGA_REGISTRADA");
      assertThat(result.get("entrega_registrada_en")).isNotNull();
    }

    @Test
    @DisplayName("403 cuando entrega el prestatario o un tercero")
    void entregaNoPrestamista() {
      // Arrange
      var id = loan("RESERVADO");
      // Act + Assert
      login("borrower");
      status(403, () -> run("delivery", id));
      login("third");
      status(403, () -> run("delivery", id));
    }

    @Test
    @DisplayName("409 cuando el préstamo no está RESERVADO")
    void entregaEstadoIncorrecto() {
      // Arrange
      var id = loan("ACTIVO");
      login("lender");
      // Act + Assert
      status(409, () -> run("delivery", id));
    }

    @Test
    @DisplayName("409 cuando falta confirmar el pago de la tarifa")
    void entregaSinPago() {
      // Arrange
      var id = loan("RESERVADO");
      login("lender");
      // Act + Assert
      status(409, () -> run("delivery", id));
    }

    @Test
    @DisplayName("409 cuando se exige garantía y aún no está constituida")
    void entregaSinGarantia() {
      // Arrange
      var id = w.prestamo(lenderId, borrowerId, "RESERVADO", true).get("id").toString();
      w.store.update("prestamos", id, Map.of("pago_tarifa_confirmado_en", "2026-10-04T10:00:00"));
      login("lender");
      // Act + Assert
      status(409, () -> run("delivery", id));
    }

    @Test
    @DisplayName("delivery con garantía constituida y pago confirmado es válido")
    void entregaConGarantia() {
      // Arrange
      var id = w.prestamo(lenderId, borrowerId, "RESERVADO", true).get("id").toString();
      w.store.update("prestamos", id, Map.of("pago_tarifa_confirmado_en", "2026-10-04T10:00:00", "garantia_constituida_en", "2026-10-04T10:05:00"));
      login("lender");
      // Act
      var result = map(run("delivery", id));
      // Assert
      assertThat(result).containsEntry("estado", "ENTREGA_REGISTRADA");
    }

    @Test
    @DisplayName("receipt activa el préstamo cuando el prestatario confirma la recepción")
    void recepcion() {
      // Arrange
      var id = loan("ENTREGA_REGISTRADA");
      login("borrower");
      // Act
      var result = map(run("receipt", id));
      // Assert
      assertThat(result).containsEntry("estado", "ACTIVO");
      assertThat(result.get("activado_en")).isNotNull().isEqualTo(result.get("recepcion_confirmada_en"));
    }

    @Test
    @DisplayName("receipt: 403 al prestamista y 409 si no se registró la entrega")
    void recepcionInvalida() {
      // Arrange
      var entregado = loan("ENTREGA_REGISTRADA");
      var reservado = loan("RESERVADO");
      // Act + Assert
      login("lender");
      status(403, () -> run("receipt", entregado));
      login("borrower");
      status(409, () -> run("receipt", reservado));
    }

    @Test
    @DisplayName("returnLoan registra la devolución del préstamo ACTIVO")
    void devolucion() {
      // Arrange
      var id = loan("ACTIVO");
      login("borrower");
      // Act
      var result = map(run("returnLoan", id));
      // Assert
      assertThat(result).containsEntry("estado", "DEVOLUCION_REGISTRADA");
    }

    @Test
    @DisplayName("returnLoan: 409 si no está ACTIVO y 403 al prestamista")
    void devolucionInvalida() {
      // Arrange
      var reservado = loan("RESERVADO");
      var activo = loan("ACTIVO");
      // Act + Assert
      login("borrower");
      status(409, () -> run("returnLoan", reservado));
      login("lender");
      status(403, () -> run("returnLoan", activo));
    }

    @Test
    @DisplayName("confirmReturn finaliza el préstamo sin incidencias pendientes")
    void confirmaDevolucion() {
      // Arrange
      var id = loan("DEVOLUCION_REGISTRADA");
      login("lender");
      // Act
      var result = map(run("confirmReturn", id));
      // Assert
      assertThat(result).containsEntry("estado", "FINALIZADO");
      assertThat(result.get("finalizado_en")).isNotNull();
    }

    @Test
    @DisplayName("confirmReturn: 409 con incidencias pendientes o devolución sin registrar, 403 al prestatario")
    void confirmaDevolucionInvalida() {
      // Arrange
      var conIncidencia = loan("DEVOLUCION_REGISTRADA");
      w.store.update("prestamos", conIncidencia, Map.of("incidencias_pendientes", 1));
      var sinDevolucion = loan("ACTIVO");
      // Act + Assert
      login("lender");
      status(409, () -> run("confirmReturn", conIncidencia));
      status(409, () -> run("confirmReturn", sinDevolucion));
      login("borrower");
      status(403, () -> run("confirmReturn", conIncidencia));
    }
  }

  @Nested
  @DisplayName("extensiones y reprogramaciones")
  class Cambios {

    private Map<String, Object> propuesta(String action, String loanId, String user) {
      login(user);
      return map(run(action, Map.of("id", loanId), Map.of("fecha_propuesta_en", "2026-10-09T18:00:00"), Map.of()));
    }

    @Test
    @DisplayName("extension y reschedule crean un cambio PENDIENTE con tipo, proponente y fecha anterior")
    void creaCambios() {
      // Arrange
      var id = loan("ACTIVO");
      // Act
      var extension = propuesta("extension", id, "borrower");
      var reprogramacion = propuesta("reschedule", id, "lender");
      // Assert
      assertThat(extension).containsEntry("tipo", "EXTENSION").containsEntry("estado", "PENDIENTE").containsEntry("propuesto_por_usuario_id", borrowerId);
      assertThat(extension.get("fecha_anterior_en")).isEqualTo(w.store.get("prestamos", id).get("devolucion_vigente_en"));
      assertThat(reprogramacion).containsEntry("tipo", "REPROGRAMACION").containsEntry("propuesto_por_usuario_id", lenderId);
    }

    @Test
    @DisplayName("403 cuando un tercero propone un cambio")
    void proponeTercero() {
      // Arrange
      var id = loan("ACTIVO");
      login("third");
      // Act + Assert
      status(403, () -> run("extension", Map.of("id", id), Map.of("fecha_propuesta_en", "2026-10-09T18:00:00"), Map.of()));
    }

    @Test
    @DisplayName("la contraparte aplica una reprogramación y se actualiza la devolución vigente")
    void aplicaReprogramacion() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("reschedule", id, "borrower");
      login("lender");
      // Act
      var result = map(run("rescheduleResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "APLICADA"), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "APLICADA").containsEntry("respondido_por_usuario_id", lenderId);
      assertThat(w.store.get("prestamos", id).get("devolucion_vigente_en")).isEqualTo(change.get("fecha_propuesta_en"));
    }

    @Test
    @DisplayName("la contraparte acepta una extensión pendiente de pago sin mover la devolución")
    void aceptaExtension() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("extension", id, "borrower");
      var original = w.store.get("prestamos", id).get("devolucion_vigente_en");
      login("lender");
      // Act
      var result = map(run("extensionResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "ACEPTADA_PENDIENTE_PAGO"), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "ACEPTADA_PENDIENTE_PAGO");
      assertThat(w.store.get("prestamos", id).get("devolucion_vigente_en")).isEqualTo(original);
    }

    @Test
    @DisplayName("la contraparte rechaza el cambio")
    void rechazaCambio() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("extension", id, "borrower");
      login("lender");
      // Act
      var result = map(run("extensionResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "RECHAZADA"), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "RECHAZADA");
    }

    @Test
    @DisplayName("403 cuando quien propuso intenta responder su propio cambio")
    void respondeProponente() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("reschedule", id, "borrower");
      // Act + Assert
      status(403, () -> run("rescheduleResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "APLICADA"), Map.of()));
    }

    @Test
    @DisplayName("409 cuando una extensión se marca APLICADA sin pago")
    void extensionAplicadaSinPago() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("extension", id, "borrower");
      login("lender");
      // Act + Assert
      status(409, () -> run("extensionResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "APLICADA"), Map.of()));
    }

    @Test
    @DisplayName("400 cuando una reprogramación se marca ACEPTADA_PENDIENTE_PAGO")
    void reprogramacionConPago() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("reschedule", id, "borrower");
      login("lender");
      // Act + Assert
      status(400, () -> run("rescheduleResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "ACEPTADA_PENDIENTE_PAGO"), Map.of()));
    }

    @Test
    @DisplayName("400 ante un estado de respuesta no permitido")
    void respuestaInvalida() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("reschedule", id, "borrower");
      login("lender");
      // Act + Assert
      status(400, () -> run("rescheduleResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "TALVEZ"), Map.of()));
    }

    @Test
    @DisplayName("409 cuando el cambio ya fue respondido")
    void cambioYaRespondido() {
      // Arrange
      var id = loan("ACTIVO");
      var change = propuesta("reschedule", id, "borrower");
      login("lender");
      var vars = Map.of("id", id, "subid", change.get("id").toString());
      run("rescheduleResponse", vars, Map.of("estado", "RECHAZADA"), Map.of());
      // Act + Assert
      status(409, () -> run("rescheduleResponse", vars, Map.of("estado", "RECHAZADA"), Map.of()));
    }

    @Test
    @DisplayName("404 cuando el cambio es de otro préstamo o de otro tipo")
    void cambioAjeno() {
      // Arrange
      var id = loan("ACTIVO");
      var otro = loan("ACTIVO");
      var change = propuesta("extension", id, "borrower");
      login("lender");
      // Act + Assert
      status(404, () -> run("extensionResponse", Map.of("id", otro, "subid", change.get("id").toString()), Map.of("estado", "RECHAZADA"), Map.of()));
      status(404, () -> run("rescheduleResponse", Map.of("id", id, "subid", change.get("id").toString()), Map.of("estado", "RECHAZADA"), Map.of()));
    }
  }

  @Test
  @DisplayName("400 ante una acción desconocida")
  void accionDesconocida() {
    // Arrange
    login("lender");
    // Act + Assert
    status(400, () -> run("noExiste", Map.of(), Map.of(), Map.of()));
  }
}
