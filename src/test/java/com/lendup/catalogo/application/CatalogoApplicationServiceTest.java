package com.lendup.catalogo.application;

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
import org.junit.jupiter.api.Test;

@DisplayName("Catálogo - servicio de aplicación")
class CatalogoApplicationServiceTest {

  private TestWorld w;
  private String ownerId;
  private Map<String, Object> pub;

  @BeforeEach
  void setUp() {
    w = new TestWorld();
    ownerId = w.usuario("owner");
    w.usuario("other");
    w.usuario("borrower");
    pub = w.publicacion(ownerId, null);
  }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private Object run(String action, Map<String, String> vars, Map<String, Object> body, Map<String, String> query) {
    return w.catalogo.execute(action, vars, body, query);
  }

  private Map<String, Object> nueva() {
    var b = new LinkedHashMap<>(pub);
    b.keySet().removeAll(java.util.List.of("id", "estado", "propietario_usuario_id"));
    return b;
  }

  @Test
  @DisplayName("createPublication asigna como propietario al usuario autenticado aunque el cuerpo envíe otro")
  void creaPublicacion() {
    // Arrange
    login("other");
    var body = nueva();
    body.put("propietario_usuario_id", ownerId);
    var otherId = w.store.list("usuarios", "firebase_uid", "other").getFirst().get("id");
    // Act
    var created = map(run("createPublication", Map.of(), body, Map.of()));
    // Assert
    assertThat(created).containsEntry("propietario_usuario_id", otherId).containsEntry("estado", "ACTIVA");
  }

  @Test
  @DisplayName("400 cuando faltan campos obligatorios de la publicación")
  void publicacionIncompleta() {
    // Arrange
    login("owner");
    var body = nueva();
    body.remove("titulo");
    // Act + Assert
    status(400, () -> run("createPublication", Map.of(), body, Map.of()));
  }

  @Test
  @DisplayName("updatePublication permite al propietario cambiar campos y categoria_id")
  void actualizaPublicacion() {
    // Arrange
    login("owner");
    var otraCategoria = w.store.create("categorias", Map.of("codigo", "LIBROS", "nombre", "Libros"));
    // Act
    var updated = map(run("updatePublication", Map.of("id", pub.get("id").toString()),
        Map.of("titulo", "Nuevo título", "categoria_id", otraCategoria.get("id")), Map.of()));
    // Assert
    assertThat(updated).containsEntry("titulo", "Nuevo título").containsEntry("categoria_id", otraCategoria.get("id"));
  }

  @Test
  @DisplayName("publicationState cambia el estado pero ignora columnas *_id")
  void cambiaEstado() {
    // Arrange
    login("owner");
    // Act
    var updated = map(run("publicationState", Map.of("id", pub.get("id").toString()),
        Map.of("estado", "PAUSADA", "categoria_id", "otra"), Map.of()));
    // Assert
    assertThat(updated).containsEntry("estado", "PAUSADA").containsEntry("categoria_id", TestWorld.CATEGORIA);
  }

  @Test
  @DisplayName("403 cuando un no propietario modifica la publicación")
  void modificaNoPropietario() {
    // Arrange
    login("other");
    var id = Map.of("id", pub.get("id").toString());
    // Act + Assert
    status(403, () -> run("updatePublication", id, Map.of("titulo", "x"), Map.of()));
    status(403, () -> run("publicationState", id, Map.of("estado", "PAUSADA"), Map.of()));
    status(403, () -> run("availability", id, Map.of("desde", DESDE, "hasta", HASTA), Map.of()));
  }

  @Test
  @DisplayName("404 cuando la publicación no existe")
  void publicacionInexistente() {
    // Arrange
    login("owner");
    // Act + Assert
    status(404, () -> run("publication", Map.of("id", "no-existe"), Map.of(), Map.of()));
    status(404, () -> run("updatePublication", Map.of("id", "no-existe"), Map.of(), Map.of()));
  }

