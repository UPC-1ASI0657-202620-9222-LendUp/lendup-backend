package com.lendup.prestamos.infrastructure.messaging;
import com.lendup.prestamos.application.PaymentConfirmationService;
import java.util.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
/** Reads the event envelope (event_id, event_type, occurred_at, payload). Failures are retried and then go to the DLQ. */
@Component
@ConditionalOnProperty(name="lendup.messaging.enabled",havingValue="true")
public class PaymentEventsConsumer {
  private final JsonMapper json;
  private final PaymentConfirmationService payments;
  public PaymentEventsConsumer(JsonMapper json,PaymentConfirmationService payments){this.json=json;this.payments=payments;}
  @RabbitListener(queues=PaymentEventsConfiguration.QUEUE)
  public void consume(Message message){handle(message.getMessageProperties().getReceivedRoutingKey(),message.getBody());}
  @SuppressWarnings("unchecked")
  boolean handle(String routingKey,byte[] body){
    Map<String,Object> event=json.readValue(body,new TypeReference<Map<String,Object>>(){});
    var eventId=UUID.fromString(Objects.toString(event.get("event_id"),"")).toString();
    if(!(event.get("payload") instanceof Map<?,?> payload))throw new IllegalArgumentException("Evento sin payload");
    return payments.apply(eventId,Objects.toString(event.get("event_type"),routingKey),routingKey,(Map<String,Object>)payload,Objects.toString(event.get("occurred_at"),null));
  }
}
