package com.lendup.identidad.interfaces.rest;
import com.lendup.identidad.application.IdentidadApplicationService;
import com.lendup.identidad.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Identidad")
public class IdentidadController {
  private final IdentidadApplicationService flow;
  private final JsonMapper mapper;
  public IdentidadController(IdentidadApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="createUser",description="POST /api/v1/estudiantes; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/estudiantes")
  public ResponseEntity<Object> createUser(@Valid @RequestBody CreateUserRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("createUser",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="me",description="GET /api/v1/estudiantes/me; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/estudiantes/me")
  public ResponseEntity<Object> me() {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("me",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="updateMe",description="PUT /api/v1/estudiantes/me; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PutMapping("/estudiantes/me")
  public ResponseEntity<Object> updateMe(@Valid @RequestBody UpdateMeRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("updateMe",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="verify",description="POST /api/v1/estudiantes/me/verificacion; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/estudiantes/me/verificacion")
  public ResponseEntity<Object> verify(@Valid @RequestBody VerifyRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("verify",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="terms",description="POST /api/v1/estudiantes/me/aceptacion-terminos; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PostMapping("/estudiantes/me/aceptacion-terminos")
  public ResponseEntity<Object> terms(@Valid @RequestBody TermsRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("terms",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="userById",description="GET /api/v1/estudiantes/{id}; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/estudiantes/{id}")
  public ResponseEntity<Object> userById(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("userById",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
}
