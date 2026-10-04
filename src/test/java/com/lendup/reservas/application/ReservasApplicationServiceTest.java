package com.lendup.reservas.application;

import static com.lendup.support.Asserts.list;
import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.DESDE;
import static com.lendup.support.TestWorld.HASTA;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.lendup.support.TestWorld;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Reservas - servicio de aplicación")
class ReservasApplicationServiceTest {

  private TestWorld w;
  private String ownerId;
  private String borrowerId;
  private Map<String, Object> pub;

  @BeforeEach
  void setUp() {
    w = new TestWorld();
    ownerId = w.usuario("owner");
    borrowerId = w.usuario("borrower");
    w.usuario("third");
    pub = w.publicacion(ownerId, new BigDecimal("50.00"));
    w.disponibilidad(pub.get("id"), "2026-10-01T00:00:00", "2026-12-31T00:00:00");
  }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private Object run(String action, Map<String, String> vars, Map<String, Object> body, Map<String, String> query) {
    return w.reservas.execute(action, vars, body, query);
  }

  private Object accept(String requestId) { return run("acceptRequest", Map.of("id", requestId), Map.of(), Map.of()); }

  private Map<String, Object> body(Object publicacionId, String desde, String hasta) {
    var b = new LinkedHashMap<String, Object>();
    b.put("publicacion_id", publicacionId);
    b.put("desde", desde);
    b.put("hasta", hasta);
    return b;
  }

  @Nested
  @DisplayName("createRequest")
  class CrearSolicitud {

    @Test
    @DisplayName("crea una solicitud PENDIENTE copiando las condiciones publicadas y notifica al propietario")
    void creaSolicitud() {
      // Arrange
      login("borrower");
      // Act
      var request = map(run("createRequest", Map.of(), body(pub.get("id"), DESDE, HASTA), Map.of()));
      // Assert
      assertThat(request).containsEntry("estado", "PENDIENTE")
          .containsEntry("prestatario_usuario_id", borrowerId)
          .containsEntry("prestamista_usuario_id", ownerId)
          .containsEntry("tarifa_diaria_aceptada", new BigDecimal("5.00"))
          .containsEntry("garantia_monetaria_aceptada", new BigDecimal("50.00"))
          .containsEntry("moneda_aceptada", "PEN")
          .containsEntry("lugar_intercambio_aceptado", "Puerta 3")
          .containsEntry("condiciones_uso_aceptadas", "Uso académico");
      assertThat(w.store.list("notificaciones", "destinatario_usuario_id", ownerId)).hasSize(1);
    }

    @Test
    @DisplayName("400 cuando desde no es anterior a hasta")
    void periodoInvertido() {
      // Arrange
      login("borrower");
      // Act + Assert
      status(400, () -> run("createRequest", Map.of(), body(pub.get("id"), HASTA, DESDE), Map.of()));
    }

    @Test
    @DisplayName("400 cuando las fechas no tienen formato ISO")
    void fechasInvalidas() {
      // Arrange
      login("borrower");
      // Act + Assert
      status(400, () -> run("createRequest", Map.of(), body(pub.get("id"), "mañana", HASTA), Map.of()));
    }

    @Test
    @DisplayName("404 cuando la publicación no existe")
    void publicacionInexistente() {
      // Arrange
      login("borrower");
      // Act + Assert
      status(404, () -> run("createRequest", Map.of(), body("no-existe", DESDE, HASTA), Map.of()));
    }

    @Test
    @DisplayName("409 cuando la publicación no está ACTIVA")
    void publicacionInactiva() {
      // Arrange
      w.store.update("publicaciones", pub.get("id").toString(), Map.of("estado", "PAUSADA"));
      login("borrower");
      // Act + Assert
      status(409, () -> run("createRequest", Map.of(), body(pub.get("id"), DESDE, HASTA), Map.of()));
    }

    @Test
    @DisplayName("409 cuando el periodo está fuera de la disponibilidad publicada")
    void fueraDeDisponibilidad() {
      // Arrange
      login("borrower");
      // Act + Assert
      status(409, () -> run("createRequest", Map.of(), body(pub.get("id"), "2027-01-05T09:00:00", "2027-01-06T09:00:00"), Map.of()));
    }

    @Test
    @DisplayName("409 cuando el propietario solicita su propio objeto")
    void propioObjeto() {
      // Arrange
      login("owner");
      // Act + Assert
      status(409, () -> run("createRequest", Map.of(), body(pub.get("id"), DESDE, HASTA), Map.of()));
    }

