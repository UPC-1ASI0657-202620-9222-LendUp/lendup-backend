package com.lendup.shared;

import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class IncidentsService extends FlowSupport {
  private final NamedParameterJdbcTemplate db;
  private final CloudinaryImageClient cloud;
  public IncidentsService(EvidenciasRepository store, JsonMapper json, NamedParameterJdbcTemplate db, CloudinaryImageClient cloud) {
    super(store,json); this.db=db; this.cloud=cloud;
  }
  private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
  private void open(Map<String,Object> incident) {
    if("RESUELTA".equals(incident.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"La incidencia ya está resuelta");
  }
  private String content(Map<String,Object> body,String field,int minimum) {
    var value=string(body,field).trim();
    if(value.length()<minimum || value.length()>10000) throw bad(field+": debe contener entre "+minimum+" y 10000 caracteres");
    return value;
  }
  private Map<String,Object> lockLoan(String id) {
    var rows=db.queryForList("SELECT * FROM prestamos WHERE id=:id FOR UPDATE",Map.of("id",id));
    if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Préstamo no encontrado");
    return rows.getFirst();
  }
  private Map<String,Object> access(String id) {
    var incident=store.get("incidencias",id);
    var loan=store.get("prestamos",incident.get("prestamo_id").toString());
    var me=userId();
    if(!me.equals(loan.get("prestatario_usuario_id")) && !me.equals(loan.get("prestamista_usuario_id"))) admin();
    return incident;
  }
  private BigDecimal money(Object value) { return value==null?BigDecimal.ZERO:new BigDecimal(value.toString()); }
  private Map<String,Object> decorate(Map<String,Object> incident) {
    var copy=new LinkedHashMap<>(incident);
    var loan=new LinkedHashMap<>(store.get("prestamos",incident.get("prestamo_id").toString()));
    var reservation=store.get("reservas",loan.get("reserva_id").toString());
    copy.put("garantia_monetaria_acordada",money(reservation.get("garantia_monetaria_acordada")));
    copy.put("reserva",reservation);
    loan.put("evidencias",db.queryForList("SELECT * FROM evidencias WHERE prestamo_id=:id ORDER BY registrada_en,id",Map.of("id",loan.get("id"))));
    copy.put("prestamo",loan);
    copy.put("evidencias",db.queryForList("SELECT * FROM evidencias WHERE incidencia_id=:id ORDER BY registrada_en,id",Map.of("id",incident.get("id"))));
    copy.put("observaciones",db.queryForList("SELECT * FROM observaciones_incidencia WHERE incidencia_id=:id ORDER BY registrada_en,id",Map.of("id",incident.get("id"))));
    var statements=db.queryForList("SELECT * FROM evidencias WHERE incidencia_id=:id AND tipo='OBSERVACION' AND registrada_por_usuario_id<>:reporter ORDER BY registrada_en,id LIMIT 1",Map.of("id",incident.get("id"),"reporter",incident.get("reportada_por_usuario_id")));
    if(!statements.isEmpty()) {copy.put("descargo",statements.getFirst().get("observacion"));copy.put("descargo_en",statements.getFirst().get("registrada_en"));}
    return copy;
  }
  public List<Map<String,Object>> list(boolean administrator,Map<String,String> filters) {
    if(administrator) admin();
    var sql=new StringBuilder("SELECT i.* FROM incidencias i JOIN prestamos p ON p.id=i.prestamo_id WHERE 1=1");
    var params=new LinkedHashMap<String,Object>();
    if(!administrator) {sql.append(" AND (p.prestatario_usuario_id=:me OR p.prestamista_usuario_id=:me)");params.put("me",userId());}
    for(var field:List.of("estado","tipo")) if(filters.get(field)!=null) {sql.append(" AND i."+field+"=:"+field);params.put(field,filters.get(field));}
    sql.append(" ORDER BY i.reportada_en DESC,i.id");
    return db.queryForList(sql.toString(),params).stream().map(this::decorate).toList();
  }
  public Map<String,Object> detail(String id) {return decorate(access(id));}
  public Map<String,Object> report(Map<String,Object> body,List<MultipartFile> files) {
    var loan=participantLoan(string(body,"prestamo_id"));
    loan=lockLoan(loan.get("id").toString());
    if(List.of("FINALIZADO","CANCELADO").contains(loan.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"El préstamo está cerrado");
    var type=string(body,"tipo");
    if(!List.of("DANIO","PERDIDA","RETRASO","NO_DEVOLUCION","OTRO").contains(type)) throw bad("Tipo de incidencia inválido");
    var description=content(body,"descripcion",20);
    var notes=new ArrayList<String>();
    if(body.get("observaciones") instanceof List<?> values) for(var value:values) notes.add(content(Map.of("nota",Objects.toString(value,"")),"nota",1));
    if(files.size()>6 || notes.size()>20) throw bad("Máximo 6 fotos y 20 observaciones");
    var bytes=new ArrayList<byte[]>();var mimes=new ArrayList<String>();
    for(var file:files) {try {var data=file.getBytes();mimes.add(PublicationImagesService.mime(data));bytes.add(data);} catch(java.io.IOException e){throw bad("No pudimos leer la foto");}}
    if(!files.isEmpty()) cloud.requireConfigured();
    var requestId=string(body,"clave_idempotencia");
    if(!requestId.isBlank()) {
      try {requestId=UUID.fromString(requestId).toString();}catch(IllegalArgumentException e){throw bad("Clave de idempotencia inválida");}
      var existing=db.queryForList("SELECT * FROM incidencias WHERE id=:id",Map.of("id",requestId));
      if(!existing.isEmpty()) {
        var old=existing.getFirst();
        if(!loan.get("id").equals(old.get("prestamo_id")) || !userId().equals(old.get("reportada_por_usuario_id")) || !type.equals(old.get("tipo")) || !description.equals(old.get("descripcion"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"La clave ya corresponde a otro reporte");
        return decorate(old);
      }
    }
    var uploaded=new ArrayList<String>();
    try {
      Map<String,Object> incident;
      if(requestId.isBlank()) incident=store.create("incidencias",Map.of("prestamo_id",loan.get("id"),"reportada_por_usuario_id",userId(),"tipo",type,"descripcion",description,"estado","PENDIENTE"));
      else {
        db.update("INSERT INTO incidencias (id,prestamo_id,reportada_por_usuario_id,tipo,descripcion,estado) VALUES (:id,:loan,:me,:type,:description,'PENDIENTE')",Map.of("id",requestId,"loan",loan.get("id"),"me",userId(),"type",type,"description",description));
        incident=store.get("incidencias",requestId);
      }
      var incidentId=incident.get("id").toString();
      for(var note:notes) store.create("evidencias",Map.of("prestamo_id",loan.get("id"),"incidencia_id",incidentId,"registrada_por_usuario_id",userId(),"etapa","INCIDENCIA","tipo","OBSERVACION","observacion",note,"estado_integracion","COMPLETADA"));
      for(int i=0;i<bytes.size();i++) {
        var asset=cloud.upload(bytes.get(i),mimes.get(i),"lendup/incidencias/"+incidentId+"/"+UUID.randomUUID());uploaded.add(asset.publicId());
        store.create("evidencias",Map.of("prestamo_id",loan.get("id"),"incidencia_id",incidentId,"registrada_por_usuario_id",userId(),"etapa","INCIDENCIA","tipo","FOTO","url",asset.url(),"cloudinary_public_id",asset.publicId(),"estado_integracion","COMPLETADA"));
      }
      db.update("UPDATE prestamos SET incidencias_pendientes=incidencias_pendientes+1 WHERE id=:id",Map.of("id",loan.get("id")));
      notifyParticipants(loan,incidentId,"Incidencia reportada","Se ha reportado una incidencia en tu préstamo.");
      return decorate(incident);
    } catch(RuntimeException error) {for(var asset:uploaded) try{cloud.delete(asset);}catch(RuntimeException ignored){} throw error;}
  }
  private void notifyParticipants(Map<String,Object> loan,String id,String title,String message) {
    for(var recipient:new HashSet<>(List.of(loan.get("prestatario_usuario_id"),loan.get("prestamista_usuario_id")))) store.create("notificaciones",Map.of("destinatario_usuario_id",recipient,"clave_notificacion",UUID.randomUUID().toString(),"tipo","EVENTO","origen_tipo","INCIDENCIA","origen_id",id,"evento_origen_id",UUID.randomUUID().toString(),"titulo",title,"mensaje",message,"estado","DISPONIBLE","disponible_en",now()));
  }
  public Map<String,Object> statement(String id,Map<String,Object> body) {
    var incident=access(id);participantLoan(incident.get("prestamo_id").toString());
    lockLoan(incident.get("prestamo_id").toString());incident=store.get("incidencias",id);open(incident);
    if(userId().equals(incident.get("reportada_por_usuario_id"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo la contraparte puede presentar el descargo");
    var text=content(body,"contenido",20);
    var previous=db.queryForList("SELECT id FROM evidencias WHERE incidencia_id=:id AND registrada_por_usuario_id=:me AND tipo='OBSERVACION'",Map.of("id",id,"me",userId()));
    if(!previous.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya presentaste tu descargo");
    store.create("evidencias",Map.of("prestamo_id",incident.get("prestamo_id"),"incidencia_id",id,"registrada_por_usuario_id",userId(),"etapa","INCIDENCIA","tipo","OBSERVACION","observacion",text,"estado_integracion","COMPLETADA"));
    notifyParticipants(store.get("prestamos",incident.get("prestamo_id").toString()),id,"Descargo presentado","La contraparte presentó su descargo.");
    return decorate(incident);
  }
  public Map<String,Object> review(String id) {
    admin();var incident=store.get("incidencias",id);lockLoan(incident.get("prestamo_id").toString());incident=store.get("incidencias",id);open(incident);
    if(!"EN_REVISION".equals(incident.get("estado"))) {
      incident=store.update("incidencias",id,Map.of("estado","EN_REVISION","revision_iniciada_en",now(),"actualizada_en",now()));
      notifyParticipants(store.get("prestamos",incident.get("prestamo_id").toString()),id,"Incidencia en revisión","Un administrador está revisando tu incidencia.");
    }
    return decorate(incident);
  }
  public Map<String,Object> note(String id,Map<String,Object> body) {
    admin();var incident=store.get("incidencias",id);lockLoan(incident.get("prestamo_id").toString());incident=store.get("incidencias",id);open(incident);
    if(!"EN_REVISION".equals(incident.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Inicia la revisión antes de añadir notas");
    store.create("observaciones_incidencia",Map.of("incidencia_id",id,"administrador_usuario_id",userId(),"contenido",content(body,"contenido",1)));
    return decorate(incident);
  }
  public Map<String,Object> resolve(String id,Map<String,Object> body) {
    admin();var incident=store.get("incidencias",id);var loan=lockLoan(incident.get("prestamo_id").toString());incident=store.get("incidencias",id);open(incident);
    if(!"EN_REVISION".equals(incident.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Inicia la revisión antes de resolver");
    var justification=content(body,"justificacion_resolucion",20);
    var reservation=store.get("reservas",loan.get("reserva_id").toString());
    var agreed=money(reservation.get("garantia_monetaria_acordada"));
    var sum=db.queryForList("SELECT COALESCE(SUM(monto_garantia_afectado),0) AS total FROM incidencias WHERE prestamo_id=:id AND estado='RESUELTA'",Map.of("id",loan.get("id"))).getFirst();
    var available=agreed.subtract(money(sum.get("total"))).max(BigDecimal.ZERO);
    BigDecimal amount;try{amount=money(body.get("monto_garantia_afectado")).setScale(2,java.math.RoundingMode.UNNECESSARY);}catch(RuntimeException e){throw bad("Monto inválido");}
    var decision=string(body,"decision_garantia");
    if(!List.of("SIN_AFECTACION","AFECTACION_PARCIAL","AFECTACION_TOTAL").contains(decision) || amount.signum()<0 || amount.compareTo(available)>0
      || (decision.equals("SIN_AFECTACION") && amount.signum()!=0)
      || (decision.equals("AFECTACION_PARCIAL") && (amount.signum()<=0 || amount.compareTo(available)>=0))
      || (decision.equals("AFECTACION_TOTAL") && (available.signum()<=0 || amount.compareTo(available)!=0))) throw bad("Decisión y monto incompatibles con la garantía disponible");
    var data=new LinkedHashMap<String,Object>();data.put("estado","RESUELTA");data.put("resuelta_en",now());data.put("resuelta_por_usuario_id",userId());data.put("justificacion_resolucion",justification);data.put("decision_garantia",decision);data.put("monto_garantia_afectado",amount);data.put("saldo_garantia_previsto",available.subtract(amount));data.put("moneda",loan.get("moneda"));data.put("actualizada_en",now());
    var resolved=store.updateTrusted("incidencias",id,data);
    db.update("UPDATE prestamos SET incidencias_pendientes=(SELECT COUNT(*) FROM incidencias WHERE prestamo_id=:id AND estado<>'RESUELTA') WHERE id=:id",Map.of("id",loan.get("id")));
    // This records the decision; it never fabricates a provider capture or refund.
    db.update("UPDATE garantias SET monto_afectado=:amount,actualizada_en=:now WHERE prestamo_id=:id",Map.of("amount",agreed.subtract(available).add(amount),"now",now(),"id",loan.get("id")));
    notifyParticipants(loan,id,"Incidencia resuelta",justification);
    return decorate(resolved);
  }
}
