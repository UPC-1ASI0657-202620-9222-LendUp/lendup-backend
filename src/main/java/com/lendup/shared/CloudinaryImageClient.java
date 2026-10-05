package com.lendup.shared;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
@Service
public class CloudinaryImageClient {
  public record Asset(String publicId,String url) {}
  private final String cloud,key,secret;
  private final JsonMapper json;
  private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public CloudinaryImageClient(@Value("${CLOUDINARY_CLOUD_NAME:}") String cloud,@Value("${CLOUDINARY_API_KEY:}") String key,@Value("${CLOUDINARY_API_SECRET:}") String secret,JsonMapper json){this.cloud=cloud;this.key=key;this.secret=secret;this.json=json;}
  public void requireConfigured(){if(!cloud.matches("[a-zA-Z0-9_-]+")||key.isBlank()||secret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La subida de fotos no está configurada");}
  static String signature(Map<String,String> params,String secret){
    try {var text=new TreeMap<>(params).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&"))+secret;
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8)));
    }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  protected Map<String,Object> request(String action,Map<String,String> signed,Map<String,String> extra){
    requireConfigured();var fields=new LinkedHashMap<>(signed);fields.putAll(extra);fields.put("api_key",key);fields.put("signature",signature(signed,secret));
    var body=fields.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
    try {
      var req=HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/"+cloud+"/image/"+action)).timeout(Duration.ofSeconds(40)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
      var response=http.send(req,HttpResponse.BodyHandlers.ofString());
      if(response.statusCode()<200||response.statusCode()>=300)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary no pudo procesar la foto. Reintenta.");
      return json.readValue(response.body(),new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    }catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"La subida fue interrumpida");}
    catch(java.io.IOException|IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No pudimos comunicarnos con Cloudinary. Reintenta.");}
  }
  public Asset upload(byte[] bytes,String mime,String publicId){
    var response=request("upload",Map.of("public_id",publicId,"timestamp",Long.toString(Instant.now().getEpochSecond()),"overwrite","false","allowed_formats","jpg,png,webp"),Map.of("file","data:"+mime+";base64,"+Base64.getEncoder().encodeToString(bytes)));
    var url=Objects.toString(response.get("secure_url"),"");
    if(!publicId.equals(response.get("public_id"))||!url.startsWith("https://res.cloudinary.com/"))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary devolvió una foto inválida");
    return new Asset(publicId,url);
  }
  public void delete(String publicId){var response=request("destroy",Map.of("public_id",publicId,"timestamp",Long.toString(Instant.now().getEpochSecond()),"invalidate","true"),Map.of());
    if(!List.of("ok","not found").contains(response.get("result")))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No pudimos eliminar la foto. Reintenta.");}
}