    @Test
    @DisplayName("403 cuando el usuario autenticado no registró su perfil")
    void usuarioSinPerfil() {
      // Arrange
      login("desconocido");
      // Act + Assert
      status(403, () -> run("createRequest", Map.of(), body(pub.get("id"), DESDE, HASTA), Map.of()));
    }
  }

  @Nested
  @DisplayName("acceptRequest")
  class AceptarSolicitud {

    @Test
    @DisplayName("confirma la reserva copiando las condiciones acordadas, crea el préstamo RESERVADO y notifica a ambos")
    void aceptaSolicitud() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("owner");
      // Act
      var reservation = map(accept(request.get("id").toString()));
      // Assert
      assertThat(reservation).containsEntry("estado", "CONFIRMADA")
          .containsEntry("tarifa_diaria_acordada", new BigDecimal("5.00"))
          .containsEntry("garantia_monetaria_acordada", new BigDecimal("50.00"))
          .containsEntry("moneda_acordada", "PEN")
          .containsEntry("lugar_intercambio_acordado", "Puerta 3")
          .containsEntry("condiciones_uso_acordadas", "Uso académico")
          .containsEntry("condiciones_entrega_acordadas", "En mano")
          .containsEntry("condiciones_devolucion_acordadas", "Limpia")
          .containsEntry("condiciones_cancelacion_acordadas", "24h antes");
      assertThat(w.store.get("solicitudes", request.get("id").toString())).containsEntry("estado", "ACEPTADA");
      var loans = w.store.list("prestamos", "reserva_id", reservation.get("id"));
      assertThat(loans).hasSize(1);
      assertThat(loans.getFirst()).containsEntry("estado", "RESERVADO").containsEntry("garantia_requerida", true);
      assertThat(w.store.list("notificaciones", "origen_id", reservation.get("id"))).hasSize(2);
    }

    @Test
    @DisplayName("crea la agenda del objeto la primera vez y la reutiliza después")
    void agendaSeReutiliza() {
      // Arrange
      var first = w.solicitud("borrower", pub.get("id"), "2026-10-05T09:00:00", "2026-10-06T09:00:00");
      var second = w.solicitud("borrower", pub.get("id"), "2026-10-08T09:00:00", "2026-10-09T09:00:00");
      login("owner");
      // Act
      accept(first.get("id").toString());
      accept(second.get("id").toString());
      // Assert
      assertThat(w.store.count("agendas_objeto")).isEqualTo(1);
      assertThat(w.store.count("reservas")).isEqualTo(2);
    }

    @Test
    @DisplayName("garantia_requerida es false si la publicación no exige garantía")
    void sinGarantia() {
      // Arrange
      var free = w.publicacion(ownerId, null);
      w.disponibilidad(free.get("id"), "2026-10-01T00:00:00", "2026-12-31T00:00:00");
      var request = w.solicitud("borrower", free.get("id"), DESDE, HASTA);
      login("owner");
      // Act
      var reservation = map(accept(request.get("id").toString()));
      // Assert
      assertThat(w.store.list("prestamos", "reserva_id", reservation.get("id")).getFirst()).containsEntry("garantia_requerida", false);
    }

