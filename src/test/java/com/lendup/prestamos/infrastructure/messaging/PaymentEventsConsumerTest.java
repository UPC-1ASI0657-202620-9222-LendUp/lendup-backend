package com.lendup.prestamos.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.lendup.prestamos.application.PaymentConfirmationService;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PaymentEventsConsumerTest {
  final PaymentConfirmationService payments=mock(PaymentConfirmationService.class);
  final PaymentEventsConsumer consumer=new PaymentEventsConsumer(JsonMapper.builder().build(),payments);
  // Same message as contracts/events/v1/examples.json of lendup-payments-service.
  final String confirmed="""
    {"event_id":"a1111111-1111-4111-8111-111111111111","event_type":"PagoConfirmado","event_version":1,"aggregate_version":1762009200000,
     "aggregate_id":"b2222222-2222-4222-8222-222222222222","occurred_at":"2026-11-01T15:00:00Z","correlation_id":"req-123",
     "payload":{"prestamo_id":"c3333333-3333-4333-8333-333333333333","transaccion_id":"b2222222-2222-4222-8222-222222222222",
       "tipo":"PAGO_TARIFA","estado":"CONFIRMADA","monto":60.00,"moneda":"PEN","confirmada_en":"2026-11-01T15:00:00Z"}}""";

  @Test void envelopeIsPassedToTheLoanService(){
    when(payments.apply(anyString(),anyString(),anyString(),anyMap(),any())).thenReturn(true);
    assertTrue(consumer.handle("pagos.pago-confirmado.v1",confirmed.getBytes(StandardCharsets.UTF_8)));
    verify(payments).apply(eq("a1111111-1111-4111-8111-111111111111"),eq("PagoConfirmado"),eq("pagos.pago-confirmado.v1"),
      argThat(p->"PAGO_TARIFA".equals(p.get("tipo"))&&"c3333333-3333-4333-8333-333333333333".equals(p.get("prestamo_id"))),eq("2026-11-01T15:00:00Z"));
  }
  @Test void messagesWithoutValidIdOrPayloadAreRejected(){
    assertThrows(IllegalArgumentException.class,()->consumer.handle("pagos.pago-confirmado.v1","{\"event_id\":\"x\",\"payload\":{}}".getBytes(StandardCharsets.UTF_8)));
    assertThrows(IllegalArgumentException.class,()->consumer.handle("pagos.pago-confirmado.v1","{\"event_id\":\"a1111111-1111-4111-8111-111111111111\"}".getBytes(StandardCharsets.UTF_8)));
    verifyNoInteractions(payments);
  }
}
