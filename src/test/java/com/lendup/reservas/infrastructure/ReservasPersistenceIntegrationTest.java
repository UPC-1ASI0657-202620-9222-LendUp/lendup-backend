package com.lendup.reservas.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lendup.catalogo.application.CatalogoApplicationService;
import com.lendup.reservas.application.ReservasApplicationService;
import com.lendup.shared.Store;
import com.lendup.support.TestWorld;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Nivel 2: servicio de Reservas + Store + schema.sql/data.sql contra MySQL real (InnoDB, REPEATABLE READ).
 * Se omite automáticamente si no hay MySQL. Variables: LENDUP_TEST_DB_URL, LENDUP_TEST_DB_USER, LENDUP_TEST_DB_PASSWORD
 * (por defecto root/root en lendup_test, localhost:3306).
 */
@SpringBootTest
@EnabledIf("mysqlDisponible")
@DisplayName("Reservas - integración de persistencia (MySQL real)")
class ReservasPersistenceIntegrationTest {

  private static final String URL = env("LENDUP_TEST_DB_URL",
      "jdbc:mysql://localhost:3306/lendup_test?createDatabaseIfNotExist=true&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false");
  private static final String USER = env("LENDUP_TEST_DB_USER", "root");
  private static final String PASSWORD = env("LENDUP_TEST_DB_PASSWORD", "root");
  private static final int RONDAS = 8;
  private static final int HILOS = 5;

  private static String env(String name, String fallback) {
    var v = System.getenv(name);
    return v == null || v.isBlank() ? fallback : v;
  }

