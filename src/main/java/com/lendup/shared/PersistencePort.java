package com.lendup.shared;
import java.util.List;
import java.util.Map;
/** Shared low-level operations; each BC has a separate business repository port. */
public interface PersistencePort {
  Map<String,Object> get(String table,String id);
  List<Map<String,Object>> list(String table,String field,Object value);
  Map<String,Object> create(String table,Map<String,Object> data);
  Map<String,Object> update(String table,String id,Map<String,Object> data);
  Map<String,Object> updateTrusted(String table,String id,Map<String,Object> data);
  void lockAgenda(String agendaId);
  boolean overlaps(String agendaId,Object from,Object to);
  boolean offered(String publicationId,Object from,Object to);
  List<Map<String,Object>> searchPublications(Map<String,String> filters);
}
