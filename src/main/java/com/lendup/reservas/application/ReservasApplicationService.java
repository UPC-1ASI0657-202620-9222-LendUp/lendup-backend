package com.lendup.reservas.application;
import com.lendup.shared.FlowSupport;
import com.lendup.reservas.domain.repositories.ReservasRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class ReservasApplicationService extends FlowSupport {
  public ReservasApplicationService(ReservasRepository store,JsonMapper mapper){super(store,mapper);}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "createRequest":{
        validPeriod(b);
        var pub=store.get("publicaciones",string(b,"publicacion_id"));
        if(!"ACTIVA".equals(pub.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Objeto no disponible");
        if(b.get("desde")==null||b.get("hasta")==null||!store.offered(pub.get("id").toString(),b.get("desde"),b.get("hasta")))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Periodo fuera de la disponibilidad publicada");
        var fields=editable(b);fields.put("prestatario_usuario_id",userId());fields.put("prestamista_usuario_id",pub.get("propietario_usuario_id"));
        if(userId().equals(pub.get("propietario_usuario_id")))throw new ResponseStatusException(HttpStatus.CONFLICT,"No puedes solicitar tu propio objeto");
        for(var k:List.of("tarifa_diaria","garantia_monetaria","moneda","lugar_intercambio","condiciones_uso","condiciones_entrega","condiciones_devolucion","condiciones_cancelacion")){
          var suffix=k.equals("lugar_intercambio")?"_aceptado":List.of("tarifa_diaria","garantia_monetaria","moneda").contains(k)?"_aceptada":"_aceptadas";
          if(k.equals("moneda"))suffix="_aceptada";
          var target=k+suffix;if(pub.get(k)!=null)fields.put(target,pub.get(k));
        }
        fields.putIfAbsent("condiciones_aceptadas_en",java.time.LocalDateTime.now(java.time.Clock.systemUTC()));
        var request=store.create("solicitudes",fields);
        notifyUser(pub.get("propietario_usuario_id"),"SOLICITUD",request.get("id").toString(),
          "Nueva solicitud","Recibiste una solicitud para tu objeto");
        return request;
      }
      case "acceptRequest":{
        var req=store.get("solicitudes",id);owns(req,"prestamista_usuario_id");
        if(!"PENDIENTE".equals(req.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solicitud no pendiente");
        var agendas=store.list("agendas_objeto","publicacion_id",req.get("publicacion_id"));
        var agenda=agendas.isEmpty()?store.create("agendas_objeto",Map.of("publicacion_id",req.get("publicacion_id"),"version",0)):agendas.getFirst();
        store.lockAgenda(agenda.get("id").toString());
        if(store.overlaps(agenda.get("id").toString(),req.get("desde"),req.get("hasta")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Periodo ya reservado");
        var data=new LinkedHashMap<String,Object>();data.put("solicitud_id",id);data.put("agenda_id",agenda.get("id"));
        data.put("desde",req.get("desde"));data.put("hasta",req.get("hasta"));data.put("estado","CONFIRMADA");
        for(var k:List.of("tarifa_diaria","garantia_monetaria","moneda","lugar_intercambio","condiciones_uso","condiciones_entrega","condiciones_devolucion","condiciones_cancelacion")){
          for(var suffix:List.of("_aceptada","_aceptado","_aceptadas")){
            var val=req.get(k+suffix);
            if(val!=null)data.put(k+suffix.replace("_aceptad","_acordad"),val);
          }
        }
        var reservation=store.create("reservas",data);change("solicitudes",id,"estado","ACEPTADA");
        var loan=new LinkedHashMap<String,Object>();loan.put("reserva_id",reservation.get("id"));loan.put("publicacion_id",req.get("publicacion_id"));
        loan.put("prestamista_usuario_id",req.get("prestamista_usuario_id"));loan.put("prestatario_usuario_id",req.get("prestatario_usuario_id"));
        loan.put("entrega_programada_en",req.get("desde"));loan.put("devolucion_original_en",req.get("hasta"));loan.put("devolucion_vigente_en",req.get("hasta"));
        loan.put("tarifa_diaria_acordada",req.get("tarifa_diaria_aceptada"));loan.put("moneda",req.get("moneda_aceptada"));
        loan.put("garantia_requerida",req.get("garantia_monetaria_aceptada")!=null);
        store.create("prestamos",loan);
        notifyUser(req.get("prestatario_usuario_id"),"RESERVA",reservation.get("id").toString(),"Reserva confirmada","Tu solicitud fue aceptada");
        notifyUser(req.get("prestamista_usuario_id"),"RESERVA",reservation.get("id").toString(),"Reserva confirmada","Confirmaste una reserva");
        return reservation;
      }
      case "rejectRequest":case "cancelRequest":{
        var req=store.get("solicitudes",id);owns(req,a.equals("rejectRequest")?"prestamista_usuario_id":"prestatario_usuario_id");
        if(!"PENDIENTE".equals(req.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solicitud no pendiente");
        var update=new LinkedHashMap<String,Object>();
        update.put("estado",a.equals("rejectRequest")?"RECHAZADA":"CANCELADA");
        update.put(a.equals("rejectRequest")?"respondida_en":"cancelada_en",now());
        if(a.equals("rejectRequest")&&!string(b,"motivo_rechazo").isBlank())
          update.put("motivo_rechazo",b.get("motivo_rechazo"));
        return store.update("solicitudes",id,update);
      }
      case "request":{var req=store.get("solicitudes",id);if(!userId().equals(req.get("prestatario_usuario_id"))&&!userId().equals(req.get("prestamista_usuario_id")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return req;}
      case "listRequests":{
        var result=new ArrayList<Map<String,Object>>();for(var req:store.list("solicitudes",null,null)){
          var requestedRole=q.getOrDefault("rol","");
          var eligible=requestedRole.equals("PRESTAMISTA")?userId().equals(req.get("prestamista_usuario_id")):
            requestedRole.equals("PRESTATARIO")?userId().equals(req.get("prestatario_usuario_id")):
            userId().equals(req.get("prestatario_usuario_id"))||userId().equals(req.get("prestamista_usuario_id"));
          if(eligible&&(!q.containsKey("estado")||q.get("estado").equals(req.get("estado"))))result.add(req);
        }return result;
      }
      case "listReservations":{
        var result=new ArrayList<Map<String,Object>>();
        for(var r:store.list("reservas",null,null)){
          var request=store.get("solicitudes",r.get("solicitud_id").toString());
          var requestedRole=q.getOrDefault("rol","");
          var eligible=requestedRole.equals("PRESTAMISTA")?userId().equals(request.get("prestamista_usuario_id")):
            requestedRole.equals("PRESTATARIO")?userId().equals(request.get("prestatario_usuario_id")):
            userId().equals(request.get("prestatario_usuario_id"))||userId().equals(request.get("prestamista_usuario_id"));
          if(eligible)result.add(r);
        }return result;
      }
      case "cancelReservation":{
        var reservation=store.get("reservas",id);var request=store.get("solicitudes",reservation.get("solicitud_id").toString());
        if(!userId().equals(request.get("prestatario_usuario_id"))&&!userId().equals(request.get("prestamista_usuario_id")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(!"CONFIRMADA".equals(reservation.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Reserva no confirmada");
        var loan=store.list("prestamos","reserva_id",id);
        if(!loan.isEmpty()&&!"RESERVADO".equals(loan.getFirst().get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Préstamo iniciado");
        if(!loan.isEmpty())store.update("prestamos",loan.getFirst().get("id").toString(),Map.of("estado","CANCELADO","cancelado_en",now()));
        var update=new LinkedHashMap<String,Object>();update.put("estado","CANCELADA");
        update.put("cancelada_en",now());update.put("cancelada_por_usuario_id",userId());
        if(!string(b,"motivo_cancelacion").isBlank())update.put("motivo_cancelacion",b.get("motivo_cancelacion"));
        return store.updateTrusted("reservas",id,update);
      }
      case "contact":{
        var reservation=store.get("reservas",id);
        if(!"CONFIRMADA".equals(reservation.get("estado")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var request=store.get("solicitudes",reservation.get("solicitud_id").toString());
        var me=userId();var other=me.equals(request.get("prestatario_usuario_id"))?request.get("prestamista_usuario_id"):
          me.equals(request.get("prestamista_usuario_id"))?request.get("prestatario_usuario_id"):null;
        if(other==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var profiles=store.list("perfiles","usuario_id",other);return profiles.isEmpty()?Map.of():Map.of("telefono",profiles.getFirst().get("telefono"));
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
