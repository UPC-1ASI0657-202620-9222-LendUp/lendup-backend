package com.lendup.pagos.application;
import com.lendup.shared.FlowSupport;
import com.lendup.pagos.domain.repositories.PagosRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class PagosApplicationService extends FlowSupport {
  public PagosApplicationService(PagosRepository store,JsonMapper mapper){super(store,mapper);}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "paymentMethods":return List.of(Map.of("codigo","MERCADO_PAGO","nombre","Mercado Pago","estado","PENDIENTE_INTEGRACION"));
      case "payment":{
        var loan=participantLoan(string(b,"prestamo_id"));
        owns(loan,"prestatario_usuario_id");
        var type=string(b,"tipo");
        if(!List.of("PAGO_TARIFA","PAGO_EXTENSION").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de pago inválido");
        if(b.get("monto")==null||string(b,"medio_pago_seleccionado").isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requiere monto y medio_pago_seleccionado");
        var data=editable(b);data.put("tipo",type);data.put("estado","PENDIENTE");data.put("moneda",loan.get("moneda"));data.put("clave_idempotencia",newIdempotency(b));
        return store.create("transacciones_economicas",data);
      }
      case "guarantee":{
        var loan=participantLoan(string(b,"prestamo_id"));owns(loan,"prestatario_usuario_id");
        var found=store.list("garantias","prestamo_id",loan.get("id"));
        if(!found.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Garantía ya registrada");
        var guarantee=store.create("garantias",guaranteeForLoan(loan));
        if(!string(b,"medio_pago_seleccionado").isBlank()&&!string(b,"clave_idempotencia").isBlank()){
          store.create("transacciones_economicas",Map.of("prestamo_id",loan.get("id"),"garantia_id",guarantee.get("id"),
            "tipo","CONSTITUCION_GARANTIA","estado","PENDIENTE","monto",guarantee.get("monto_acordado"),
            "moneda",guarantee.get("moneda"),"medio_pago_seleccionado",b.get("medio_pago_seleccionado"),
            "clave_idempotencia",newIdempotency(b)));
        }return guarantee;
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
