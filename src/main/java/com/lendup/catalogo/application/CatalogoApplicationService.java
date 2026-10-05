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
  private final com.lendup.shared.TermsDocumentService terms;
  public CatalogoApplicationService(CatalogoRepository store,JsonMapper mapper,com.lendup.shared.TermsDocumentService terms){super(store,mapper);this.terms=terms;}
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
        owns(store.get("publicaciones",id),"propietario_usuario_id");
        validPeriod(b);
        return create("disponibilidades_publicacion",b,Map.of("publicacion_id",id));
      }
      case "listPublications":return store.searchPublications(q);
      case "publication":return store.get("publicaciones",id);
      case "termsDocument":return terms.document();
      default:throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acción desconocida");
    }
  }
}
