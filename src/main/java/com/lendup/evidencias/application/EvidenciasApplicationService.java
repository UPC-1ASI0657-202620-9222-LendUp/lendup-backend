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
      case "incident":{
        var loan=participantLoan(string(b,"prestamo_id"));
        var incident=create("incidencias",b,Map.of("reportada_por_usuario_id",userId(),"estado","PENDIENTE"));
        var pending=((Number)loan.get("incidencias_pendientes")).intValue();
        store.update("prestamos",loan.get("id").toString(),Map.of("incidencias_pendientes",pending+1));
        return incident;
      }
      case "incidentById":{
        var incident=store.get("incidencias",id);
        participantLoan(incident.get("prestamo_id").toString());
        return incident;
      }
      case "adminIncidents":{
        admin();var result=new ArrayList<Map<String,Object>>();
        for(var incident:store.list("incidencias","estado",q.get("estado")))
          if(!q.containsKey("tipo")||q.get("tipo").equals(incident.get("tipo")))result.add(incident);
        return result;
      }
      case "resolve":{
        admin();var data=editable(b);
        var incident=store.get("incidencias",id);
        if("RESUELTA".equals(incident.get("estado")))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Incidencia ya resuelta");
        for(var key:List.of("justificacion_resolucion","decision_garantia","monto_garantia_afectado","saldo_garantia_previsto"))
          if(data.get(key)==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta "+key);
        data.put("estado","RESUELTA");data.put("resuelta_por_usuario_id",userId());data.put("resuelta_en",now());
        var resolved=store.updateTrusted("incidencias",id,data);
        var loan=store.get("prestamos",incident.get("prestamo_id").toString());
        var pending=((Number)loan.get("incidencias_pendientes")).intValue();
        store.update("prestamos",loan.get("id").toString(),Map.of("incidencias_pendientes",Math.max(0,pending-1)));
        return resolved;
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
