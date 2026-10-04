package com.lendup.reputacion.interfaces.rest;
import com.lendup.reputacion.application.ReputacionApplicationService;
import com.lendup.reputacion.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Reputacion")
public class ReputacionController {
  private final ReputacionApplicationService flow;
  private final JsonMapper mapper;
  public ReputacionController(ReputacionApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="rating",description="POST /api/v1/prestamos/{id}/calificaciones")
  @PostMapping("/prestamos/{id}/calificaciones")
  public ResponseEntity<Object> rating(@PathVariable("id") String id, @Valid @RequestBody RatingRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("rating",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="reputation",description="GET /api/v1/estudiantes/{id}/reputacion")
  @GetMapping("/estudiantes/{id}/reputacion")
  public ResponseEntity<Object> reputation(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("reputation",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
}