  static boolean mysqlDisponible() {
    try (var c = DriverManager.getConnection(URL, USER, PASSWORD)) {
      return c.isValid(2);
    } catch (Exception e) {
      return false;
    }
  }

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> URL);
    registry.add("spring.datasource.username", () -> USER);
    registry.add("spring.datasource.password", () -> PASSWORD);
  }

  @Autowired ReservasApplicationService reservas;
  @Autowired CatalogoApplicationService catalogo;
  @Autowired Store store;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager txManager;
  @MockitoBean FirebaseAuth firebaseAuth;

  private String ownerId;
  private String pubId;

  @BeforeEach
  void setUp() {
    jdbc.execute((java.sql.Connection c) -> {
      try (var st = c.createStatement()) {
        st.execute("SET FOREIGN_KEY_CHECKS=0");
        for (var t : jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'", String.class))
          st.execute("TRUNCATE TABLE `" + t + "`");
        st.execute("SET FOREIGN_KEY_CHECKS=1");
      }
      return null;
    });
    new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(jdbc.getDataSource());
    ownerId = usuario("owner");
    pubId = store.create("publicaciones", publicacion(ownerId)).get("id").toString();
    store.create("disponibilidades_publicacion", Map.of("publicacion_id", pubId, "desde", "2026-10-01T00:00:00", "hasta", "2026-12-31T00:00:00"));
  }

  @AfterEach
  void tearDown() { TestWorld.logout(); }

  private String usuario(String uid) {
    return store.create("usuarios", Map.of("firebase_uid", uid, "correo_institucional", uid + "@uni.edu", "rol_id", TestWorld.ROL_ESTUDIANTE))
        .get("id").toString();
  }

  private Map<String, Object> publicacion(String owner) {
    var d = new LinkedHashMap<String, Object>();
    d.put("propietario_usuario_id", owner);
    d.put("categoria_id", TestWorld.CATEGORIA);
    d.put("titulo", "Calculadora");
    d.put("descripcion", "Casio");
    d.put("condicion_objeto", "BUENA");
    d.put("universidad", "UNI");
    d.put("campus", "Central");
    d.put("ubicacion", "Biblioteca");
    d.put("lugar_intercambio", "Puerta 3");
    d.put("tarifa_diaria", new BigDecimal("5.00"));
    d.put("moneda", "PEN");
    d.put("condiciones_uso", "a");
    d.put("condiciones_entrega", "b");
    d.put("condiciones_devolucion", "c");
    d.put("condiciones_cancelacion", "d");
    return d;
  }

  /** Solicitud PENDIENTE de un nuevo prestatario sobre la publicación (vía createRequest, con su propia transacción). */
  private String solicitud(String borrowerUid, String desde, String hasta) {
    usuario(borrowerUid);
    TestWorld.login(borrowerUid);
    var body = new LinkedHashMap<String, Object>(Map.of("publicacion_id", pubId, "desde", desde, "hasta", hasta));
    return ((Map<?, ?>) reservas.execute("createRequest", Map.of(), body, Map.of())).get("id").toString();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> aceptar(String requestId) {
    return (Map<String, Object>) reservas.execute("acceptRequest", Map.of("id", requestId), Map.of(), Map.of());
  }

  private int reservasConfirmadas() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM reservas WHERE estado='CONFIRMADA'", Integer.class);
  }

  private static boolean es409(Throwable t) {
    return t instanceof ResponseStatusException e && e.getStatusCode().value() == 409;
  }

  @Test
  @DisplayName("la base de pruebas usa InnoDB con aislamiento REPEATABLE READ (precondición del diseño)")
  void aislamientoRepeatableRead() {
    // Arrange + Act
    var isolation = jdbc.queryForObject("SELECT @@transaction_isolation", String.class);
    var engine = jdbc.queryForObject("SELECT engine FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='reservas'", String.class);
    // Assert
    assertThat(isolation).isEqualTo("REPEATABLE-READ");
    assertThat(engine).isEqualTo("InnoDB");
  }

  @Test
  @DisplayName("aceptación secuencial de periodos solapados: la segunda recibe 409 y queda una sola reserva")
  void aceptacionSecuencialConSolapamiento() {
    // Arrange
    var a = solicitud("borrower-a", "2026-10-05T09:00:00", "2026-10-07T09:00:00");
    var b = solicitud("borrower-b", "2026-10-06T09:00:00", "2026-10-08T09:00:00");
    TestWorld.login("owner");
    // Act
    var primera = aceptar(a);
    // Assert
    assertThat(primera).containsEntry("estado", "CONFIRMADA");
    assertThatThrownBy(() -> aceptar(b)).matches(ReservasPersistenceIntegrationTest::es409);
    assertThat(reservasConfirmadas()).isEqualTo(1);
    assertThat(store.get("solicitudes", b)).containsEntry("estado", "PENDIENTE");
  }

  @Test
  @DisplayName("aceptar crea reserva y préstamo con las condiciones acordadas persistidas en MySQL")
  void aceptacionPersisteCondiciones() {
    // Arrange
    var a = solicitud("borrower-a", "2026-10-05T09:00:00", "2026-10-07T09:00:00");
    TestWorld.login("owner");
    // Act
    var reserva = aceptar(a);
    // Assert
    var fila = store.get("reservas", reserva.get("id").toString());
    assertThat(fila).containsEntry("lugar_intercambio_acordado", "Puerta 3").containsEntry("condiciones_uso_acordadas", "a")
        .containsEntry("condiciones_cancelacion_acordadas", "d");
    assertThat(store.list("prestamos", "reserva_id", reserva.get("id"))).hasSize(1);
  }

  private List<Throwable> aceptarEnParalelo(List<String> solicitudes) throws Exception {
    var pool = Executors.newFixedThreadPool(solicitudes.size());
    var listos = new CountDownLatch(solicitudes.size());
    var arranque = new CountDownLatch(1);
    var resultados = new ArrayList<Future<Throwable>>();
    try {
      for (var id : solicitudes) {
        Callable<Throwable> tarea = () -> {
          TestWorld.login("owner");
          listos.countDown();
          arranque.await();
          try {
            aceptar(id);
            return null;
          } catch (Throwable t) {
            return t;
          } finally {
            TestWorld.logout();
          }
        };
        resultados.add(pool.submit(tarea));
      }
      assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
      arranque.countDown();
      var fallos = new ArrayList<Throwable>();
      for (var f : resultados) fallos.add(f.get(30, TimeUnit.SECONDS));
      return fallos;
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("aceptaciones simultáneas del mismo periodo (varias rondas): exactamente 1 reserva, el resto 409")
  void aceptacionesSimultaneas() throws Exception {
    // Arrange: la agenda ya existe para aislar el protocolo de bloqueo de la creación inicial de la agenda
    store.create("agendas_objeto", Map.of("publicacion_id", pubId));
    for (int ronda = 0; ronda < RONDAS; ronda++) {
      var desde = "2026-11-%02dT09:00:00".formatted(ronda + 1);
      var hasta = "2026-11-%02dT18:00:00".formatted(ronda + 1);
      var ids = new ArrayList<String>();
      for (int i = 0; i < HILOS; i++) ids.add(solicitud("b-" + ronda + "-" + i, desde, hasta));
      // Act
      var fallos = aceptarEnParalelo(ids);
      // Assert
      var exitos = fallos.stream().filter(f -> f == null).count();
      var conflictos = fallos.stream().filter(f -> f != null && es409(f)).count();
      assertThat(exitos).as("ronda %d: aceptaciones exitosas", ronda).isEqualTo(1);
      assertThat(conflictos).as("ronda %d: rechazos 409 (otros fallos: %s)", ronda, fallos).isEqualTo(HILOS - 1);
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservas WHERE estado='CONFIRMADA' AND desde=?", Integer.class,
          desde.replace('T', ' '))).as("ronda %d: reservas del periodo", ronda).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("aceptaciones simultáneas sin agenda previa: nunca hay doble reserva (los perdedores reciben 409)")
  void aceptacionesSimultaneasSinAgenda() throws Exception {
    // Arrange
    var ids = new ArrayList<String>();
    for (int i = 0; i < HILOS; i++) ids.add(solicitud("c-" + i, "2026-12-01T09:00:00", "2026-12-01T18:00:00"));
    // Act
    var fallos = aceptarEnParalelo(ids);
    // Assert: la carrera por crear la agenda se traduce en DuplicateKeyException, que ApiErrors expone como 409
    assertThat(fallos.stream().filter(f -> f == null).count()).isEqualTo(1);
    assertThat(fallos.stream().filter(f -> f != null)).allMatch(f -> es409(f) || f instanceof org.springframework.dao.DuplicateKeyException);
    assertThat(reservasConfirmadas()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM agendas_objeto", Integer.class)).isEqualTo(1);
  }

  @Test
  @DisplayName("determinista: B toma snapshot antes del commit de A y obtiene el lock después; su chequeo debe ver la reserva de A")
  void chequeoPostLockVeReservaConfirmada() throws Exception {
    // Arrange
    store.create("agendas_objeto", Map.of("publicacion_id", pubId));
    var a = solicitud("borrower-a", "2026-10-05T09:00:00", "2026-10-07T09:00:00");
    var b = solicitud("borrower-b", "2026-10-06T09:00:00", "2026-10-08T09:00:00");
    var pool = Executors.newSingleThreadExecutor();
    var tx = new TransactionTemplate(txManager);
    Future<Throwable> resultadoB;
    try {
      // Act: A acepta dentro de una transacción externa que no se confirma hasta que B esté esperando el lock
      resultadoB = tx.execute(status -> {
        TestWorld.login("owner");
        aceptar(a);
        var futuro = pool.submit((Callable<Throwable>) () -> {
          TestWorld.login("owner");
          try {
            aceptar(b);
            return null;
          } catch (Throwable t) {
            return t;
          } finally {
            TestWorld.logout();
          }
        });
        esperarTransaccionEnLockWait(futuro);
        return futuro;
      });
      // Assert: A ya hizo commit; B (snapshot previo) debe detectar la reserva de A y fallar con 409
      var fallo = resultadoB.get(30, TimeUnit.SECONDS);
      assertThat(fallo).as("B debe ser rechazada con 409; si es null hubo doble reserva").isNotNull().matches(ReservasPersistenceIntegrationTest::es409);
    } finally {
      pool.shutdownNow();
    }
    assertThat(reservasConfirmadas()).isEqualTo(1);
  }

  /** Observa InnoDB desde una conexión autónoma (fuera de las transacciones bajo prueba) hasta ver una espera de lock. */
  private void esperarTransaccionEnLockWait(Future<Throwable> b) {
    var limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    try (var c = DriverManager.getConnection(URL, USER, PASSWORD); var st = c.createStatement()) {
      while (System.nanoTime() < limite) {
        try (var rs = st.executeQuery("SELECT COUNT(*) FROM performance_schema.data_lock_waits")) {
          rs.next();
          if (rs.getInt(1) > 0) return;
        }
        Thread.sleep(25);
      }
    } catch (java.sql.SQLException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
    throw new AssertionError("B nunca quedó esperando el lock de la agenda; estado de B: "
        + (b.isDone() ? "terminó " + resultado(b) : "en ejecución, sin lock wait"));
  }

  private static String resultado(Future<Throwable> f) {
    try {
      return "con " + f.get();
    } catch (Exception e) {
      return "con error " + e;
    }
  }

  @Test
  @DisplayName("la búsqueda por periodo excluye objetos con reserva CONFIRMADA solapada y respeta periodos libres")
  void busquedaExcluyePeriodosReservados() {
    // Arrange
    var a = solicitud("borrower-a", "2026-10-05T09:00:00", "2026-10-07T09:00:00");
    TestWorld.login("owner");
    aceptar(a);
    // Act
    var solapado = store.searchPublications(Map.of("desde", "2026-10-06T09:00:00", "hasta", "2026-10-06T18:00:00"));
    var libre = store.searchPublications(Map.of("desde", "2026-10-08T09:00:00", "hasta", "2026-10-09T09:00:00"));
    var contiguo = store.searchPublications(Map.of("desde", "2026-10-07T09:00:00", "hasta", "2026-10-08T09:00:00"));
    var sinPeriodo = store.searchPublications(Map.of());
    // Assert
    assertThat(solapado).isEmpty();
    assertThat(libre).extracting(p -> p.get("id")).containsExactly(pubId);
    assertThat(contiguo).extracting(p -> p.get("id")).containsExactly(pubId);
    assertThat(sinPeriodo).hasSize(1);
  }

  @Test
  @DisplayName("tras cancelar la reserva el periodo vuelve a aparecer en la búsqueda")
  void busquedaTrasCancelacion() {
    // Arrange
    var a = solicitud("borrower-a", "2026-10-05T09:00:00", "2026-10-07T09:00:00");
    TestWorld.login("owner");
    var reserva = aceptar(a);
    reservas.execute("cancelReservation", Map.of("id", reserva.get("id").toString()), Map.of(), Map.of());
    // Act
    var resultado = catalogo.execute("listPublications", Map.of(), Map.of(), Map.of("desde", "2026-10-05T09:00:00", "hasta", "2026-10-07T09:00:00"));
    // Assert
    assertThat((List<?>) resultado).hasSize(1);
  }
}
