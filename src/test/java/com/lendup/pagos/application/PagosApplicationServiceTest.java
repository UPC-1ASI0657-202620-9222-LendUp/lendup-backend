package com.lendup.pagos.application;

import static com.lendup.support.Asserts.list;
import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lendup.support.TestWorld;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

@DisplayName("Pagos - servicio de aplicación")
class PagosApplicationServiceTest {

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
    return w.pagos.execute(action, vars, body, Map.of());
  }

  private Map<String, Object> pago(String loanId, String clave) {
    var b = new LinkedHashMap<String, Object>();
    b.put("prestamo_id", loanId);
    b.put("tipo", "PAGO_TARIFA");
    b.put("monto", new BigDecimal("15.00"));
    b.put("medio_pago_seleccionado", "MERCADO_PAGO");
    b.put("clave_idempotencia", clave);
    return b;
  }

  private String loan(boolean garantia) { return w.prestamo(lenderId, borrowerId, "RESERVADO", garantia).get("id").toString(); }

  @Test
  @DisplayName("paymentMethods lista Mercado Pago pendiente de integración")
  void medios() {
    // Arrange
    login("borrower");
    // Act
    var methods = list(run("paymentMethods", Map.of(), Map.of()));
    // Assert
    assertThat(methods).hasSize(1);
    assertThat(methods.getFirst()).containsEntry("codigo", "MERCADO_PAGO").containsEntry("estado", "PENDIENTE_INTEGRACION");
  }

  @Test
  @DisplayName("payment registra una transacción PENDIENTE con la moneda del préstamo")
  void registraPago() {
    // Arrange
    var id = loan(false);
    login("borrower");
    // Act
    var tx = map(run("payment", Map.of(), pago(id, "k-1")));
    // Assert
    assertThat(tx).containsEntry("estado", "PENDIENTE").containsEntry("moneda", "PEN")
        .containsEntry("tipo", "PAGO_TARIFA").containsEntry("clave_idempotencia", "k-1");
  }

  @Test
  @DisplayName("payment: 403 al prestamista y a un tercero")
  void pagoNoPrestatario() {
    // Arrange
    var id = loan(false);
    // Act + Assert
    login("lender");
    status(403, () -> run("payment", Map.of(), pago(id, "k-2")));
    login("third");
    status(403, () -> run("payment", Map.of(), pago(id, "k-3")));
  }

  @Test
  @DisplayName("payment: 400 por tipo inválido, monto ausente, medio ausente o clave de idempotencia ausente")
  void pagoInvalido() {
    // Arrange
    var id = loan(false);
    login("borrower");
    var tipo = pago(id, "k-4");
    tipo.put("tipo", "REGALO");
    var monto = pago(id, "k-5");
    monto.remove("monto");
    var medio = pago(id, "k-6");
    medio.put("medio_pago_seleccionado", " ");
    var clave = pago(id, "k-7");
    clave.remove("clave_idempotencia");
    // Act + Assert
    status(400, () -> run("payment", Map.of(), tipo));
    status(400, () -> run("payment", Map.of(), monto));
    status(400, () -> run("payment", Map.of(), medio));
    status(400, () -> run("payment", Map.of(), clave));
  }

  @Test
  @DisplayName("payment: 404 cuando el préstamo no existe")
  void pagoPrestamoInexistente() {
    // Arrange
    login("borrower");
    // Act + Assert
    status(404, () -> run("payment", Map.of(), pago("no-existe", "k-8")));
  }

  @Test
  @DisplayName("payment repetido con la misma clave de idempotencia viola la unicidad (el handler lo traduce a 409)")
  void pagoDuplicado() {
    // Arrange
    var id = loan(false);
    login("borrower");
    run("payment", Map.of(), pago(id, "k-9"));
    // Act + Assert
    assertThatThrownBy(() -> run("payment", Map.of(), pago(id, "k-9"))).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @DisplayName("guarantee crea la garantía con el monto acordado y la transacción de constitución")
  void creaGarantia() {
    // Arrange
    var id = loan(true);
    login("borrower");
    var body = Map.<String, Object>of("prestamo_id", id, "medio_pago_seleccionado", "MERCADO_PAGO", "clave_idempotencia", "g-1");
    // Act
    var guarantee = map(run("guarantee", Map.of(), body));
    // Assert
    assertThat(guarantee).containsEntry("monto_acordado", new BigDecimal("50.00")).containsEntry("estado", "PENDIENTE");
    var txs = w.store.list("transacciones_economicas", "garantia_id", guarantee.get("id"));
    assertThat(txs).hasSize(1);
    assertThat(txs.getFirst()).containsEntry("tipo", "CONSTITUCION_GARANTIA").containsEntry("monto", new BigDecimal("50.00"));
  }

  @Test
  @DisplayName("guarantee sin datos de pago solo crea la garantía")
  void garantiaSinPago() {
    // Arrange
    var id = loan(true);
    login("borrower");
    // Act
    run("guarantee", Map.of(), Map.of("prestamo_id", id));
    // Assert
    assertThat(w.store.count("garantias")).isEqualTo(1);
    assertThat(w.store.count("transacciones_economicas")).isZero();
  }

  @Test
  @DisplayName("guarantee: 409 si ya existe la garantía y 409 si el préstamo no la requiere")
  void garantiaConflictos() {
    // Arrange
    var con = loan(true);
    var sin = loan(false);
    login("borrower");
    run("guarantee", Map.of(), Map.of("prestamo_id", con));
    // Act + Assert
    status(409, () -> run("guarantee", Map.of(), Map.of("prestamo_id", con)));
    status(409, () -> run("guarantee", Map.of(), Map.of("prestamo_id", sin)));
  }

  @Test
  @DisplayName("guarantee: 403 al prestamista")
  void garantiaPrestamista() {
    // Arrange
    var id = loan(true);
    login("lender");
    // Act + Assert
    status(403, () -> run("guarantee", Map.of(), Map.of("prestamo_id", id)));
  }

  @Test
  @DisplayName("webhook guarda el evento del proveedor como PENDIENTE_VALIDACION")
  void guardaWebhook() {
    // Arrange
    var payload = Map.<String, Object>of("id", "evt-1", "type", "payment");
    // Act
    var event = map(run("webhook", Map.of(), payload));
    // Assert
    assertThat(event).containsEntry("proveedor", "MERCADO_PAGO").containsEntry("evento_proveedor_id", "evt-1")
        .containsEntry("estado", "PENDIENTE_VALIDACION");
    assertThat(event.get("payload_json").toString()).contains("evt-1");
  }

  @Test
  @DisplayName("webhook sin id de evento se guarda sin evento_proveedor_id")
  void webhookSinId() {
    // Arrange
    var payload = Map.<String, Object>of("type", "payment");
    // Act
    var event = map(run("webhook", Map.of(), payload));
    // Assert
    assertThat(event).doesNotContainKey("evento_proveedor_id");
  }

  @Test
  @DisplayName("400 en webhook con payload vacío")
  void webhookVacio() {
    // Arrange + Act + Assert
    status(400, () -> run("webhook", Map.of(), Map.of()));
  }

  @Test
  @DisplayName("transactions lista las transacciones del préstamo solo a participantes")
  void listaTransacciones() {
    // Arrange
    var id = loan(false);
    login("borrower");
    run("payment", Map.of(), pago(id, "k-10"));
    // Act
    var txs = list(run("transactions", Map.of("id", id), Map.of()));
    // Assert
    assertThat(txs).hasSize(1);
    login("third");
    status(403, () -> run("transactions", Map.of("id", id), Map.of()));
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
