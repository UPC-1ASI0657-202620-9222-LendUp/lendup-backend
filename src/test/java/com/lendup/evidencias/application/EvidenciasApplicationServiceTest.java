package com.lendup.evidencias.application;

import static com.lendup.support.Asserts.list;
import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.lendup.support.TestWorld;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Evidencias e incidencias - servicio de aplicación")
class EvidenciasApplicationServiceTest {

  private TestWorld w;
  private String lenderId;
  private String borrowerId;
  private String loanId;

  @BeforeEach
  void setUp() {
    w = new TestWorld();
    lenderId = w.usuario("lender");
    borrowerId = w.usuario("borrower");
    w.usuario("third");
    w.admin("admin");
    loanId = w.prestamo(lenderId, borrowerId, "ACTIVO", false).get("id").toString();
  }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private Object run(String action, Map<String, String> vars, Map<String, Object> body, Map<String, String> query) {
    return w.evidencias.execute(action, vars, body, query);
  }

  private Object run(String action, Map<String, String> vars, Map<String, Object> body) { return run(action, vars, body, Map.of()); }

  private Map<String, Object> evidencia(String etapa, String tipo) {
    var b = new LinkedHashMap<String, Object>();
    b.put("etapa", etapa);
    b.put("tipo", tipo);
    return b;
  }

  private Map<String, Object> incidencia() {
    return new LinkedHashMap<>(Map.of("prestamo_id", loanId, "tipo", "DANIO", "descripcion", "Rayón en la tapa"));
  }

  @Nested
  @DisplayName("evidence")
  class Evidencia {

    @Test
    @DisplayName("FOTO con url y cloudinary_public_id queda REFERENCIA_REGISTRADA")
    void fotoConReferencia() {
      // Arrange
      login("borrower");
      var body = evidencia("ENTREGA", "FOTO");
      body.put("url", "https://img/1.jpg");
      body.put("cloudinary_public_id", "pub-1");
      // Act
      var result = map(run("evidence", Map.of("id", loanId), body));
      // Assert
      assertThat(result).containsEntry("estado_integracion", "REFERENCIA_REGISTRADA").containsEntry("url", "https://img/1.jpg")
          .containsEntry("registrada_por_usuario_id", borrowerId).containsEntry("prestamo_id", loanId);
    }

    @Test
    @DisplayName("FOTO sin referencia queda PENDIENTE y descarta url parcial")
    void fotoPendiente() {
      // Arrange
      login("borrower");
      var body = evidencia("ENTREGA", "FOTO");
      body.put("url", "https://img/2.jpg");
      // Act
      var result = map(run("evidence", Map.of("id", loanId), body));
      // Assert
      assertThat(result).containsEntry("estado_integracion", "PENDIENTE").doesNotContainKey("url");
    }

    @Test
    @DisplayName("OBSERVACION con texto queda COMPLETADA")
    void observacion() {
      // Arrange
      login("lender");
      var body = evidencia("DEVOLUCION", "OBSERVACION");
      body.put("observacion", "Sin novedades");
      // Act
      var result = map(run("evidence", Map.of("id", loanId), body));
      // Assert
      assertThat(result).containsEntry("estado_integracion", "COMPLETADA").containsEntry("observacion", "Sin novedades");
    }

    @Test
    @DisplayName("400 en OBSERVACION sin texto y en tipo desconocido")
    void evidenciaInvalida() {
      // Arrange
      login("lender");
      // Act + Assert
      status(400, () -> run("evidence", Map.of("id", loanId), evidencia("ENTREGA", "OBSERVACION")));
      status(400, () -> run("evidence", Map.of("id", loanId), evidencia("ENTREGA", "AUDIO")));
    }

    @Test
    @DisplayName("403 cuando un tercero registra evidencia")
    void evidenciaTercero() {
      // Arrange
      login("third");
      // Act + Assert
      status(403, () -> run("evidence", Map.of("id", loanId), evidencia("ENTREGA", "FOTO")));
    }
  }

  @Nested
  @DisplayName("analyze")
  class Analisis {

    private String evidencia(String etapa, String prestamo) {
      return w.store.create("evidencias", Map.of("prestamo_id", prestamo, "registrada_por_usuario_id", borrowerId, "etapa", etapa, "tipo", "FOTO")).get("id").toString();
    }

    @Test
    @DisplayName("crea el análisis PENDIENTE con evidencia de ENTREGA y de DEVOLUCION del mismo préstamo")
    void analiza() {
      // Arrange
      login("borrower");
      var body = Map.<String, Object>of("evidencia_inicial_id", evidencia("ENTREGA", loanId), "evidencia_final_id", evidencia("DEVOLUCION", loanId));
      // Act
      var result = map(run("analyze", Map.of("id", loanId), body));
      // Assert
      assertThat(result).containsEntry("estado", "PENDIENTE").containsEntry("solicitado_por_usuario_id", borrowerId);
    }

    @Test
    @DisplayName("400 cuando las evidencias son de otro préstamo o de etapas invertidas")
    void evidenciasIncompatibles() {
      // Arrange
      login("borrower");
      var otro = w.prestamo(lenderId, borrowerId, "ACTIVO", false).get("id").toString();
      var deOtro = Map.<String, Object>of("evidencia_inicial_id", evidencia("ENTREGA", otro), "evidencia_final_id", evidencia("DEVOLUCION", loanId));
      var invertidas = Map.<String, Object>of("evidencia_inicial_id", evidencia("DEVOLUCION", loanId), "evidencia_final_id", evidencia("ENTREGA", loanId));
      // Act + Assert
      status(400, () -> run("analyze", Map.of("id", loanId), deOtro));
      status(400, () -> run("analyze", Map.of("id", loanId), invertidas));
    }

