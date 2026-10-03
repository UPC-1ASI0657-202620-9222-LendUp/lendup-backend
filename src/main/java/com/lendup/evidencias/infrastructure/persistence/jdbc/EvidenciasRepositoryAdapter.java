package com.lendup.evidencias.infrastructure.persistence.jdbc;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import com.lendup.shared.Store;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
@Repository
public class EvidenciasRepositoryAdapter implements EvidenciasRepository {
  private final Store store;
  public EvidenciasRepositoryAdapter(Store store){this.store=store;}
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
