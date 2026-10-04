package com.lendup.reservas.interfaces.rest;
import com.lendup.reservas.application.ReservasApplicationService;
import com.lendup.reservas.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Reservas")
public class ReservasController {
  private final ReservasApplicationService flow;
  private final JsonMapper mapper;
  public ReservasController(ReservasApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="createRequest",description="POST /api/v1/solicitudes")
  @PostMapping("/solicitudes")
  public ResponseEntity<Object> createRequest(@Valid @RequestBody CreateRequestRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("createRequest",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="acceptRequest",description="POST /api/v1/solicitudes/{id}/aceptacion")
  @PostMapping("/solicitudes/{id}/aceptacion")
  public ResponseEntity<Object> acceptRequest(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("acceptRequest",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="rejectRequest",description="POST /api/v1/solicitudes/{id}/rechazo")
  @PostMapping("/solicitudes/{id}/rechazo")
  public ResponseEntity<Object> rejectRequest(@PathVariable("id") String id, @Valid @RequestBody RejectRequestRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("rejectRequest",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="listReservations",description="GET /api/v1/reservas")
  @GetMapping("/reservas")
  public ResponseEntity<Object> listReservations(@RequestParam(value="rol",required=false) String rol) {
    var query=new java.util.LinkedHashMap<String,String>();
    if(rol!=null)query.put("rol",rol);
    var result=flow.execute("listReservations",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="cancelReservation",description="POST /api/v1/reservas/{id}/cancelacion")
  @PostMapping("/reservas/{id}/cancelacion")
  public ResponseEntity<Object> cancelReservation(@PathVariable("id") String id, @Valid @RequestBody CancelReservationRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("cancelReservation",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="contact",description="GET /api/v1/reservas/{id}/contacto")
  @GetMapping("/reservas/{id}/contacto")
  public ResponseEntity<Object> contact(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("contact",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="listRequests",description="GET /api/v1/solicitudes")
  @GetMapping("/solicitudes")
  public ResponseEntity<Object> listRequests(@RequestParam(value="rol",required=false) String rol, @RequestParam(value="estado",required=false) String estado) {
    var query=new java.util.LinkedHashMap<String,String>();
    if(rol!=null)query.put("rol",rol);
    if(estado!=null)query.put("estado",estado);
    var result=flow.execute("listRequests",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="request",description="GET /api/v1/solicitudes/{id}")
  @GetMapping("/solicitudes/{id}")
  public ResponseEntity<Object> request(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("request",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="cancelRequest",description="POST /api/v1/solicitudes/{id}/cancelacion")
  @PostMapping("/solicitudes/{id}/cancelacion")
  public ResponseEntity<Object> cancelRequest(@PathVariable("id") String id, @Valid @RequestBody CancelRequestRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("cancelRequest",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
}