  @Test
  @DisplayName("availability registra una ventana de disponibilidad para el propietario")
  void registraDisponibilidad() {
    // Arrange
    login("owner");
    // Act
    var slot = map(run("availability", Map.of("id", pub.get("id").toString()), Map.of("desde", DESDE, "hasta", HASTA), Map.of()));
    // Assert
    assertThat(slot).containsEntry("publicacion_id", pub.get("id"));
    assertThat(w.store.offered(pub.get("id").toString(), DESDE, HASTA)).isTrue();
  }

  @Test
  @DisplayName("400 en availability con periodo inválido")
  void disponibilidadInvalida() {
    // Arrange
    login("owner");
    // Act + Assert
    status(400, () -> run("availability", Map.of("id", pub.get("id").toString()), Map.of("desde", HASTA, "hasta", DESDE), Map.of()));
  }

  @Test
  @DisplayName("listPublications filtra por nombre, campus y categoría y solo devuelve ACTIVA")
  void buscaPorFiltros() {
    // Arrange
    login("borrower");
    var libro = w.publicacion(ownerId, null);
    w.store.update("publicaciones", libro.get("id").toString(), Map.of("titulo", "Libro de cálculo", "campus", "Norte"));
    var pausada = w.publicacion(ownerId, null);
    w.store.update("publicaciones", pausada.get("id").toString(), Map.of("estado", "PAUSADA"));
    // Act
    var todas = list(run("listPublications", Map.of(), Map.of(), Map.of()));
    var porNombre = list(run("listPublications", Map.of(), Map.of(), Map.of("nombre", "CÁLCULO")));
    var porCampus = list(run("listPublications", Map.of(), Map.of(), Map.of("campus", "Norte")));
    var porCategoria = list(run("listPublications", Map.of(), Map.of(), Map.of("categoria", "otra")));
    // Assert
    assertThat(todas).hasSize(2);
    assertThat(porNombre).extracting(p -> p.get("id")).containsExactly(libro.get("id"));
    assertThat(porCampus).hasSize(1);
    assertThat(porCategoria).isEmpty();
  }

  @Test
  @DisplayName("listPublications por periodo excluye objetos sin disponibilidad o con reserva CONFIRMADA")
  void buscaPorPeriodo() {
    // Arrange
    var disponible = w.publicacion(ownerId, null);
    w.disponibilidad(disponible.get("id"), "2026-10-01T00:00:00", "2026-12-31T00:00:00");
    var reservado = w.publicacion(ownerId, null);
    w.disponibilidad(reservado.get("id"), "2026-10-01T00:00:00", "2026-12-31T00:00:00");
    var agenda = w.store.create("agendas_objeto", Map.of("publicacion_id", reservado.get("id")));
    w.store.seed("reservas", Map.of("id", "r-fijo", "solicitud_id", "s", "agenda_id", agenda.get("id"),
        "desde", java.time.LocalDateTime.parse(DESDE), "hasta", java.time.LocalDateTime.parse(HASTA), "estado", "CONFIRMADA"));
    login("borrower");
    // Act
    var result = list(run("listPublications", Map.of(), Map.of(), Map.of("desde", DESDE, "hasta", HASTA)));
    // Assert
    assertThat(result).extracting(p -> p.get("id")).contains(disponible.get("id")).doesNotContain(reservado.get("id"), pub.get("id"));
  }

  @Test
  @DisplayName("publication devuelve la publicación y termsDocument un mensaje informativo")
  void consultas() {
    // Arrange
    login("borrower");
    // Act
    var found = map(run("publication", Map.of("id", pub.get("id").toString()), Map.of(), Map.of()));
    var terms = map(run("termsDocument", Map.of(), Map.of(), Map.of()));
    // Assert
    assertThat(found).containsEntry("titulo", "Calculadora científica").containsEntry("tarifa_diaria", new BigDecimal("5.00"));
    assertThat(terms).containsKey("message");
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
