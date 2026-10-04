package com.lendup.notificaciones.application;

import static com.lendup.support.Asserts.list;
import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.lendup.support.TestWorld;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Notificaciones - servicio de aplicación")
class NotificacionesApplicationServiceTest {

  private TestWorld w;
  private String anaId;
  private String lucasId;
  private String anaNoteId;

  @BeforeEach
  void setUp() {
    w = new TestWorld();
    anaId = w.usuario("ana");
    lucasId = w.usuario("lucas");
    login("ana");
    anaNoteId = nota(anaId, "n-1");
    nota(lucasId, "n-2");
  }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private String nota(String destinatario, String clave) {
    return w.store.create("notificaciones", Map.of("destinatario_usuario_id", destinatario, "clave_notificacion", clave, "tipo", "EVENTO",
        "origen_tipo", "RESERVA", "origen_id", "r-1", "titulo", "Aviso", "mensaje", "Hola", "estado", "DISPONIBLE")).get("id").toString();
  }

  private Object run(String action, Map<String, String> vars) { return w.notificaciones.execute(action, vars, Map.of(), Map.of()); }

  @Test
  @DisplayName("listNotifications devuelve solo las notificaciones del usuario autenticado")
  void listaPropias() {
    // Arrange
    login("ana");
    // Act
    var notes = list(run("listNotifications", Map.of()));
    // Assert
    assertThat(notes).hasSize(1).first().satisfies(n -> assertThat(n).containsEntry("destinatario_usuario_id", anaId));
  }

  @Test
  @DisplayName("readNotification marca la notificación como LEIDA con su fecha")
  void marcaLeida() {
    // Arrange
    login("ana");
    // Act
    var note = map(run("readNotification", Map.of("id", anaNoteId)));
    // Assert
    assertThat(note).containsEntry("estado", "LEIDA");
    assertThat(note.get("leida_en")).isNotNull();
  }

  @Test
  @DisplayName("409 al leer una notificación que ya no está DISPONIBLE")
  void yaLeida() {
    // Arrange
    login("ana");
    run("readNotification", Map.of("id", anaNoteId));
    // Act + Assert
    status(409, () -> run("readNotification", Map.of("id", anaNoteId)));
  }

  @Test
  @DisplayName("403 al leer la notificación de otro usuario y 404 si no existe")
  void ajenaOInexistente() {
    // Arrange
    login("lucas");
    // Act + Assert
    status(403, () -> run("readNotification", Map.of("id", anaNoteId)));
    status(404, () -> run("readNotification", Map.of("id", "no-existe")));
  }

  @Test
  @DisplayName("400 ante una acción desconocida")
  void accionDesconocida() {
    // Arrange
    login("ana");
    // Act + Assert
    status(400, () -> run("noExiste", Map.of()));
  }
}
