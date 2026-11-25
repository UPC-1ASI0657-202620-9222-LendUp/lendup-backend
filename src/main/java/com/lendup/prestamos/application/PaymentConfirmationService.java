package com.lendup.prestamos.application;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Applies the payment events published by lendup-payments-service to the loans of the monolith.
 * The inbox row (processed_events) is written in the same transaction as the loan change, so a
 * redelivered event_id is ignored.
 */
@Service
public class PaymentConfirmationService {
  public static final String PAYMENT_CONFIRMED="pagos.pago-confirmado.v1";
  public static final String GUARANTEE_CONSTITUTED="pagos.garantia-constituida.v1";
  private static final Logger log=LoggerFactory.getLogger(PaymentConfirmationService.class);
  private final NamedParameterJdbcTemplate db;
  private final Clock clock;
  @org.springframework.beans.factory.annotation.Autowired
  public PaymentConfirmationService(NamedParameterJdbcTemplate db){this(db,Clock.systemUTC());}
  PaymentConfirmationService(NamedParameterJdbcTemplate db,Clock clock){this.db=db;this.clock=clock;}

  /** Returns false when the event was already processed. */
  @Transactional
  public boolean apply(String eventId,String eventType,String routingKey,Map<String,Object> payload,String occurredAt){
    if(processed(eventId))return false;
    var at=utc(Objects.toString(payload.getOrDefault("confirmada_en",payload.getOrDefault("constituida_en",occurredAt)),null));
    switch(routingKey){
      case PAYMENT_CONFIRMED->paymentConfirmed(payload,at);
      case GUARANTEE_CONSTITUTED->markLoan(text(payload,"prestamo_id"),"garantia_constituida_en",at);
      default->log.info("Evento {} ignorado por Préstamos",routingKey);
    }
    db.update("INSERT INTO processed_events (event_id,event_type) VALUES (:id,:type)",Map.of("id",eventId,"type",eventType));
    return true;
  }
  private void paymentConfirmed(Map<String,Object> payload,LocalDateTime at){
    switch(text(payload,"tipo")){
      case "PAGO_TARIFA"->markLoan(text(payload,"prestamo_id"),"pago_tarifa_confirmado_en",at);
      case "PAGO_EXTENSION"->applyExtension(payload,at);
      default->{}
    }
  }
  private void markLoan(String loanId,String column,LocalDateTime at){
    // column is one of two constants above, never user input.
    var rows=db.update("UPDATE prestamos SET "+column+"=COALESCE("+column+",:at),actualizado_en=:now WHERE id=:id",Map.of("at",at,"now",now(),"id",loanId));
    if(rows==0)throw new IllegalStateException("Préstamo no encontrado: "+loanId);
  }
  private void applyExtension(Map<String,Object> payload,LocalDateTime at){
    var changeId=text(payload,"cambio_fecha_prestamo_id");var loanId=text(payload,"prestamo_id");
    var changes=db.queryForList("SELECT * FROM cambios_fecha_prestamo WHERE id=:id AND prestamo_id=:loan FOR UPDATE",Map.of("id",changeId,"loan",loanId));
    if(changes.isEmpty())throw new IllegalStateException("Extensión no encontrada: "+changeId);
    var change=changes.getFirst();
    if(!"EXTENSION".equals(change.get("tipo"))||!"ACEPTADA_PENDIENTE_PAGO".equals(change.get("estado"))){
      log.warn("Pago de extensión {} recibido con el cambio en estado {}; no se aplica",changeId,change.get("estado"));return;
    }
    var newDate=change.get("fecha_propuesta_en");
    var update=new LinkedHashMap<String,Object>();
    update.put("id",changeId);update.put("transaction",Objects.toString(payload.get("transaccion_id"),null));update.put("at",at);update.put("date",newDate);update.put("now",now());
    db.update("UPDATE cambios_fecha_prestamo SET estado='APLICADA',transaccion_pago_id=:transaction,pago_confirmado_en=:at,fecha_resultante_en=:date,aplicado_en=:now WHERE id=:id",update);
    db.update("UPDATE prestamos SET devolucion_vigente_en=:date,actualizado_en=:now WHERE id=:id",Map.of("date",newDate,"now",now(),"id",loanId));
  }
  private boolean processed(String eventId){
    return db.queryForObject("SELECT COUNT(*) FROM processed_events WHERE event_id=:id",Map.of("id",eventId),Integer.class)>0;
  }
  private String text(Map<String,Object> payload,String key){
    var value=Objects.toString(payload.get(key),"");
    if(value.isBlank())throw new IllegalArgumentException("Falta "+key+" en el evento");
    return value;
  }
  private LocalDateTime now(){return LocalDateTime.now(clock);}
  private LocalDateTime utc(String value){
    if(value==null||value.isBlank())return now();
    if(value.endsWith("Z")||value.matches(".*[+-][0-9]{2}:[0-9]{2}$"))return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    return LocalDateTime.parse(value);
  }
}