    @Test
    @DisplayName("invoca lockAgenda antes de overlaps (control pesimista sobre la agenda)")
    void lockAntesDeOverlaps() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("owner");
      w.store.calls().clear();
      // Act
      accept(request.get("id").toString());
      // Assert
      assertThat(w.store.calls()).containsExactly("lockAgenda", "overlaps");
    }

    @Test
    @DisplayName("409 cuando otra reserva CONFIRMADA solapa el periodo (aceptación secuencial)")
    void solapamientoSecuencial() {
      // Arrange
      var a = w.solicitud("borrower", pub.get("id"), "2026-10-05T09:00:00", "2026-10-07T09:00:00");
      var b = w.solicitud("third", pub.get("id"), "2026-10-06T09:00:00", "2026-10-08T09:00:00");
      login("owner");
      accept(a.get("id").toString());
      // Act + Assert
      status(409, () -> accept(b.get("id").toString()));
      assertThat(w.store.count("reservas")).isEqualTo(1);
      assertThat(w.store.get("solicitudes", b.get("id").toString())).containsEntry("estado", "PENDIENTE");
    }

    @Test
    @DisplayName("periodos contiguos (hasta == desde) no se consideran solapados")
    void periodosContiguos() {
      // Arrange
      var a = w.solicitud("borrower", pub.get("id"), "2026-10-05T09:00:00", "2026-10-07T09:00:00");
      var b = w.solicitud("third", pub.get("id"), "2026-10-07T09:00:00", "2026-10-08T09:00:00");
      login("owner");
      // Act
      accept(a.get("id").toString());
      accept(b.get("id").toString());
      // Assert
      assertThat(w.store.count("reservas")).isEqualTo(2);
    }

    @Test
    @DisplayName("una reserva CANCELADA no bloquea el periodo")
    void reservaCanceladaNoBloquea() {
      // Arrange
      var a = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      var b = w.solicitud("third", pub.get("id"), DESDE, HASTA);
      login("owner");
      var reservation = map(accept(a.get("id").toString()));
      run("cancelReservation", Map.of("id", reservation.get("id").toString()), Map.of(), Map.of());
      // Act
      var second = map(accept(b.get("id").toString()));
      // Assert
      assertThat(second).containsEntry("estado", "CONFIRMADA");
    }

    @Test
    @DisplayName("403 cuando acepta el prestatario en vez del propietario")
    void aceptaPrestatario() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("borrower");
      // Act + Assert
      status(403, () -> accept(request.get("id").toString()));
    }

    @Test
    @DisplayName("403 cuando acepta un tercero")
    void aceptaTercero() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("third");
      // Act + Assert
      status(403, () -> accept(request.get("id").toString()));
    }

    @Test
    @DisplayName("404 cuando la solicitud no existe")
    void solicitudInexistente() {
      // Arrange
      login("owner");
      // Act + Assert
      status(404, () -> accept("no-existe"));
    }

    @Test
    @DisplayName("409 cuando la solicitud ya no está PENDIENTE")
    void solicitudYaAceptada() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("owner");
      accept(request.get("id").toString());
      // Act + Assert
      status(409, () -> accept(request.get("id").toString()));
    }
  }

  @Nested
  @DisplayName("rejectRequest / cancelRequest")
  class RechazarCancelar {

    @Test
    @DisplayName("el propietario rechaza con motivo y la solicitud pasa a RECHAZADA")
    void rechaza() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("owner");
      // Act
      var result = map(run("rejectRequest", Map.of("id", request.get("id").toString()), Map.of("motivo_rechazo", "No disponible"), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "RECHAZADA").containsEntry("motivo_rechazo", "No disponible");
      assertThat(result.get("respondida_en")).isNotNull();
    }

    @Test
    @DisplayName("rechazar sin motivo no escribe motivo_rechazo")
    void rechazaSinMotivo() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("owner");
      // Act
      var result = map(run("rejectRequest", Map.of("id", request.get("id").toString()), Map.of(), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "RECHAZADA").doesNotContainKey("motivo_rechazo");
    }

    @Test
    @DisplayName("403 cuando rechaza el prestatario")
    void rechazaPrestatario() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("borrower");
      // Act + Assert
      status(403, () -> run("rejectRequest", Map.of("id", request.get("id").toString()), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("el prestatario cancela su solicitud y pasa a CANCELADA")
    void cancela() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      // Act
      var result = map(run("cancelRequest", Map.of("id", request.get("id").toString()), Map.of(), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "CANCELADA");
      assertThat(result.get("cancelada_en")).isNotNull();
    }

    @Test
    @DisplayName("403 cuando el propietario intenta cancelar la solicitud del prestatario")
    void cancelaPropietario() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      login("owner");
      // Act + Assert
      status(403, () -> run("cancelRequest", Map.of("id", request.get("id").toString()), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("409 al cancelar una solicitud que ya no está PENDIENTE")
    void cancelaNoPendiente() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      run("cancelRequest", Map.of("id", request.get("id").toString()), Map.of(), Map.of());
      // Act + Assert
      status(409, () -> run("cancelRequest", Map.of("id", request.get("id").toString()), Map.of(), Map.of()));
    }
  }

  @Nested
  @DisplayName("consultas")
  class Consultas {

    @Test
    @DisplayName("request devuelve la solicitud a un participante y 403 a un tercero")
    void soloParticipantes() {
      // Arrange
      var request = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      var id = request.get("id").toString();
      // Act
      var visible = map(run("request", Map.of("id", id), Map.of(), Map.of()));
      // Assert
      assertThat(visible).containsEntry("id", id);
      login("third");
      status(403, () -> run("request", Map.of("id", id), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("listRequests filtra por rol y por estado")
    void listaSolicitudes() {
      // Arrange
      var first = w.solicitud("borrower", pub.get("id"), DESDE, HASTA);
      w.solicitud("borrower", pub.get("id"), "2026-11-05T09:00:00", "2026-11-06T09:00:00");
      run("cancelRequest", Map.of("id", first.get("id").toString()), Map.of(), Map.of());
      // Act
      var asBorrower = list(run("listRequests", Map.of(), Map.of(), Map.of("rol", "PRESTATARIO")));
      var asLender = list(run("listRequests", Map.of(), Map.of(), Map.of("rol", "PRESTAMISTA")));
      var pending = list(run("listRequests", Map.of(), Map.of(), Map.of("estado", "PENDIENTE")));
      var both = list(run("listRequests", Map.of(), Map.of(), Map.of()));
      // Assert
      assertThat(asBorrower).hasSize(2);
      assertThat(asLender).isEmpty();
      assertThat(pending).hasSize(1);
      assertThat(both).hasSize(2);
    }

    @Test
    @DisplayName("listReservations devuelve solo las reservas del usuario según su rol")
    void listaReservas() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      // Act
      login("borrower");
      var asBorrower = list(run("listReservations", Map.of(), Map.of(), Map.of("rol", "PRESTATARIO")));
      var asLenderOfNothing = list(run("listReservations", Map.of(), Map.of(), Map.of("rol", "PRESTAMISTA")));
      login("owner");
      var asLender = list(run("listReservations", Map.of(), Map.of(), Map.of("rol", "PRESTAMISTA")));
      var any = list(run("listReservations", Map.of(), Map.of(), Map.of()));
      login("third");
      var thirdParty = list(run("listReservations", Map.of(), Map.of(), Map.of()));
      // Assert
      assertThat(asBorrower).extracting(r -> r.get("id")).containsExactly(loan.get("reserva_id"));
      assertThat(asLenderOfNothing).isEmpty();
      assertThat(asLender).hasSize(1);
      assertThat(any).hasSize(1);
      assertThat(thirdParty).isEmpty();
    }
  }

  @Nested
  @DisplayName("cancelReservation y contact")
  class ReservaConfirmada {

    @Test
    @DisplayName("un participante cancela la reserva: queda CANCELADA y el préstamo CANCELADO")
    void cancelaReserva() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      login("borrower");
      // Act
      var result = map(run("cancelReservation", Map.of("id", loan.get("reserva_id").toString()), Map.of("motivo_cancelacion", "Cambio de planes"), Map.of()));
      // Assert
      assertThat(result).containsEntry("estado", "CANCELADA")
          .containsEntry("cancelada_por_usuario_id", borrowerId)
          .containsEntry("motivo_cancelacion", "Cambio de planes");
      assertThat(w.store.get("prestamos", loan.get("id").toString())).containsEntry("estado", "CANCELADO");
    }

    @Test
    @DisplayName("403 cuando un tercero cancela la reserva")
    void cancelaTercero() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      login("third");
      // Act + Assert
      status(403, () -> run("cancelReservation", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("409 cuando la reserva ya está CANCELADA")
    void cancelaDosVeces() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      login("owner");
      run("cancelReservation", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of());
      // Act + Assert
      status(409, () -> run("cancelReservation", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("409 cuando el préstamo ya fue iniciado")
    void prestamoIniciado() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "ACTIVO", false);
      login("owner");
      // Act + Assert
      status(409, () -> run("cancelReservation", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("contact devuelve el teléfono de la contraparte")
    void contacto() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      login("borrower");
      // Act
      var contact = map(run("contact", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
      // Assert
      assertThat(contact).containsEntry("telefono", "999-owner");
    }

    @Test
    @DisplayName("contact devuelve un mapa vacío si la contraparte no tiene perfil")
    void contactoSinPerfil() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      var profile = w.store.list("perfiles", "usuario_id", ownerId).getFirst();
      w.store.seed("perfiles", Map.of("id", profile.get("id"), "usuario_id", "otro"));
      login("borrower");
      // Act
      var contact = map(run("contact", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
      // Assert
      assertThat(contact).isEmpty();
    }

    @Test
    @DisplayName("403 en contact para un tercero")
    void contactoTercero() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      login("third");
      // Act + Assert
      status(403, () -> run("contact", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
    }

    @Test
    @DisplayName("403 en contact si la reserva no está CONFIRMADA")
    void contactoReservaCancelada() {
      // Arrange
      var loan = w.prestamo(ownerId, borrowerId, "RESERVADO", false);
      login("owner");
      run("cancelReservation", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of());
      // Act + Assert
      status(403, () -> run("contact", Map.of("id", loan.get("reserva_id").toString()), Map.of(), Map.of()));
    }
  }

  @Test
  @DisplayName("400 ante una acción desconocida")
  void accionDesconocida() {
    // Arrange
    login("owner");
    // Act + Assert
    status(400, () -> run("noExiste", Map.of(), Map.of(), Map.of()));
  }
}
