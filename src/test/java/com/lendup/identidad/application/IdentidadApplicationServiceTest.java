package com.lendup.identidad.application;

import static com.lendup.support.Asserts.map;
import static com.lendup.support.Asserts.status;
import static com.lendup.support.TestWorld.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.lendup.support.InMemoryStore;
import com.lendup.support.TestWorld;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Identidad - servicio de aplicación")
class IdentidadApplicationServiceTest {

  private TestWorld w;

  @BeforeEach
  void setUp() { w = new TestWorld(); }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private Object run(String action, Map<String, String> vars, Map<String, Object> body) {
    return w.identidad.execute(action, vars, body, Map.of());
  }

  private Map<String, Object> registro() {
    var b = new LinkedHashMap<String, Object>();
    b.put("correo_institucional", "ana@uni.edu");
    b.put("nombre", "Ana");
    b.put("universidad", "UNI");
    b.put("campus", "Central");
    b.put("carrera", "Sistemas");
    b.put("ciclo", "5");
    b.put("telefono", "999");
    return b;
  }

  @Test
  @DisplayName("createUser registra al usuario con rol ESTUDIANTE, estado NO_VERIFICADO y su perfil")
  void registraUsuario() {
    // Arrange
    login("ana");
    // Act
    var user = map(run("createUser", Map.of(), registro()));
    // Assert
    assertThat(user).containsEntry("firebase_uid", "ana").containsEntry("rol_id", TestWorld.ROL_ESTUDIANTE)
        .containsEntry("estado_verificacion", "NO_VERIFICADO");
    assertThat(w.store.list("perfiles", "usuario_id", user.get("id"))).hasSize(1);
  }

  @Test
  @DisplayName("409 cuando el uid de Firebase ya está registrado")
  void usuarioDuplicado() {
    // Arrange
    login("ana");
    run("createUser", Map.of(), registro());
    // Act + Assert
    status(409, () -> run("createUser", Map.of(), registro()));
  }

  @Test
  @DisplayName("409 cuando falta el rol ESTUDIANTE en la base")
  void faltaRol() {
    // Arrange
    var vacio = new IdentidadApplicationService(new InMemoryStore(), w.json);
    login("ana");
    // Act + Assert
    status(409, () -> vacio.execute("createUser", Map.of(), registro(), Map.of()));
  }

  @Test
  @DisplayName("400 cuando el perfil no trae todos los campos obligatorios")
  void perfilIncompleto() {
    // Arrange
    login("ana");
    var body = registro();
    body.remove("telefono");
    // Act + Assert
    status(400, () -> run("createUser", Map.of(), body));
  }

  @Test
  @DisplayName("me devuelve usuario y perfil; 403 si el usuario no está registrado")
  void me() {
    // Arrange
    w.usuario("ana");
    login("ana");
    // Act
    var me = map(run("me", Map.of(), Map.of()));
    // Assert
    assertThat(map(me.get("usuario"))).containsEntry("firebase_uid", "ana");
    assertThat(map(me.get("perfil"))).containsEntry("nombre", "ana");
    login("nadie");
    status(403, () -> run("me", Map.of(), Map.of()));
  }

  @Test
  @DisplayName("me devuelve perfil vacío cuando el usuario aún no tiene perfil")
  void meSinPerfil() {
    // Arrange
    w.store.create("usuarios", Map.of("firebase_uid", "sinperfil", "correo_institucional", "s@uni.edu", "rol_id", TestWorld.ROL_ESTUDIANTE));
    login("sinperfil");
    // Act
    var me = map(run("me", Map.of(), Map.of()));
    // Assert
    assertThat(map(me.get("perfil"))).isEmpty();
  }

  @Test
  @DisplayName("updateMe actualiza el perfil del usuario autenticado")
  void actualizaPerfil() {
    // Arrange
    w.usuario("ana");
    login("ana");
    // Act
    var profile = map(run("updateMe", Map.of(), Map.of("nombre", "Ana María", "ciclo", "6")));
    // Assert
    assertThat(profile).containsEntry("nombre", "Ana María").containsEntry("ciclo", "6");
  }

  @Test
  @DisplayName("verify deja la verificación PENDIENTE con su referencia")
  void solicitaVerificacion() {
    // Arrange
    w.usuario("ana");
    login("ana");
    // Act
    var user = map(run("verify", Map.of(), Map.of("verificacion_referencia", "carnet-123")));
    // Assert
    assertThat(user).containsEntry("estado_verificacion", "PENDIENTE").containsEntry("verificacion_referencia", "carnet-123");
  }

  @Test
  @DisplayName("400 en verify sin verificacion_referencia")
  void verificacionSinReferencia() {
    // Arrange
    w.usuario("ana");
    login("ana");
    // Act + Assert
    status(400, () -> run("verify", Map.of(), Map.of()));
  }

  @Test
  @DisplayName("terms registra las versiones aceptadas")
  void aceptaTerminos() {
    // Arrange
    w.usuario("ana");
    login("ana");
    // Act
    var user = map(run("terms", Map.of(), Map.of("version_terminos_aceptada", "1.0", "version_descargo_aceptada", "1.0")));
    // Assert
    assertThat(user).containsEntry("version_terminos_aceptada", "1.0").containsEntry("version_descargo_aceptada", "1.0");
    assertThat(user.get("aceptados_en")).isNotNull();
  }

  @Test
  @DisplayName("400 en terms cuando falta alguna versión")
  void terminosIncompletos() {
    // Arrange
    w.usuario("ana");
    login("ana");
    // Act + Assert
    status(400, () -> run("terms", Map.of(), Map.of("version_terminos_aceptada", "1.0")));
  }

  @Test
  @DisplayName("userById expone solo datos públicos y no el correo ni el teléfono")
  void usuarioPublico() {
    // Arrange
    var id = w.usuario("ana");
    login("ana");
    // Act
    var pub = map(run("userById", Map.of("id", id), Map.of()));
    // Assert
    assertThat(pub).containsEntry("nombre", "ana").containsEntry("estado_verificacion", "NO_VERIFICADO")
        .doesNotContainKeys("telefono", "correo_institucional");
  }

  @Test
  @DisplayName("userById sin perfil devuelve solo id y estado; 404 si el usuario no existe")
  void usuarioPublicoSinPerfil() {
    // Arrange
    var u = w.store.create("usuarios", Map.of("firebase_uid", "x", "correo_institucional", "x@uni.edu", "rol_id", TestWorld.ROL_ESTUDIANTE));
    login("x");
    // Act
    var pub = map(run("userById", Map.of("id", u.get("id").toString()), Map.of()));
    // Assert
    assertThat(pub).containsOnlyKeys("id", "estado_verificacion");
    status(404, () -> run("userById", Map.of("id", "no-existe"), Map.of()));
  }

  @Test
  @DisplayName("400 ante una acción desconocida")
  void accionDesconocida() {
    // Arrange
    login("ana");
    // Act + Assert
    status(400, () -> run("noExiste", Map.of(), Map.of()));
  }
}
