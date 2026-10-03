package com.lendup.notificaciones.application;
import com.lendup.shared.FlowSupport;
import com.lendup.notificaciones.domain.repositories.NotificacionesRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class NotificacionesApplicationService extends FlowSupport {
  public NotificacionesApplicationService(NotificacionesRepository store,JsonMapper mapper){super(store,mapper);}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "listNotifications":return store.list("notificaciones","destinatario_usuario_id",userId());
      case "readNotification":{
        var note=store.get("notificaciones",id);owns(note,"destinatario_usuario_id");
        if(!"DISPONIBLE".equals(note.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Aviso no disponible");
        return store.update("notificaciones",id,Map.of("estado","LEIDA","leida_en",now()));
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
