package com.lendup.shared;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PublicationImagesService {
  public static final int MAX_IMAGES=6,MAX_BYTES=5*1024*1024;
  private final NamedParameterJdbcTemplate db;
  private final CloudinaryImageClient cloud;
  public PublicationImagesService(NamedParameterJdbcTemplate db,CloudinaryImageClient cloud){this.db=db;this.cloud=cloud;}
  public List<Map<String,Object>> decorate(List<Map<String,Object>> publications){
    if(publications.isEmpty())return publications;
    var ids=publications.stream().map(p->p.get("id").toString()).toList();
    var rows=db.queryForList("SELECT id,publicacion_id,url,orden FROM imagenes_publicacion WHERE publicacion_id IN (:ids) ORDER BY orden,creado_en",Map.of("ids",ids));
    return publications.stream().map(p->{var copy=new LinkedHashMap<>(p);copy.put("imagenes",rows.stream().filter(i->i.get("publicacion_id").equals(p.get("id"))).toList());return (Map<String,Object>)copy;}).toList();
  }
  public Map<String,Object> decorate(Map<String,Object> publication){return decorate(List.of(publication)).getFirst();}
  private void owner(String id){
    var rows=db.queryForList("SELECT p.id FROM publicaciones p JOIN usuarios u ON u.id=p.propietario_usuario_id WHERE p.id=:id AND u.firebase_uid=:uid FOR UPDATE",Map.of("id",id,"uid",SecurityContextHolder.getContext().getAuthentication().getName()));
    if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el propietario puede modificar las fotos");
  }
  static String mime(byte[] bytes){
    if(bytes.length==0||bytes.length>MAX_BYTES)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cada foto debe pesar hasta 5 MB");
    if(bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)return "image/jpeg";
    if(bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
    if(bytes.length>=12&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP"))return "image/webp";
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa una foto JPG, PNG o WebP");
  }
  @Transactional public Map<String,Object> upload(String publicationId,MultipartFile file,String uploadId){
    owner(publicationId);cloud.requireConfigured();
    String imageId;
    try{imageId=uploadId==null?UUID.randomUUID().toString():UUID.fromString(uploadId).toString();}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Identificador de subida inválido");}
    var previous=db.queryForList("SELECT id,url,orden FROM imagenes_publicacion WHERE id=:imageId AND publicacion_id=:publicationId",Map.of("imageId",imageId,"publicationId",publicationId));
    if(!previous.isEmpty())return previous.getFirst();
    var params=Map.of("id",publicationId);
    var rows=db.queryForList("SELECT orden FROM imagenes_publicacion WHERE publicacion_id=:id ORDER BY orden",params);
    if(rows.size()>=MAX_IMAGES)throw new ResponseStatusException(HttpStatus.CONFLICT,"Puedes añadir hasta 6 fotos");
    byte[] bytes;try{bytes=file.getBytes();}catch(java.io.IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No pudimos leer la foto");}
    var mime=mime(bytes);var publicId="lendup/objetos/"+publicationId+"/"+imageId;
    var asset=cloud.upload(bytes,mime,publicId);var order=rows.stream().mapToInt(r->((Number)r.get("orden")).intValue()).max().orElse(-1)+1;
    try{db.update("INSERT INTO imagenes_publicacion (id,publicacion_id,cloudinary_public_id,url,orden) VALUES (:id,:publication,:public,:url,:order)",Map.of("id",imageId,"publication",publicationId,"public",asset.publicId(),"url",asset.url(),"order",order));}
    catch(RuntimeException e){try{cloud.delete(asset.publicId());}catch(RuntimeException cleanup){org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo limpiar la foto huérfana {}",asset.publicId());}throw e;}
    return Map.of("id",imageId,"url",asset.url(),"orden",order);
  }
  @Transactional public void delete(String publicationId,String imageId){
    owner(publicationId);
    var params=Map.of("publication",publicationId,"id",imageId);
    var rows=db.queryForList("SELECT cloudinary_public_id FROM imagenes_publicacion WHERE id=:id AND publicacion_id=:publication",params);
    if(rows.isEmpty())return;
    cloud.delete(rows.getFirst().get("cloudinary_public_id").toString());
    db.update("DELETE FROM imagenes_publicacion WHERE id=:id AND publicacion_id=:publication",params);
  }
}
