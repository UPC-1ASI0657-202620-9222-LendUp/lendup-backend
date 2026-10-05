package com.lendup.evidencias.application;
import com.lendup.shared.FlowSupport;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class EvidenciasApplicationService extends FlowSupport {
  public EvidenciasApplicationService(EvidenciasRepository store,JsonMapper mapper){super(store,mapper);}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "evidence":{
        participantLoan(id);
        var data=editable(b);data.put("prestamo_id",id);data.put("registrada_por_usuario_id",userId());
        if(!string(data,"incidencia_id").isBlank()) {
          var incident=store.get("incidencias",string(data,"incidencia_id"));
          if(!id.equals(incident.get("prestamo_id")) || !"INCIDENCIA".equals(data.get("etapa"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Incidencia incompatible");
          if("RESUELTA".equals(incident.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Incidencia resuelta");
        }
        var type=string(data,"tipo");
        if(!List.of("FOTO","VIDEO","OBSERVACION").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de evidencia inválido");
        if(type.equals("OBSERVACION")){
          if(string(data,"observacion").isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta observacion");
          data.put("estado_integracion","COMPLETADA");data.remove("url");data.remove("cloudinary_public_id");
        }else if(!string(data,"url").isBlank()&&!string(data,"cloudinary_public_id").isBlank()){
          data.put("estado_integracion","REFERENCIA_REGISTRADA");
        }else{
          data.put("estado_integracion","PENDIENTE");data.remove("url");data.remove("cloudinary_public_id");
        }
        return store.create("evidencias",data);
      }
      case "analyze":{
        participantLoan(id);
        var data=editable(b);data.put("prestamo_id",id);data.put("solicitado_por_usuario_id",userId());
        data.put("estado","PENDIENTE");
        var initial=store.get("evidencias",string(data,"evidencia_inicial_id"));
        var last=store.get("evidencias",string(data,"evidencia_final_id"));
        if(!id.equals(initial.get("prestamo_id"))||!id.equals(last.get("prestamo_id"))||
            !"ENTREGA".equals(initial.get("etapa"))||!"DEVOLUCION".equals(last.get("etapa")))
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evidencias incompatibles con el préstamo");
        return store.create("analisis_evidencias",data);
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
