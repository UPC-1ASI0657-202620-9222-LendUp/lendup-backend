package com.lendup.evidencias.application;
import com.lendup.shared.FlowSupport;
import com.lendup.shared.CloudinaryImageClient;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
@Service
public class EvidenciasApplicationService extends FlowSupport {
  static final int MAX_MEDIA=6,MAX_BYTES=5*1024*1024;
  private final CloudinaryImageClient cloud;
  public EvidenciasApplicationService(EvidenciasRepository store,JsonMapper mapper,CloudinaryImageClient cloud){super(store,mapper);this.cloud=cloud;}
  static String mediaMime(byte[] bytes,String declared){
    if(bytes.length==0||bytes.length>MAX_BYTES)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cada archivo debe pesar hasta 5 MB");
    if(bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)return "image/jpeg";
    if(bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
    if(bytes.length>=12&&new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))return "image/webp";
    if(bytes.length>=12&&new String(bytes,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp"))return "video/quicktime".equals(declared)?"video/quicktime":"video/mp4";
    if(bytes.length>=4&&(bytes[0]&255)==0x1A&&(bytes[1]&255)==0x45&&(bytes[2]&255)==0xDF&&(bytes[3]&255)==0xA3)return "video/webm";
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa archivos JPG, PNG, WebP, MP4, WebM o MOV");
  }
  @Transactional
  public Map<String,Object> upload(String id,String stage,MultipartFile file,String uploadId){
    var loan=participantLoan(id);
    if(!List.of("ENTREGA","DEVOLUCION").contains(stage))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Etapa de evidencia inválida");
    if("ENTREGA".equals(stage)){owns(loan,"prestamista_usuario_id");if(!"RESERVADO".equals(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"La entrega ya no admite evidencias iniciales");}
    else {owns(loan,"prestatario_usuario_id");if(!List.of("ACTIVO","VENCIDO").contains(loan.get("estado")))throw new ResponseStatusException(HttpStatus.CONFLICT,"La devolución aún no admite evidencias finales");}
    String requestId;try{requestId=UUID.fromString(uploadId).toString();}catch(Exception error){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Identificador de subida inválido");}
    var publicId="lendup/evidencias/"+id+"/"+stage.toLowerCase(Locale.ROOT)+"/"+requestId;
    var previous=store.list("evidencias","cloudinary_public_id",publicId);
    if(!previous.isEmpty()){
      var saved=previous.getFirst();
      if(id.equals(saved.get("prestamo_id"))&&stage.equals(saved.get("etapa"))&&userId().equals(saved.get("registrada_por_usuario_id")))return saved;
      throw new ResponseStatusException(HttpStatus.CONFLICT,"La subida ya corresponde a otra evidencia");
    }
    var mediaCount=store.list("evidencias","prestamo_id",id).stream().filter(row->stage.equals(row.get("etapa"))&&List.of("FOTO","VIDEO").contains(row.get("tipo"))).count();
    if(mediaCount>=MAX_MEDIA)throw new ResponseStatusException(HttpStatus.CONFLICT,"Puedes adjuntar hasta 6 fotos o videos por etapa");
    byte[] bytes;try{bytes=file.getBytes();}catch(java.io.IOException error){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No pudimos leer el archivo");}
    var mime=mediaMime(bytes,file.getContentType());var video=mime.startsWith("video/");cloud.requireConfigured();
    var asset=cloud.uploadMedia(bytes,mime,publicId);
    try{return store.create("evidencias",Map.of("prestamo_id",id,"registrada_por_usuario_id",userId(),"etapa",stage,"tipo",video?"VIDEO":"FOTO","url",asset.url(),"cloudinary_public_id",asset.publicId(),"estado_integracion","COMPLETADA"));}
    catch(RuntimeException error){try{cloud.deleteMedia(asset.publicId(),video);}catch(RuntimeException cleanup){org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo limpiar la evidencia huérfana {}",asset.publicId());}throw error;}
  }
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
