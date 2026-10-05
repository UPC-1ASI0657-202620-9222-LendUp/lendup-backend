package com.lendup.catalogo.infrastructure.persistence.jdbc;
import com.lendup.catalogo.domain.repositories.CatalogoRepository;
import com.lendup.shared.Store;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import org.springframework.stereotype.Repository;
@Repository
public class CatalogoRepositoryAdapter implements CatalogoRepository {
  private final Store store;
  public CatalogoRepositoryAdapter(Store store){this.store=store;}
  public Map<String,Object> get(String table,String id){return store.get(table,id);}
  public List<Map<String,Object>> list(String table,String field,Object value){return store.list(table,field,value);}
  public Map<String,Object> create(String table,Map<String,Object> data){return store.create(table,data);}
  public Map<String,Object> update(String table,String id,Map<String,Object> data){return store.update(table,id,data);}
  public Map<String,Object> updateTrusted(String table,String id,Map<String,Object> data){return store.updateTrusted(table,id,data);}
  public void lockAgenda(String agendaId){store.lockAgenda(agendaId);}
  public boolean overlaps(String agendaId,Object from,Object to){return store.overlaps(agendaId,from,to);}
  public boolean offered(String publicationId,Object from,Object to){return store.offered(publicationId,from,to);}
  public List<Map<String,Object>> searchPublications(Map<String,String> filters){return store.searchPublications(filters);}
  public List<Map<String,Object>> availability(String publicationId){return store.availability(publicationId);}
  public Map<String,List<Map<String,Object>>> availabilityForPublications(Collection<String> publicationIds){return store.availabilityForPublications(publicationIds);}
  public void lockPublication(String publicationId){store.lockPublication(publicationId);}
  public boolean availabilityOverlaps(String publicationId,String excludedId,Object from,Object to){return store.availabilityOverlaps(publicationId,excludedId,from,to);}
  public void deleteAvailability(String publicationId,String availabilityId){store.deleteAvailability(publicationId,availabilityId);}
}