    @Test
    @DisplayName("404 cuando la evidencia no existe y 403 a un tercero")
    void analisisInvalido() {
      // Arrange
      var body = Map.<String, Object>of("evidencia_inicial_id", "no-existe", "evidencia_final_id", "tampoco");
      // Act + Assert
      login("borrower");
      status(404, () -> run("analyze", Map.of("id", loanId), body));
      login("third");
      status(403, () -> run("analyze", Map.of("id", loanId), body));
    }
  }

  @Nested
  @DisplayName("incidencias")
  class Incidencias {

    @Test
    @DisplayName("incident crea la incidencia PENDIENTE e incrementa incidencias_pendientes del préstamo")
    void reportaIncidencia() {
      // Arrange
      login("lender");
      // Act
      var incident = map(run("incident", Map.of(), incidencia()));
      // Assert
      assertThat(incident).containsEntry("estado", "PENDIENTE").containsEntry("reportada_por_usuario_id", lenderId);
      assertThat(w.store.get("prestamos", loanId)).containsEntry("incidencias_pendientes", 1);
    }

    @Test
    @DisplayName("403 cuando un tercero reporta una incidencia")
    void incidenciaTercero() {
      // Arrange
      login("third");
      // Act + Assert
      status(403, () -> run("incident", Map.of(), incidencia()));
    }

    @Test
    @DisplayName("incidentById: participante la ve, tercero recibe 403, inexistente 404")
    void consultaIncidencia() {
      // Arrange
      login("lender");
      var id = map(run("incident", Map.of(), incidencia())).get("id").toString();
      // Act
      login("borrower");
      var found = map(run("incidentById", Map.of("id", id), Map.of()));
      // Assert
      assertThat(found).containsEntry("id", id);
      login("third");
      status(403, () -> run("incidentById", Map.of("id", id), Map.of()));
      status(404, () -> run("incidentById", Map.of("id", "no-existe"), Map.of()));
    }

    @Test
    @DisplayName("adminIncidents lista y filtra por tipo para el administrador; 403 a un estudiante")
    void listaAdmin() {
      // Arrange
      login("lender");
      run("incident", Map.of(), incidencia());
      var otra = incidencia();
      otra.put("tipo", "RETRASO");
      run("incident", Map.of(), otra);
      // Act
      login("admin");
      var todas = list(run("adminIncidents", Map.of(), Map.of(), Map.of("estado", "PENDIENTE")));
      var retrasos = list(run("adminIncidents", Map.of(), Map.of(), Map.of("tipo", "RETRASO")));
      // Assert
      assertThat(todas).hasSize(2);
      assertThat(retrasos).hasSize(1);
      login("lender");
      status(403, () -> run("adminIncidents", Map.of(), Map.of(), Map.of()));
    }

    private Map<String, Object> resolucion() {
      return new LinkedHashMap<>(Map.of("justificacion_resolucion", "Daño menor", "decision_garantia", "RETENER_PARCIAL",
          "monto_garantia_afectado", 10, "saldo_garantia_previsto", 40));
    }

    @Test
    @DisplayName("resolve marca la incidencia RESUELTA y descuenta incidencias_pendientes")
    void resuelve() {
      // Arrange
      login("lender");
      var id = map(run("incident", Map.of(), incidencia())).get("id").toString();
      login("admin");
      // Act
      var result = map(run("resolve", Map.of("id", id), resolucion()));
      // Assert
      assertThat(result).containsEntry("estado", "RESUELTA").containsEntry("decision_garantia", "RETENER_PARCIAL");
      assertThat(w.store.get("prestamos", loanId)).containsEntry("incidencias_pendientes", 0);
    }

    @Test
    @DisplayName("resolve: 403 a no administradores")
    void resuelveNoAdmin() {
      // Arrange
      login("lender");
      var id = map(run("incident", Map.of(), incidencia())).get("id").toString();
      // Act + Assert
      status(403, () -> run("resolve", Map.of("id", id), resolucion()));
    }

    @Test
    @DisplayName("resolve: 409 si ya está resuelta y 400 si falta algún campo de resolución")
    void resolucionInvalida() {
      // Arrange
      login("lender");
      var id = map(run("incident", Map.of(), incidencia())).get("id").toString();
      login("admin");
      var incompleta = resolucion();
      incompleta.remove("decision_garantia");
      status(400, () -> run("resolve", Map.of("id", id), incompleta));
      run("resolve", Map.of("id", id), resolucion());
      // Act + Assert
      status(409, () -> run("resolve", Map.of("id", id), resolucion()));
    }

    @Test
    @DisplayName("resolve: 404 si la incidencia no existe")
    void resuelveInexistente() {
      // Arrange
      login("admin");
      // Act + Assert
      status(404, () -> run("resolve", Map.of("id", "no-existe"), resolucion()));
    }
  }

  @Test
  @DisplayName("400 ante una acción desconocida")
  void accionDesconocida() {
    // Arrange
    login("lender");
    // Act + Assert
    status(400, () -> run("noExiste", Map.of(), Map.of()));
  }
}
