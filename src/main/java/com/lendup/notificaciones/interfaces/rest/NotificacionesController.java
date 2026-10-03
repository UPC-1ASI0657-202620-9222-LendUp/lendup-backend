package com.lendup.notificaciones.interfaces.rest;
import com.lendup.notificaciones.application.NotificacionesApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
@Tag(name="Notificaciones")
public class NotificacionesController {
  private final NotificacionesApplicationService flow;
  public NotificacionesController(NotificacionesApplicationService flow){this.flow=flow;}
  @Operation(summary="listNotifications",description="GET /api/v1/notificaciones; ver estados y ejemplos en JSON_CONTRACTS.md")
  @GetMapping("/notificaciones")
  public ResponseEntity<Object> listNotifications() {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("listNotifications",java.util.Map.of(),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
  @Operation(summary="readNotification",description="PATCH /api/v1/notificaciones/{id}; ver estados y ejemplos en JSON_CONTRACTS.md")
  @PatchMapping("/notificaciones/{id}")
  public ResponseEntity<Object> readNotification(@PathVariable("id") String id) {
    var query=new java.util.LinkedHashMap<String,String>();
    var result=flow.execute("readNotification",java.util.Map.of("id",id),java.util.Map.of(),query);
    return ResponseEntity.status(200).body(result);
  }
}
