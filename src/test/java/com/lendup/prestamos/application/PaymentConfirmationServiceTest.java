package com.lendup.prestamos.application;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PaymentConfirmationServiceTest {
  NamedParameterJdbcTemplate db;
  PaymentConfirmationService service;
  @BeforeEach void setup(){
    var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    db=new NamedParameterJdbcTemplate(ds);var jdbc=db.getJdbcTemplate();
    jdbc.execute("CREATE TABLE prestamos(id VARCHAR(36) PRIMARY KEY,estado VARCHAR(32),devolucion_vigente_en TIMESTAMP,pago_tarifa_confirmado_en TIMESTAMP,garantia_constituida_en TIMESTAMP,actualizado_en TIMESTAMP)");
    jdbc.execute("CREATE TABLE cambios_fecha_prestamo(id VARCHAR(36) PRIMARY KEY,prestamo_id VARCHAR(36),tipo VARCHAR(20),estado VARCHAR(32),fecha_propuesta_en TIMESTAMP,transaccion_pago_id VARCHAR(36),pago_confirmado_en TIMESTAMP,fecha_resultante_en TIMESTAMP,aplicado_en TIMESTAMP)");
    jdbc.execute("CREATE TABLE processed_events(event_id VARCHAR(36) PRIMARY KEY,event_type VARCHAR(100),processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    jdbc.update("INSERT INTO prestamos(id,estado,devolucion_vigente_en) VALUES ('loan','RESERVADO','2026-11-22 09:00:00')");
    jdbc.update("INSERT INTO cambios_fecha_prestamo(id,prestamo_id,tipo,estado,fecha_propuesta_en) VALUES ('change','loan','EXTENSION','ACEPTADA_PENDIENTE_PAGO','2026-11-24 09:00:00')");
    service=new PaymentConfirmationService(db,Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"),ZoneOffset.UTC));
  }
  Map<String,Object> loan(){return db.queryForMap("SELECT * FROM prestamos WHERE id='loan'",Map.of());}
  int processed(){return db.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM processed_events",Integer.class);}
  Map<String,Object> payment(String type){
    var payload=new HashMap<String,Object>(Map.of("prestamo_id","loan","transaccion_id","tx","tipo",type,"estado","CONFIRMADA","confirmada_en","2026-11-01T15:00:00Z"));
    if(type.equals("PAGO_EXTENSION"))payload.put("cambio_fecha_prestamo_id","change");
    return payload;
  }

  @Test void confirmedRentEnablesDeliveryOnceAndDuplicatesAreIgnored(){
    assertTrue(service.apply("e1","PagoConfirmado",PaymentConfirmationService.PAYMENT_CONFIRMED,payment("PAGO_TARIFA"),"2026-11-01T15:00:05Z"));
    assertEquals(LocalDateTime.parse("2026-11-01T15:00:00"),((java.sql.Timestamp)loan().get("pago_tarifa_confirmado_en")).toLocalDateTime());
    assertFalse(service.apply("e1","PagoConfirmado",PaymentConfirmationService.PAYMENT_CONFIRMED,payment("PAGO_TARIFA"),null));
    assertEquals(1,processed());
  }
  @Test void constitutedGuaranteeIsRecordedOnTheLoan(){
    service.apply("e2","GarantiaConstituida",PaymentConfirmationService.GUARANTEE_CONSTITUTED,Map.of("prestamo_id","loan","garantia_id","g","constituida_en","2026-11-01T15:01:40Z"),null);
    assertEquals(LocalDateTime.parse("2026-11-01T15:01:40"),((java.sql.Timestamp)loan().get("garantia_constituida_en")).toLocalDateTime());
    assertNull(loan().get("pago_tarifa_confirmado_en"));
  }
  @Test void paidExtensionIsAppliedAndMovesTheReturnDate(){
    service.apply("e3","PagoConfirmado",PaymentConfirmationService.PAYMENT_CONFIRMED,payment("PAGO_EXTENSION"),null);
    var change=db.queryForMap("SELECT * FROM cambios_fecha_prestamo WHERE id='change'",Map.of());
    assertEquals("APLICADA",change.get("estado"));assertEquals("tx",change.get("transaccion_pago_id"));
    assertEquals(LocalDateTime.parse("2026-11-24T09:00:00"),((java.sql.Timestamp)loan().get("devolucion_vigente_en")).toLocalDateTime());
  }
  @Test void extensionNotAwaitingPaymentIsLeftUntouched(){
    db.getJdbcTemplate().update("UPDATE cambios_fecha_prestamo SET estado='RECHAZADA'");
    service.apply("e4","PagoConfirmado",PaymentConfirmationService.PAYMENT_CONFIRMED,payment("PAGO_EXTENSION"),null);
    assertEquals("RECHAZADA",db.getJdbcTemplate().queryForObject("SELECT estado FROM cambios_fecha_prestamo",String.class));
    assertEquals(LocalDateTime.parse("2026-11-22T09:00:00"),((java.sql.Timestamp)loan().get("devolucion_vigente_en")).toLocalDateTime());
    assertEquals(1,processed());
  }
  @Test void unknownLoanFailsWithoutMarkingTheEventAsProcessed(){
    var payload=payment("PAGO_TARIFA");payload.put("prestamo_id","missing");
    assertThrows(IllegalStateException.class,()->service.apply("e5","PagoConfirmado",PaymentConfirmationService.PAYMENT_CONFIRMED,payload,null));
    assertEquals(0,processed());
  }
  @Test void otherPaymentTypesAreAcknowledgedWithoutChanges(){
    assertTrue(service.apply("e6","PagoConfirmado",PaymentConfirmationService.PAYMENT_CONFIRMED,payment("CONSTITUCION_GARANTIA"),null));
    assertNull(loan().get("pago_tarifa_confirmado_en"));
  }
}
