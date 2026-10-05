package com.lendup.pagos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.lendup.pagos.domain.repositories.PagosRepository;
import tools.jackson.databind.json.JsonMapper;

class PagosApplicationServiceTest {
  final PagosRepository store=mock(PagosRepository.class);
  final NamedParameterJdbcTemplate db=mock(NamedParameterJdbcTemplate.class);
  final PagosApplicationService service=new PagosApplicationService(store,JsonMapper.builder().build(),db);
  Map<String,Object> loan;

  @BeforeEach void setup(){
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("firebase",null,List.of()));
    when(store.list("usuarios","firebase_uid","firebase")).thenReturn(List.of(Map.of("id","borrower")));
    loan=new LinkedHashMap<>(Map.of("id","loan","reserva_id","reservation","prestatario_usuario_id","borrower","prestamista_usuario_id","lender","estado","RESERVADO","moneda","PEN","tarifa_diaria_acordada",new BigDecimal("10.00"),"entrega_programada_en",LocalDateTime.of(2026,10,5,10,0),"devolucion_original_en",LocalDateTime.of(2026,10,7,10,0)));
    when(store.get("prestamos","loan")).thenAnswer(i->loan);
    when(db.queryForList(anyString(),anyMap())).thenReturn(List.of(Map.of("id","loan")));
    when(store.list(eq("transacciones_economicas"),eq("clave_idempotencia"),any())).thenReturn(List.of());
    when(store.create(anyString(),anyMap())).thenAnswer(i->{var row=new LinkedHashMap<String,Object>(i.getArgument(1));row.put("id",UUID.randomUUID().toString());return row;});
    when(store.update(eq("prestamos"),eq("loan"),anyMap())).thenAnswer(i->{loan.putAll(i.getArgument(2));return loan;});
  }
  @AfterEach void clean(){SecurityContextHolder.clearContext();}
  Map<String,Object> payment(){return Map.of("prestamo_id","loan","tipo","PAGO_TARIFA","monto",new BigDecimal("20.00"),"medio_pago_seleccionado","PAGO_EN_LINEA","clave_idempotencia",UUID.randomUUID().toString());}

  @Test void confirmsRentalPaymentAndUpdatesTheLoan(){
    var result=(Map<?,?>)service.execute("payment",Map.of(),payment(),Map.of());
    assertEquals("PENDIENTE_LIBERACION",result.get("estado"));
    assertNotNull(loan.get("pago_tarifa_confirmado_en"));
  }

  @Test void rejectsAnAmountDifferentFromTheAgreement(){
    var body=new LinkedHashMap<String,Object>(payment());body.put("monto",new BigDecimal("19.99"));
    assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.execute("payment",Map.of(),body,Map.of()));
    verify(store,never()).update(eq("prestamos"),eq("loan"),anyMap());
  }

  @Test void confirmsGuaranteeAndUpdatesTheLoan(){
    when(store.list("garantias","prestamo_id","loan")).thenReturn(List.of());
    when(store.get("reservas","reservation")).thenReturn(Map.of("garantia_monetaria_acordada",new BigDecimal("50.00")));
    var body=Map.<String,Object>of("prestamo_id","loan","medio_pago_seleccionado","PAGO_EN_LINEA","clave_idempotencia",UUID.randomUUID().toString());
    var result=(Map<?,?>)service.execute("guarantee",Map.of(),body,Map.of());
    assertEquals("CONSTITUIDA",result.get("estado"));
    assertNotNull(loan.get("garantia_constituida_en"));
  }
}
