package com.lendup.support;

import com.lendup.catalogo.application.CatalogoApplicationService;
import com.lendup.evidencias.application.EvidenciasApplicationService;
import com.lendup.identidad.application.IdentidadApplicationService;
import com.lendup.notificaciones.application.NotificacionesApplicationService;
import com.lendup.pagos.application.PagosApplicationService;
import com.lendup.prestamos.application.PrestamosApplicationService;
import com.lendup.reputacion.application.ReputacionApplicationService;
import com.lendup.reservas.application.ReservasApplicationService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

/** Escenario base para pruebas unitarias: servicios reales sobre {@link InMemoryStore} y datos semilla de data.sql. */
public class TestWorld {
  public static final String ROL_ESTUDIANTE = "00000000-0000-4000-8000-000000000001";
  public static final String ROL_ADMIN = "00000000-0000-4000-8000-000000000002";
  public static final String CATEGORIA = "00000000-0000-4000-8000-000000000003";
  public static final String DESDE = "2026-10-05T09:00:00";
  public static final String HASTA = "2026-10-07T18:00:00";

  public final InMemoryStore store = new InMemoryStore();
  public final JsonMapper json = JsonMapper.builder().build();
  public final IdentidadApplicationService identidad = new IdentidadApplicationService(store, json);
  public final CatalogoApplicationService catalogo = new CatalogoApplicationService(store, json);
  public final ReservasApplicationService reservas = new ReservasApplicationService(store, json);
  public final PrestamosApplicationService prestamos = new PrestamosApplicationService(store, json);
  public final PagosApplicationService pagos = new PagosApplicationService(store, json);
  public final EvidenciasApplicationService evidencias = new EvidenciasApplicationService(store, json);
  public final ReputacionApplicationService reputacion = new ReputacionApplicationService(store, json);
  public final NotificacionesApplicationService notificaciones = new NotificacionesApplicationService(store, json);

  public TestWorld() {
    store.seed("roles", Map.of("id", ROL_ESTUDIANTE, "codigo", "ESTUDIANTE", "descripcion", "Estudiante"));
    store.seed("roles", Map.of("id", ROL_ADMIN, "codigo", "ADMINISTRADOR", "descripcion", "Administrador"));
    store.seed("categorias", Map.of("id", CATEGORIA, "codigo", "OTROS", "nombre", "Otros", "activa", true));
  }

