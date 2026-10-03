package com.lendup.pagos.interfaces.rest;
import com.lendup.pagos.application.PagosApplicationService;
import com.lendup.pagos.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Pagos")
public class PagosController {
  private final PagosApplicationService flow;
  private final JsonMapper mapper;
  public PagosController(PagosApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="paymentMethods",description="GET /api/v1/medios-pago; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/medios-pago")
  public ResponseEntity<Object> paymentMethods() {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("paymentMethods",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="payment",description="POST /api/v1/pagos; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/pagos")
  public ResponseEntity<Object> payment(@Valid @RequestBody PaymentRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("payment",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="guarantee",description="POST /api/v1/garantias; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/garantias")
  public ResponseEntity<Object> guarantee(@Valid @RequestBody GuaranteeRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("guarantee",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="transactions",description="GET /api/v1/prestamos/{id}/transacciones; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/prestamos/{id}/transacciones")
  public ResponseEntity<Object> transactions(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("transactions",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="webhook",description="POST /api/v1/webhooks/mercado-pago; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/webhooks/mercado-pago")
  public ResponseEntity<Object> webhook(@Valid @RequestBody WebhookRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("webhook",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
}
