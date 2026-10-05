package com.lendup.identidad.application;
import com.lendup.shared.FlowSupport;
import com.lendup.identidad.domain.repositories.IdentidadRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class IdentidadApplicationService extends FlowSupport {
  private final com.lendup.shared.TermsDocumentService terms;
  private final com.lendup.shared.UniversityCatalogService universities;
  public IdentidadApplicationService(IdentidadRepository store,JsonMapper mapper,com.lendup.shared.TermsDocumentService terms,com.lendup.shared.UniversityCatalogService universities){super(store,mapper);this.terms=terms;this.universities=universities;}
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    var details=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getDetails();
    var token=details instanceof com.google.firebase.auth.FirebaseToken value?value:null;
    boolean emailVerified=token!=null&&Boolean.TRUE.equals(token.getClaims().get("email_verified"));
    switch(a){
      case "createUser": {
        if(!store.list("usuarios","firebase_uid",uid()).isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Usuario registrado");
        if(token==null||token.getEmail()==null||!token.getEmail().equalsIgnoreCase(string(b,"correo_institucional").trim()))
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El correo debe coincidir con la cuenta Firebase");
        var university=universities.resolve(token.getEmail());
        var role=store.list("roles","codigo","ESTUDIANTE");
        if(role.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Falta rol ESTUDIANTE");
        var u=store.create("usuarios",Map.of("firebase_uid",uid(),"correo_institucional",string(b,"correo_institucional"),
          "rol_id",role.getFirst().get("id"),"estado_verificacion",emailVerified?"VERIFICADO":"NO_VERIFICADO"));
        var p=editable(b);p.put("universidad",university.id());p.put("campus",com.lendup.shared.UniversityCatalogService.campus(b.get("campus")));p.put("usuario_id",u.get("id"));store.create("perfiles",p);return u;
      }
      case "me":{
        var current=user();
        if(emailVerified&&!"VERIFICADO".equals(current.get("estado_verificacion")))
          current=store.update("usuarios",current.get("id").toString(),Map.of("estado_verificacion","VERIFICADO","verificado_en",now()));
        var profiles=store.list("perfiles","usuario_id",current.get("id"));
        return Map.of("usuario",current,"perfil",profiles.isEmpty()?Map.of():profiles.getFirst());
      }
      case "updateMe":{
        var changes=editable(b);changes.remove("universidad");
        if(changes.containsKey("campus"))changes.put("campus",com.lendup.shared.UniversityCatalogService.campus(changes.get("campus")));
        return store.update("perfiles",store.list("perfiles","usuario_id",userId()).getFirst().get("id").toString(),changes);
      }
      case "verify":{
        var reference=string(b,"verificacion_referencia");
        if(reference.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requiere verificacion_referencia");
        return store.update("usuarios",userId(),Map.of("estado_verificacion","PENDIENTE",
          "verificacion_referencia",reference,"verificacion_solicitada_en",now()));
      }
      case "terms":{
        terms.validateAcceptance(string(b,"version_terminos_aceptada"),string(b,"version_descargo_aceptada"));
        if(string(b,"version_terminos_aceptada").isBlank()||string(b,"version_descargo_aceptada").isBlank())
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requieren ambas versiones aceptadas");
        return store.update("usuarios",userId(),Map.of("version_terminos_aceptada",b.get("version_terminos_aceptada"),
          "version_descargo_aceptada",b.get("version_descargo_aceptada"),"aceptados_en",now()));
      }
      case "userById":{
        var target=store.get("usuarios",id);var profiles=store.list("perfiles","usuario_id",id);
        var response=new LinkedHashMap<String,Object>();response.put("id",id);
        response.put("estado_verificacion",target.get("estado_verificacion"));
        if(!profiles.isEmpty()){
          var profile=profiles.getFirst();for(var k:List.of("nombre","universidad","campus","carrera","ciclo","foto_url"))response.put(k,profile.get(k));
        }return response;
      }
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