  public static void login(String uid) {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(uid, null, java.util.List.of()));
  }

  public static void logout() { SecurityContextHolder.clearContext(); }

  /** Crea usuario + perfil y devuelve el id del usuario. */
  public String usuario(String uid) { return usuario(uid, ROL_ESTUDIANTE); }

  public String admin(String uid) { return usuario(uid, ROL_ADMIN); }

  private String usuario(String uid, String rol) {
    var u = store.create("usuarios", Map.of("firebase_uid", uid, "correo_institucional", uid + "@uni.edu", "rol_id", rol));
    store.create("perfiles", Map.of("usuario_id", u.get("id"), "nombre", uid, "universidad", "UNI", "campus", "Central",
        "carrera", "Sistemas", "ciclo", "5", "telefono", "999-" + uid));
    return u.get("id").toString();
  }

  public Map<String, Object> publicacion(String ownerId, BigDecimal garantia) {
    var d = new LinkedHashMap<String, Object>();
    d.put("propietario_usuario_id", ownerId);
    d.put("categoria_id", CATEGORIA);
    d.put("titulo", "Calculadora científica");
    d.put("descripcion", "Casio fx-991");
    d.put("condicion_objeto", "BUENA");
    d.put("universidad", "UNI");
    d.put("campus", "Central");
    d.put("ubicacion", "Biblioteca");
    d.put("lugar_intercambio", "Puerta 3");
    d.put("tarifa_diaria", new BigDecimal("5.00"));
    d.put("garantia_monetaria", garantia);
    d.put("moneda", "PEN");
    d.put("condiciones_uso", "Uso académico");
    d.put("condiciones_entrega", "En mano");
    d.put("condiciones_devolucion", "Limpia");
    d.put("condiciones_cancelacion", "24h antes");
    return store.create("publicaciones", d);
  }

  public void disponibilidad(Object publicacionId, String desde, String hasta) {
    store.create("disponibilidades_publicacion", Map.of("publicacion_id", publicacionId, "desde", desde, "hasta", hasta));
  }

  /** Solicitud PENDIENTE creada vía el servicio (valida disponibilidad y copia condiciones). */
  public Map<String, Object> solicitud(String borrowerUid, Object publicacionId, String desde, String hasta) {
    login(borrowerUid);
    var body = new LinkedHashMap<String, Object>();
    body.put("publicacion_id", publicacionId);
    body.put("desde", desde);
    body.put("hasta", hasta);
    return (Map<String, Object>) reservas.execute("createRequest", Map.of(), body, Map.of());
  }

  /** Préstamo en el estado indicado, directamente en la tabla (para probar el ciclo de vida). */
  public Map<String, Object> prestamo(String lenderId, String borrowerId, String estado, boolean garantiaRequerida) {
    var pub = publicacion(lenderId, garantiaRequerida ? new BigDecimal("50.00") : null);
    disponibilidad(pub.get("id"), "2026-10-01T00:00:00", "2026-12-31T00:00:00");
    var req = store.create("solicitudes", Map.ofEntries(
        Map.entry("publicacion_id", pub.get("id")), Map.entry("prestatario_usuario_id", borrowerId),
        Map.entry("prestamista_usuario_id", lenderId), Map.entry("desde", DESDE), Map.entry("hasta", HASTA),
        Map.entry("tarifa_diaria_aceptada", new BigDecimal("5.00")), Map.entry("moneda_aceptada", "PEN"),
        Map.entry("lugar_intercambio_aceptado", "Puerta 3"), Map.entry("condiciones_uso_aceptadas", "x"),
        Map.entry("condiciones_entrega_aceptadas", "x"), Map.entry("condiciones_devolucion_aceptadas", "x"),
        Map.entry("condiciones_cancelacion_aceptadas", "x"), Map.entry("condiciones_aceptadas_en", "2026-10-01T00:00:00")));
    var agenda = store.create("agendas_objeto", Map.of("publicacion_id", pub.get("id")));
    var res = new LinkedHashMap<String, Object>();
    res.put("solicitud_id", req.get("id"));
    res.put("agenda_id", agenda.get("id"));
    res.put("desde", DESDE);
    res.put("hasta", HASTA);
    res.put("tarifa_diaria_acordada", new BigDecimal("5.00"));
    res.put("garantia_monetaria_acordada", garantiaRequerida ? new BigDecimal("50.00") : null);
    res.put("moneda_acordada", "PEN");
    res.put("lugar_intercambio_acordado", "Puerta 3");
    res.put("condiciones_uso_acordadas", "x");
    res.put("condiciones_entrega_acordadas", "x");
    res.put("condiciones_devolucion_acordadas", "x");
    res.put("condiciones_cancelacion_acordadas", "x");
    var reserva = store.create("reservas", res);
    var loan = new LinkedHashMap<String, Object>();
    loan.put("reserva_id", reserva.get("id"));
    loan.put("publicacion_id", pub.get("id"));
    loan.put("prestamista_usuario_id", lenderId);
    loan.put("prestatario_usuario_id", borrowerId);
    loan.put("estado", estado);
    loan.put("entrega_programada_en", DESDE);
    loan.put("devolucion_original_en", HASTA);
    loan.put("devolucion_vigente_en", HASTA);
    loan.put("tarifa_diaria_acordada", new BigDecimal("5.00"));
    loan.put("moneda", "PEN");
    loan.put("garantia_requerida", garantiaRequerida);
    return store.create("prestamos", loan);
  }
}
