package com.lendup.evidencias.interfaces.rest;
import com.lendup.evidencias.application.EvidenciasApplicationService;
import com.lendup.evidencias.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Evidencias")
public class EvidenciasController {
  private final EvidenciasApplicationService flow;
  private final JsonMapper mapper;
  public EvidenciasController(EvidenciasApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="evidence",description="POST /api/v1/prestamos/{id}/evidencias; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/evidencias")
  public ResponseEntity<Object> evidence(@PathVariable("id") String id, @Valid @RequestBody EvidenceRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("evidence",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="analyze",description="POST /api/v1/prestamos/{id}/analisis-evidencias; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/analisis-evidencias")
  public ResponseEntity<Object> analyze(@PathVariable("id") String id, @Valid @RequestBody AnalyzeRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("analyze",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="incident",description="POST /api/v1/incidencias; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/incidencias")
  public ResponseEntity<Object> incident(@Valid @RequestBody IncidentRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("incident",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="incidentById",description="GET /api/v1/incidencias/{id}; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/incidencias/{id}")
  public ResponseEntity<Object> incidentById(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("incidentById",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="adminIncidents",description="GET /api/v1/admin/incidencias; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/admin/incidencias")
  public ResponseEntity<Object> adminIncidents(@RequestParam(value="estado",required=false) String estado, @RequestParam(value="tipo",required=false) String tipo) {
    var query=new java.util.LinkedHashMap<String,String>();
    if(estado!=null)query.put("estado",estado);
    if(tipo!=null)query.put("tipo",tipo);
    var result=flow.execute("adminIncidents",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="resolve",description="POST /api/v1/admin/incidencias/{id}/resolucion; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/admin/incidencias/{id}/resolucion")
  public ResponseEntity<Object> resolve(@PathVariable("id") String id, @Valid @RequestBody ResolveRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("resolve",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
}
