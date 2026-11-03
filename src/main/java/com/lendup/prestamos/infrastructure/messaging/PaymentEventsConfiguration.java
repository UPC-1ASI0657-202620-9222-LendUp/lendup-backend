package com.lendup.prestamos.infrastructure.messaging;
import com.lendup.prestamos.application.PaymentConfirmationService;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/** Same event standard as the microservices: topic exchange, one queue per consumer and its DLQ. */
@Configuration
@ConditionalOnProperty(name="lendup.messaging.enabled",havingValue="true")
public class PaymentEventsConfiguration {
  public static final String EXCHANGE="lendup.domain.events";
  public static final String DLX="lendup.domain.events.dlx";
  public static final String QUEUE="prestamos.events.v1";
  public static final String DLQ="prestamos.events.v1.dlq";
  @Bean TopicExchange domainExchange(){return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();}
  @Bean DirectExchange deadLetterExchange(){return ExchangeBuilder.directExchange(DLX).durable(true).build();}
  @Bean Queue loansEventsQueue(){return QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();}
  @Bean Queue loansEventsDlq(){return QueueBuilder.durable(DLQ).build();}
  @Bean Binding loansDlqBinding(Queue loansEventsDlq,DirectExchange deadLetterExchange){return BindingBuilder.bind(loansEventsDlq).to(deadLetterExchange).with(DLQ);}
  @Bean Binding paymentConfirmedBinding(Queue loansEventsQueue,TopicExchange domainExchange){return BindingBuilder.bind(loansEventsQueue).to(domainExchange).with(PaymentConfirmationService.PAYMENT_CONFIRMED);}
  @Bean Binding guaranteeConstitutedBinding(Queue loansEventsQueue,TopicExchange domainExchange){return BindingBuilder.bind(loansEventsQueue).to(domainExchange).with(PaymentConfirmationService.GUARANTEE_CONSTITUTED);}
}
