package com.lendup.prestamos.interfaces.rest;
import com.lendup.prestamos.application.PrestamosApplicationService;
import com.lendup.prestamos.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Prestamos")
public class PrestamosController {
  private final PrestamosApplicationService flow;
  private final JsonMapper mapper;
  public PrestamosController(PrestamosApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="delivery",description="POST /api/v1/prestamos/{id}/entrega; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/entrega")
  public ResponseEntity<Object> delivery(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("delivery",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="receipt",description="POST /api/v1/prestamos/{id}/recepcion; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/recepcion")
  public ResponseEntity<Object> receipt(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("receipt",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="extension",description="POST /api/v1/prestamos/{id}/extensiones; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/extensiones")
  public ResponseEntity<Object> extension(@PathVariable("id") String id, @Valid @RequestBody ExtensionRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("extension",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="extensionResponse",description="POST /api/v1/prestamos/{id}/extensiones/{subid}/respuesta; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/extensiones/{subid}/respuesta")
  public ResponseEntity<Object> extensionResponse(@PathVariable("id") String id, @PathVariable("subid") String subid, @Valid @RequestBody ExtensionResponseRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("extensionResponse",java.util.Map.of("id",id,"subid",subid),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="reschedule",description="POST /api/v1/prestamos/{id}/reprogramaciones; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/reprogramaciones")
  public ResponseEntity<Object> reschedule(@PathVariable("id") String id, @Valid @RequestBody RescheduleRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("reschedule",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="returnLoan",description="POST /api/v1/prestamos/{id}/devolucion; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/devolucion")
  public ResponseEntity<Object> returnLoan(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("returnLoan",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="confirmReturn",description="POST /api/v1/prestamos/{id}/confirmacion-devolucion; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/confirmacion-devolucion")
  public ResponseEntity<Object> confirmReturn(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("confirmReturn",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="listLoans",description="GET /api/v1/prestamos; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/prestamos")
  public ResponseEntity<Object> listLoans(@RequestParam(value="estado",required=false) String estado) {
    var query=new java.util.LinkedHashMap<String,String>();
    if(estado!=null)query.put("estado",estado);
    var result=flow.execute("listLoans",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="calendar",description="GET /api/v1/calendario; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/calendario")
  public ResponseEntity<Object> calendar() {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("calendar",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="loan",description="GET /api/v1/prestamos/{id}; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/prestamos/{id}")
  public ResponseEntity<Object> loan(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("loan",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="rescheduleResponse",description="POST /api/v1/prestamos/{id}/reprogramaciones/{subid}/respuesta; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/reprogramaciones/{subid}/respuesta")
  public ResponseEntity<Object> rescheduleResponse(@PathVariable("id") String id, @PathVariable("subid") String subid, @Valid @RequestBody RescheduleResponseRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("rescheduleResponse",java.util.Map.of("id",id,"subid",subid),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="extensionQuote",description="POST /api/v1/prestamos/{id}/extensiones/cotizacion; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/prestamos/{id}/extensiones/cotizacion")
  public ResponseEntity<Object> extensionQuote(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("extensionQuote",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="paymentQuote",description="GET /api/v1/prestamos/{id}/cotizacion-pago; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/prestamos/{id}/cotizacion-pago")
  public ResponseEntity<Object> paymentQuote(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("paymentQuote",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
}
