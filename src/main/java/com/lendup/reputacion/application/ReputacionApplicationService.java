package com.lendup.reputacion.application;
import com.lendup.shared.FlowSupport;
import com.lendup.reputacion.domain.repositories.ReputacionRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class ReputacionApplicationService extends FlowSupport {
  public ReputacionApplicationService(ReputacionRepository store,JsonMapper mapper){super(store,mapper);}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "rating":{
        var loan=participantLoan(id);
        if(!"FINALIZADO".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se califica un préstamo finalizado");
        if(!(b.get("puntaje") instanceof Number score)||score.intValue()<1||score.intValue()>5)
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El puntaje debe estar entre 1 y 5");
        var other=userId().equals(loan.get("prestatario_usuario_id"))?loan.get("prestamista_usuario_id"):loan.get("prestatario_usuario_id");
        return create("calificaciones",b,Map.of("prestamo_id",id,"evaluador_usuario_id",userId(),"evaluado_usuario_id",other));
      }
      case "reputation":{
        var ratings=store.list("calificaciones","evaluado_usuario_id",id);
        var avg=ratings.stream().mapToInt(x->((Number)x.get("puntaje")).intValue()).average();
        return Map.of("usuario_id",id,"cantidad",ratings.size(),"promedio",avg.isPresent()?avg.getAsDouble():0,"calificaciones",ratings);
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
