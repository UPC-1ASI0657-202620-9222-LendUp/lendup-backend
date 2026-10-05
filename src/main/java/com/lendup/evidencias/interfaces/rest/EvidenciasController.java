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
  private final com.lendup.shared.IncidentsService incidents;
  public EvidenciasController(EvidenciasApplicationService flow,JsonMapper mapper,com.lendup.shared.IncidentsService incidents){this.flow=flow;this.mapper=mapper;this.incidents=incidents;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="evidence",description="POST /api/v1/prestamos/{id}/evidencias")
  @PostMapping("/prestamos/{id}/evidencias")
  public ResponseEntity<Object> evidence(@PathVariable("id") String id, @Valid @RequestBody EvidenceRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("evidence",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="analyze",description="POST /api/v1/prestamos/{id}/analisis-evidencias")
  @PostMapping("/prestamos/{id}/analisis-evidencias")
  public ResponseEntity<Object> analyze(@PathVariable("id") String id, @Valid @RequestBody AnalyzeRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("analyze",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="incident",description="POST /api/v1/incidencias")
  @PostMapping(value="/incidencias",consumes="application/json")
  public ResponseEntity<Object> incident(@Valid @RequestBody IncidentRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=incidents.report(bodyMap(body),java.util.List.of());
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="incidentById",description="GET /api/v1/incidencias/{id}")
  @GetMapping("/incidencias/{id}")
  public ResponseEntity<Object> incidentById(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=incidents.detail(id);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="adminIncidents",description="GET /api/v1/admin/incidencias")
  @GetMapping("/admin/incidencias")
  public ResponseEntity<Object> adminIncidents(@RequestParam(value="estado",required=false) String estado, @RequestParam(value="tipo",required=false) String tipo) {
    var query=new java.util.LinkedHashMap<String,String>();
    if(estado!=null)query.put("estado",estado);
    if(tipo!=null)query.put("tipo",tipo);
    var result=incidents.list(true,query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="resolve",description="POST /api/v1/admin/incidencias/{id}/resolucion")
  @PostMapping("/admin/incidencias/{id}/resolucion")
  public ResponseEntity<Object> resolve(@PathVariable("id") String id, @Valid @RequestBody ResolveRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=incidents.resolve(id,bodyMap(body));
    return ResponseEntity.status(200).body(result);
  }
  @GetMapping("/incidencias")
  public Object listIncidents() {return incidents.list(false,Map.of());}
  @PostMapping(value="/incidencias",consumes="multipart/form-data")
  public ResponseEntity<Object> reportWithPhotos(@RequestPart("reporte") @Valid IncidentRequest body,
      @RequestPart(value="fotos",required=false) java.util.List<org.springframework.web.multipart.MultipartFile> photos) {
    return ResponseEntity.status(201).body(incidents.report(bodyMap(body),photos==null?java.util.List.of():photos));
  }
  public record ContentRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=10000) String contenido) {}
  @PostMapping("/incidencias/{id}/descargo")
  public Object statement(@PathVariable String id,@Valid @RequestBody ContentRequest body) {return incidents.statement(id,bodyMap(body));}
  @PostMapping("/admin/incidencias/{id}/revision")
  public Object review(@PathVariable String id) {return incidents.review(id);}
  @PostMapping("/admin/incidencias/{id}/observaciones")
  public Object note(@PathVariable String id,@Valid @RequestBody ContentRequest body) {return incidents.note(id,bodyMap(body));}
}
