package com.lendup.support;

import com.lendup.catalogo.domain.repositories.CatalogoRepository;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import com.lendup.identidad.domain.repositories.IdentidadRepository;
import com.lendup.notificaciones.domain.repositories.NotificacionesRepository;
import com.lendup.pagos.domain.repositories.PagosRepository;
import com.lendup.prestamos.domain.repositories.PrestamosRepository;
import com.lendup.reputacion.domain.repositories.ReputacionRepository;
import com.lendup.reservas.domain.repositories.ReservasRepository;
import com.lendup.shared.SchemaCatalog;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Persistencia en memoria para pruebas unitarias. Reproduce el contrato de {@code shared/Store}:
 * rechaza tablas desconocidas, filtra columnas fuera de {@link SchemaCatalog}, exige columnas
 * obligatorias, aplica los DEFAULT de schema.sql y las claves únicas, y convierte fechas ISO.
 * Registra en {@link #calls()} el orden de lockAgenda/overlaps para verificar el protocolo de bloqueo.
 */
public class InMemoryStore implements ReservasRepository, IdentidadRepository, CatalogoRepository,
    PrestamosRepository, EvidenciasRepository, PagosRepository, ReputacionRepository, NotificacionesRepository {

  private static final Map<String, Map<String, Object>> DEFAULTS = Map.ofEntries(
      Map.entry("usuarios", Map.of("estado_verificacion", "NO_VERIFICADO")),
      Map.entry("categorias", Map.of("activa", true)),
      Map.entry("publicaciones", Map.of("estado", "ACTIVA")),
      Map.entry("bloqueos_reserva_lectura", Map.of("estado", "VIGENTE")),
      Map.entry("solicitudes", Map.of("estado", "PENDIENTE")),
      Map.entry("agendas_objeto", Map.of("version", 0)),
      Map.entry("reservas", Map.of("estado", "CONFIRMADA")),
      Map.entry("prestamos", Map.of("estado", "RESERVADO", "incidencias_pendientes", 0)),
      Map.entry("cambios_fecha_prestamo", Map.of("estado", "PENDIENTE")),
      Map.entry("incidencias", Map.of("estado", "PENDIENTE")),
      Map.entry("evidencias", Map.of("estado_integracion", "PENDIENTE")),
      Map.entry("analisis_evidencias", Map.of("estado", "PENDIENTE")),
      Map.entry("garantias", Map.of("estado", "PENDIENTE")),
      Map.entry("transacciones_economicas", Map.of("estado", "PENDIENTE")),
      Map.entry("notificaciones", Map.of("enviar_correo", false, "estado_correo", "NO_APLICA", "intentos_correo", 0)),
      Map.entry("webhook_events", Map.of("estado", "PENDIENTE_VALIDACION")));

  private static final Map<String, List<List<String>>> UNIQUE = Map.of(
      "usuarios", List.of(List.of("firebase_uid"), List.of("correo_institucional")),
      "perfiles", List.of(List.of("usuario_id")),
      "agendas_objeto", List.of(List.of("publicacion_id")),
      "reservas", List.of(List.of("solicitud_id")),
      "prestamos", List.of(List.of("reserva_id")),
      "garantias", List.of(List.of("prestamo_id")),
      "transacciones_economicas", List.of(List.of("clave_idempotencia")),
      "calificaciones", List.of(List.of("prestamo_id", "evaluador_usuario_id")),
      "notificaciones", List.of(List.of("destinatario_usuario_id", "clave_notificacion")),
      "webhook_events", List.of(List.of("proveedor", "evento_proveedor_id")));

  private final SchemaCatalog schema = new SchemaCatalog();
  private final Map<String, Map<String, Map<String, Object>>> tables = new LinkedHashMap<>();
  private final List<String> calls = new ArrayList<>();

  public List<String> calls() { return calls; }

  /** Inserta una fila con id fijo, sin validar (equivale a data.sql). */
  public void seed(String table, Map<String, Object> row) {
    tables.computeIfAbsent(table, t -> new LinkedHashMap<>()).put((String) row.get("id"), new LinkedHashMap<>(row));
  }

  public int count(String table) { return rows(table).size(); }

  private Map<String, Map<String, Object>> rows(String table) {
    check(table);
    return tables.computeIfAbsent(table, t -> new LinkedHashMap<>());
  }

  private void check(String t) {
    if (schema.columns(t).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tabla desconocida");
  }

  static Object toSqlValue(String key, Object val) {
    if (!(val instanceof String str)) return val;
    if (key.equals("desde") || key.equals("hasta") || key.endsWith("_en") || key.equals("programada_para")) {
      try {
        return str.endsWith("Z") || str.matches(".*[+-][0-9]{2}:[0-9]{2}$")
            ? OffsetDateTime.parse(str).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
            : LocalDateTime.parse(str);
      } catch (DateTimeParseException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha ISO inválida: " + key);
      }
    }
    return val;
  }

  private static LocalDateTime time(Object o) {
    return o instanceof LocalDateTime t ? t : LocalDateTime.parse(o.toString());
  }

  @Override
  public Map<String, Object> get(String t, String id) {
    var row = rows(t).get(id);
    if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado");
    return new LinkedHashMap<>(row);
  }

  @Override
  public List<Map<String, Object>> list(String t, String field, Object value) {
    var all = rows(t).values().stream();
    if (field != null && value != null && schema.columns(t).contains(field)) {
      all = all.filter(r -> Objects.toString(r.get(field), "").equals(value.toString()));
    }
    return all.limit(200).<Map<String, Object>>map(LinkedHashMap::new).toList();
  }

  @Override
  public Map<String, Object> create(String t, Map<String, Object> input) {
    var rows = rows(t);
    var p = new LinkedHashMap<String, Object>();
    p.put("id", UUID.randomUUID().toString());
    input.forEach((key, val) -> {
      if (schema.columns(t).contains(key) && !key.equals("id") && val != null) p.put(key, toSqlValue(key, val));
    });
    var missing = new ArrayList<>(schema.mandatory(t));
    missing.removeAll(p.keySet());
    if (!missing.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan campos: " + missing);
    DEFAULTS.getOrDefault(t, Map.of()).forEach(p::putIfAbsent);
    for (var key : UNIQUE.getOrDefault(t, List.of())) {
      if (key.stream().anyMatch(c -> p.get(c) == null)) continue;
      boolean dup = rows.values().stream().anyMatch(r -> key.stream().allMatch(c -> Objects.equals(r.get(c), p.get(c))));
      if (dup) throw new DuplicateKeyException("Duplicate entry en " + t + key);
    }
    rows.put((String) p.get("id"), p);
    return new LinkedHashMap<>(p);
  }

  @Override
  public Map<String, Object> update(String t, String id, Map<String, Object> input) { return modify(t, id, input, false); }

  @Override
  public Map<String, Object> updateTrusted(String t, String id, Map<String, Object> input) { return modify(t, id, input, true); }

  private Map<String, Object> modify(String t, String id, Map<String, Object> input, boolean allowReferences) {
    var row = rows(t).get(id);
    if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado");
    input.forEach((key, val) -> {
      if (schema.columns(t).contains(key) && !key.equals("id") && (allowReferences || !key.endsWith("_id"))) {
        row.put(key, toSqlValue(key, val));
      }
    });
    return new LinkedHashMap<>(row);
  }

  @Override
  public void lockAgenda(String agendaId) { calls.add("lockAgenda"); }

  @Override
  public boolean overlaps(String agendaId, Object from, Object to) {
    calls.add("overlaps");
    return rows("reservas").values().stream().anyMatch(r -> agendaId.equals(r.get("agenda_id"))
        && "CONFIRMADA".equals(r.get("estado"))
        && time(r.get("desde")).isBefore(time(to)) && time(r.get("hasta")).isAfter(time(from)));
  }

  @Override
  public boolean offered(String publicationId, Object from, Object to) {
    var f = time(toSqlValue("desde", from));
    var h = time(toSqlValue("hasta", to));
    return rows("disponibilidades_publicacion").values().stream().anyMatch(d -> publicationId.equals(d.get("publicacion_id"))
        && !time(d.get("desde")).isAfter(f) && !time(d.get("hasta")).isBefore(h));
  }

  @Override
  public List<Map<String, Object>> searchPublications(Map<String, String> filters) {
    var stream = rows("publicaciones").values().stream().filter(p -> "ACTIVA".equals(p.get("estado")));
    if (filters.containsKey("nombre") && !filters.get("nombre").isBlank())
      stream = stream.filter(p -> p.get("titulo").toString().toLowerCase().contains(filters.get("nombre").toLowerCase()));
    if (filters.containsKey("categoria") && !filters.get("categoria").isBlank())
      stream = stream.filter(p -> filters.get("categoria").equals(p.get("categoria_id")));
    if (filters.containsKey("campus") && !filters.get("campus").isBlank())
      stream = stream.filter(p -> filters.get("campus").equals(p.get("campus")));
    if (filters.containsKey("desde") && filters.containsKey("hasta")) {
      var d = time(toSqlValue("desde", filters.get("desde")));
      var h = time(toSqlValue("hasta", filters.get("hasta")));
      stream = stream.filter(p -> offered(p.get("id").toString(), d, h) && !booked(p.get("id").toString(), d, h));
    }
    return stream.<Map<String, Object>>map(LinkedHashMap::new).toList();
  }

  private boolean booked(String publicationId, LocalDateTime d, LocalDateTime h) {
    return rows("agendas_objeto").values().stream().filter(a -> publicationId.equals(a.get("publicacion_id")))
        .anyMatch(a -> rows("reservas").values().stream().anyMatch(r -> a.get("id").equals(r.get("agenda_id"))
            && "CONFIRMADA".equals(r.get("estado")) && time(r.get("desde")).isBefore(h) && time(r.get("hasta")).isAfter(d)));
  }
}
