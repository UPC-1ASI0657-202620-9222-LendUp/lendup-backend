package com.lendup.reservas.application;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
class ReservasApplicationServiceTest {
  @Test void mapsTheAcceptedRequestSnapshotToTheReservationSchema(){
    var request=new LinkedHashMap<String,Object>();
    request.put("tarifa_diaria_aceptada",new BigDecimal("16.00"));
    request.put("garantia_monetaria_aceptada",new BigDecimal("40.00"));
    request.put("moneda_aceptada","PEN");
    request.put("lugar_intercambio_aceptado","Campus principal");
    request.put("condiciones_uso_aceptadas","Uso cuidadoso");
    request.put("condiciones_entrega_aceptadas","Entrega acordada");
    request.put("condiciones_devolucion_aceptadas","Devolver limpio");
    request.put("condiciones_cancelacion_aceptadas","Avisar con anticipación");

    var snapshot=ReservasApplicationService.reservationSnapshot(request);

    assertEquals(new BigDecimal("16.00"),snapshot.get("tarifa_diaria_acordada"));
    assertEquals(new BigDecimal("40.00"),snapshot.get("garantia_monetaria_acordada"));
    assertEquals("PEN",snapshot.get("moneda_acordada"));
    assertEquals("Campus principal",snapshot.get("lugar_intercambio_acordado"));
    assertEquals("Uso cuidadoso",snapshot.get("condiciones_uso_acordadas"));
    assertEquals("Entrega acordada",snapshot.get("condiciones_entrega_acordadas"));
    assertEquals("Devolver limpio",snapshot.get("condiciones_devolucion_acordadas"));
    assertEquals("Avisar con anticipación",snapshot.get("condiciones_cancelacion_acordadas"));
    assertFalse(snapshot.containsKey("lugar_intercambio_acordada"));
    assertFalse(snapshot.containsKey("condiciones_uso_acordada"));
  }

  @Test void keepsTheOptionalGuaranteeAbsent(){
    var snapshot=ReservasApplicationService.reservationSnapshot(Map.of(
      "tarifa_diaria_aceptada",new BigDecimal("16.00"),
      "moneda_aceptada","PEN"
    ));
    assertFalse(snapshot.containsKey("garantia_monetaria_acordada"));
  }
}
