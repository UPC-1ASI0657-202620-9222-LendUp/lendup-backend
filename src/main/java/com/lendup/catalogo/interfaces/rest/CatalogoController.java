package com.lendup.catalogo.interfaces.rest;
import com.lendup.catalogo.application.CatalogoApplicationService;
import com.lendup.catalogo.interfaces.rest.resources.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Catalogo")
public class CatalogoController {
  private final CatalogoApplicationService flow;
  private final JsonMapper mapper;
  public CatalogoController(CatalogoApplicationService flow,JsonMapper mapper){this.flow=flow;this.mapper=mapper;}
  private Map<String,Object> bodyMap(Object body){
    var converted=mapper.convertValue(body,new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    converted.values().removeIf(java.util.Objects::isNull);
    return converted;
  }
  @Operation(summary="createPublication",description="POST /api/v1/objetos")
  @PostMapping("/objetos")
  public ResponseEntity<Object> createPublication(@Valid @RequestBody CreatePublicationRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("createPublication",java.util.Map.of(),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="updatePublication",description="PUT /api/v1/objetos/{id}")
  @PutMapping("/objetos/{id}")
  public ResponseEntity<Object> updatePublication(@PathVariable("id") String id, @Valid @RequestBody UpdatePublicationRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("updatePublication",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="publicationState",description="PATCH /api/v1/objetos/{id}/estado")
  @PatchMapping("/objetos/{id}/estado")
  public ResponseEntity<Object> publicationState(@PathVariable("id") String id, @Valid @RequestBody PublicationStateRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("publicationState",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="availability",description="PUT /api/v1/objetos/{id}/disponibilidad")
  @PutMapping("/objetos/{id}/disponibilidad")
  public ResponseEntity<Object> availability(@PathVariable("id") String id, @Valid @RequestBody AvailabilityRequest body) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("availability",java.util.Map.of("id",id),bodyMap(body),query);
    return ResponseEntity.status(201).body(result);
  }
  @Operation(summary="listPublications",description="GET /api/v1/objetos")
  @GetMapping("/objetos")
  public ResponseEntity<Object> listPublications(@RequestParam(value="nombre",required=false) String nombre, @RequestParam(value="categoria",required=false) String categoria, @RequestParam(value="campus",required=false) String campus, @RequestParam(value="desde",required=false) String desde, @RequestParam(value="hasta",required=false) String hasta) {
    var query=new java.util.LinkedHashMap<String,String>();
    if(nombre!=null)query.put("nombre",nombre);
    if(categoria!=null)query.put("categoria",categoria);
    if(campus!=null)query.put("campus",campus);
    if(desde!=null)query.put("desde",desde);
    if(hasta!=null)query.put("hasta",hasta);
    var result=flow.execute("listPublications",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="publication",description="GET /api/v1/objetos/{id}")
  @GetMapping("/objetos/{id}")
  public ResponseEntity<Object> publication(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("publication",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="termsDocument",description="GET /api/v1/terminos")
  @GetMapping("/terminos")
  public ResponseEntity<Object> termsDocument() {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("termsDocument",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
}
