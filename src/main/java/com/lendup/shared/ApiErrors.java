package com.lendup.shared;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
@ControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String,Object>> known(ResponseStatusException e){
    return ResponseEntity.status(e.getStatusCode()).body(Map.of("status",e.getStatusCode().value(),
      "error","SOLICITUD_INVALIDA","message",e.getReason()==null?"Solicitud inválida":e.getReason()));
  }
  @ExceptionHandler({DuplicateKeyException.class,DataIntegrityViolationException.class})
  public ResponseEntity<Map<String,Object>> database(Exception e){
    return ResponseEntity.status(409).body(Map.of("status",409,"error","CONFLICTO_DATOS",
      "message","Conflicto de unicidad o relación entre datos"));
  }
}
