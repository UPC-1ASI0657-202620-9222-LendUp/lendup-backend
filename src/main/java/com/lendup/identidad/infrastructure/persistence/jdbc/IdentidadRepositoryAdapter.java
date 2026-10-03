package com.lendup.identidad.infrastructure.persistence.jdbc;
import com.lendup.identidad.domain.repositories.IdentidadRepository;
import com.lendup.shared.Store;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
@Repository
public class IdentidadRepositoryAdapter implements IdentidadRepository {
  private final Store store;
  public IdentidadRepositoryAdapter(Store store){this.store=store;}
  public Map<String,Object> get(String table,String id){return store.get(table,id);}
  public List<Map<String,Object>> list(String table,String field,Object value){return store.list(table,field,value);}
  public Map<String,Object> create(String table,Map<String,Object> data){return store.create(table,data);}
  public Map<String,Object> update(String table,String id,Map<String,Object> data){return store.update(table,id,data);}
  public Map<String,Object> updateTrusted(String table,String id,Map<String,Object> data){return store.updateTrusted(table,id,data);}
  public void lockAgenda(String agendaId){store.lockAgenda(agendaId);}
  public boolean overlaps(String agendaId,Object from,Object to){return store.overlaps(agendaId,from,to);}
  public boolean offered(String publicationId,Object from,Object to){return store.offered(publicationId,from,to);}
  public List<Map<String,Object>> searchPublications(Map<String,String> filters){return store.searchPublications(filters);}
}
