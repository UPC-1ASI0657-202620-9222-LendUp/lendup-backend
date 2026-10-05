package com.lendup.catalogo.application;
import com.lendup.shared.FlowSupport;
import com.lendup.catalogo.domain.repositories.CatalogoRepository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class CatalogoApplicationService extends FlowSupport {
  private final CatalogoRepository catalog;
  private final com.lendup.shared.TermsDocumentService terms;
  private final com.lendup.shared.PublicationImagesService images;
  public CatalogoApplicationService(CatalogoRepository store,JsonMapper mapper,com.lendup.shared.TermsDocumentService terms,com.lendup.shared.PublicationImagesService images){super(store,mapper);this.catalog=store;this.terms=terms;this.images=images;}
  private Map<String,Object> withAvailability(Map<String,Object> publication){
    var result=new LinkedHashMap<String,Object>(publication);
    result.put("disponibilidades",catalog.availability(publication.get("id").toString()));
    return result;
  }
  private List<Map<String,Object>> withAvailability(List<Map<String,Object>> publications){
    return publications.stream().map(this::withAvailability).toList();
  }
  @Transactional
  public Object execute(String a,Map<String,String> v,Map<String,Object>b,Map<String,String>q){
    String id=id(v);
    switch(a){
      case "createPublication":return create("publicaciones",b,Map.of("propietario_usuario_id",userId()));
      case "updatePublication":case "publicationState":{
        owns(store.get("publicaciones",id),"propietario_usuario_id");
        // The typed update request is allowed to replace categoria_id; owner_id
        // is never part of either REST request contract.
        return a.equals("updatePublication")?store.updateTrusted("publicaciones",id,b):store.update("publicaciones",id,b);
      }
      case "availability":{
        catalog.lockPublication(id);
        owns(store.get("publicaciones",id),"propietario_usuario_id");
        validPeriod(b);
        if(catalog.availabilityOverlaps(id,null,b.get("desde"),b.get("hasta")))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"El intervalo se superpone con otra disponibilidad");
        return create("disponibilidades_publicacion",b,Map.of("publicacion_id",id));
      }
      case "listAvailability":{
        owns(store.get("publicaciones",id),"propietario_usuario_id");
        return catalog.availability(id);
      }
      case "updateAvailability":{
        catalog.lockPublication(id);
        owns(store.get("publicaciones",id),"propietario_usuario_id");
        var slot=store.get("disponibilidades_publicacion",v.get("subid"));
        if(!id.equals(Objects.toString(slot.get("publicacion_id"),"")))
          throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Intervalo de disponibilidad no encontrado");
        validPeriod(b);
        if(catalog.availabilityOverlaps(id,v.get("subid"),b.get("desde"),b.get("hasta")))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"El intervalo se superpone con otra disponibilidad");
        return store.update("disponibilidades_publicacion",v.get("subid"),b);
      }
      case "deleteAvailability":{
        catalog.lockPublication(id);
        owns(store.get("publicaciones",id),"propietario_usuario_id");
        catalog.deleteAvailability(id,v.get("subid"));
        return Map.of();
      }
      case "listPublications":return images.decorate(withAvailability(store.searchPublications(q)));
      case "publication":return images.decorate(withAvailability(store.get("publicaciones",id)));
      case "termsDocument":return terms.document();
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
