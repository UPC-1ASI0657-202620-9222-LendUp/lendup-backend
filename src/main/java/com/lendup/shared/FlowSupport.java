package com.lendup.shared;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
public abstract class FlowSupport {
  protected final PersistencePort store;
  protected final JsonMapper json;
  protected FlowSupport(PersistencePort store,JsonMapper json){this.store=store;this.json=json;}
  protected String uid(){return (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();}
  protected String id(Map<String,String> vars){return vars.get("id");}
  protected String string(Map<String,Object> b,String key){return Objects.toString(b.get(key),"");}
  protected Map<String,Object> editable(Map<String,Object> b){return new LinkedHashMap<>(b);}
  protected Map<String,Object> user(){
    var rows=store.list("usuarios","firebase_uid",uid());
    if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Registra tu perfil en LendUp");
    return rows.getFirst();
  }
  protected String userId(){return user().get("id").toString();}
  protected Map<String,Object> participantLoan(String id){
    var loan=store.get("prestamos",id);var me=userId();
    if(!me.equals(loan.get("prestatario_usuario_id"))&&!me.equals(loan.get("prestamista_usuario_id")))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No participas en este préstamo");
    return loan;
  }
  protected void owns(Map<String,Object> row,String column){
    if(!userId().equals(Objects.toString(row.get(column),"")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Operación no permitida");
  }
  protected void admin(){
    var role=store.get("roles",user().get("rol_id").toString());
    if(!"ADMINISTRADOR".equals(role.get("codigo")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Se requiere rol administrador");
  }
  protected Map<String,Object> change(String t,String id,String key,Object value){return store.update(t,id,Map.of(key,value));}
  protected Map<String,Object> create(String t,Map<String,Object>b,Map<String,Object> extras){
    var data=editable(b);data.putAll(extras);return store.create(t,data);
  }
  protected java.time.LocalDateTime now(){return java.time.LocalDateTime.now(java.time.Clock.systemUTC());}
  private java.time.LocalDateTime parseDateTime(String value){
    if(value.endsWith("Z")||value.matches(".*[+-][0-9]{2}:[0-9]{2}$"))
      return java.time.OffsetDateTime.parse(value).withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
    return java.time.LocalDateTime.parse(value);
  }
  protected void validPeriod(Map<String,Object> body){
    try{
      var from=parseDateTime(string(body,"desde"));
      var to=parseDateTime(string(body,"hasta"));
      if(!from.isBefore(to))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"desde debe ser anterior a hasta");
    }catch(java.time.format.DateTimeParseException ex){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fechas inválidas: use yyyy-MM-ddTHH:mm:ss");
    }
  }
  protected Map<String,Object> guaranteeForLoan(Map<String,Object> loan){
    var reservation=store.get("reservas",loan.get("reserva_id").toString());
    var amount=reservation.get("garantia_monetaria_acordada");
    if(amount==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"El préstamo no requiere garantía");
    return Map.of("prestamo_id",loan.get("id"),"monto_acordado",amount,"moneda",loan.get("moneda"));
  }
  protected String newIdempotency(Map<String,Object>b){
    var key=string(b,"clave_idempotencia");
    if(key.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requiere clave_idempotencia");
    return key;
  }
  protected void notifyUser(Object recipient,String origin,String originId,String title,String message){
    store.create("notificaciones",Map.of("destinatario_usuario_id",recipient,"clave_notificacion",origin+":"+originId+":"+recipient,
      "tipo","EVENTO","origen_tipo",origin,"origen_id",originId,
      "evento_origen_id",UUID.randomUUID().toString(),"titulo",title,"mensaje",message,
      "estado","DISPONIBLE","disponible_en",now()));
  }
}
