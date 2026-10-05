package com.lendup.pagos.application;
import com.lendup.shared.FlowSupport;
import com.lendup.pagos.domain.repositories.PagosRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
@Service
public class PagosApplicationService extends FlowSupport {
  private final NamedParameterJdbcTemplate db;
  public PagosApplicationService(PagosRepository store,JsonMapper mapper,NamedParameterJdbcTemplate db){super(store,mapper);this.db=db;}
  private Map<String,Object> lockedLoan(String id){
    var loan=participantLoan(id);
    db.queryForList("SELECT id FROM prestamos WHERE id=:id FOR UPDATE",Map.of("id",id));
    return participantLoan(loan.get("id").toString());
  }
  private BigDecimal money(Object value){return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
  private LocalDateTime date(Object value){
    if(value instanceof LocalDateTime local)return local;
    if(value instanceof java.sql.Timestamp timestamp)return timestamp.toLocalDateTime();
    return LocalDateTime.parse(value.toString().replace(' ','T'));
  }
  private BigDecimal rentalAmount(Map<String,Object> loan){
    var seconds=Duration.between(date(loan.get("entrega_programada_en")),date(loan.get("devolucion_original_en"))).getSeconds();
    var days=Math.max(1,(seconds+86_399)/86_400);
    return money(loan.get("tarifa_diaria_acordada")).multiply(BigDecimal.valueOf(days)).setScale(2,RoundingMode.HALF_UP);
  }
  private Optional<Map<String,Object>> repeated(Map<String,Object> body,String loanId,String type){
    var key=newIdempotency(body);var rows=store.list("transacciones_economicas","clave_idempotencia",key);
    if(rows.isEmpty())return Optional.empty();
    var row=rows.getFirst();
    if(!loanId.equals(row.get("prestamo_id"))||!type.equals(row.get("tipo")))throw new ResponseStatusException(HttpStatus.CONFLICT,"La clave de pago ya fue utilizada");
    return Optional.of(row);
  }
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "paymentMethods":return List.of(Map.of("codigo","PAGO_EN_LINEA","nombre","Pago en línea","estado","ACTIVO"));
      case "payment":{
        var loan=lockedLoan(string(b,"prestamo_id"));
        owns(loan,"prestatario_usuario_id");
        var type=string(b,"tipo");
        if(!List.of("PAGO_TARIFA","PAGO_EXTENSION").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de pago inválido");
        if(b.get("monto")==null||string(b,"medio_pago_seleccionado").isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requiere monto y medio_pago_seleccionado");
        var repeat=repeated(b,loan.get("id").toString(),type);if(repeat.isPresent())return repeat.get();
        if(!"PAGO_EN_LINEA".equals(string(b,"medio_pago_seleccionado")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Medio de pago inválido");
        if(!"RESERVADO".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"El préstamo ya no admite pagos");
        if("PAGO_TARIFA".equals(type)){
          if(loan.get("pago_tarifa_confirmado_en")!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"La tarifa ya fue pagada");
          if(money(b.get("monto")).compareTo(rentalAmount(loan))!=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El importe no coincide con la tarifa acordada");
        }
        var confirmed=now();var data=editable(b);data.put("tipo",type);data.put("estado","PENDIENTE_LIBERACION");data.put("moneda",loan.get("moneda"));data.put("clave_idempotencia",newIdempotency(b));data.put("confirmada_en",confirmed);
        var transaction=store.create("transacciones_economicas",data);
        if("PAGO_TARIFA".equals(type))store.update("prestamos",loan.get("id").toString(),Map.of("pago_tarifa_confirmado_en",confirmed));
        return transaction;
      }
      case "guarantee":{
        var loan=lockedLoan(string(b,"prestamo_id"));owns(loan,"prestatario_usuario_id");
        var repeat=repeated(b,loan.get("id").toString(),"CONSTITUCION_GARANTIA");if(repeat.isPresent())return repeat.get();
        if(!"PAGO_EN_LINEA".equals(string(b,"medio_pago_seleccionado")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Medio de pago inválido");
        if(!"RESERVADO".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"El préstamo ya no admite garantías");
        var found=store.list("garantias","prestamo_id",loan.get("id"));
        if(!found.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Garantía ya registrada");
        var confirmed=now();var guaranteeData=new LinkedHashMap<String,Object>(guaranteeForLoan(loan));guaranteeData.put("estado","CONSTITUIDA");guaranteeData.put("monto_constituido",guaranteeData.get("monto_acordado"));guaranteeData.put("constituida_en",confirmed);
        var guarantee=store.create("garantias",guaranteeData);
        var transaction=store.create("transacciones_economicas",Map.of("prestamo_id",loan.get("id"),"garantia_id",guarantee.get("id"),
          "tipo","CONSTITUCION_GARANTIA","estado","CONSTITUIDA","monto",guarantee.get("monto_acordado"),"confirmada_en",confirmed,
          "moneda",guarantee.get("moneda"),"medio_pago_seleccionado",b.get("medio_pago_seleccionado"),"clave_idempotencia",newIdempotency(b)));
        store.update("prestamos",loan.get("id").toString(),Map.of("garantia_constituida_en",confirmed));return transaction;
      }
      case "webhook":{
        if(b.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta payload");
        try{
          var data=new LinkedHashMap<String,Object>();data.put("proveedor","MERCADO_PAGO");
          var event=string(b,"id");if(!event.isBlank())data.put("evento_proveedor_id",event);
          data.put("payload_json",json.writeValueAsString(b));data.put("estado","PENDIENTE_VALIDACION");
          return store.create("webhook_events",data);
        }catch(tools.jackson.core.JacksonException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payload inválido",e);}
      }
      case "transactions":participantLoan(id);return store.list("transacciones_economicas","prestamo_id",id);
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
