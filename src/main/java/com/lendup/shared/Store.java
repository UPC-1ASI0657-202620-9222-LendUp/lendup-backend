package com.lendup.shared;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class Store implements PersistencePort {
  private final NamedParameterJdbcTemplate db;
  private final SchemaCatalog schema;
  public Store(NamedParameterJdbcTemplate db,SchemaCatalog schema){this.db=db;this.schema=schema;}
  private Object toSqlValue(String key,Object val){
    if(!(val instanceof String str))return val;
    if(key.equals("desde")||key.equals("hasta")||key.endsWith("_en")||key.equals("programada_para")){
      try{return str.endsWith("Z")||str.matches(".*[+-][0-9]{2}:[0-9]{2}$")
        ? OffsetDateTime.parse(str).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
        : LocalDateTime.parse(str);}
      catch(DateTimeParseException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fecha ISO inválida: "+key);}
    }return val;
  }
  private void check(String t){if(schema.columns(t).isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tabla desconocida");}
  public Map<String,Object> get(String t,String id){check(t);
    var rows=db.queryForList("SELECT * FROM `"+t+"` WHERE id=:id",Map.of("id",id));
    if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No encontrado");
    return rows.getFirst();
  }
  public List<Map<String,Object>> list(String t,String field,Object value){check(t);
    if(field!=null && value!=null && schema.columns(t).contains(field))return db.queryForList("SELECT * FROM `"+t+"` WHERE `"+field+"`=:v LIMIT 200",Map.of("v",value));
    return db.queryForList("SELECT * FROM `"+t+"` LIMIT 200",Map.of());
  }
  public void lockAgenda(String agendaId){
    db.queryForList("SELECT id FROM agendas_objeto WHERE id=:id FOR UPDATE",Map.of("id",agendaId));
  }
  public boolean overlaps(String agendaId,Object from,Object to){
    var p=Map.of("agenda",agendaId,"from",from,"to",to);
    return !db.queryForList("SELECT id FROM reservas WHERE agenda_id=:agenda AND estado='CONFIRMADA' AND desde < :to AND hasta > :from LIMIT 1",p).isEmpty();
  }
  public boolean offered(String publicationId,Object from,Object to){
    var p=Map.of("publication",publicationId,"from",toSqlValue("desde",from),"to",toSqlValue("hasta",to));
    return !db.queryForList("SELECT id FROM disponibilidades_publicacion WHERE publicacion_id=:publication AND desde<=:from AND hasta>=:to LIMIT 1",p).isEmpty();
  }
  public List<Map<String,Object>> availability(String publicationId){
    var sql="""
      SELECT d.id,d.desde,d.hasta,'DISPONIBLE' AS estado
      FROM disponibilidades_publicacion d
      WHERE d.publicacion_id=:publication
      UNION ALL
      SELECT NULL AS id,r.desde,r.hasta,'RESERVADA' AS estado
      FROM reservas r
      JOIN agendas_objeto a ON a.id=r.agenda_id
      WHERE a.publicacion_id=:publication AND r.estado='CONFIRMADA'
      ORDER BY desde
      """;
    return db.queryForList(sql,Map.of("publication",publicationId));
  }
  public List<Map<String,Object>> searchPublications(Map<String,String> filters){
    var p=new LinkedHashMap<String,Object>();var sql=new StringBuilder("SELECT p.* FROM publicaciones p WHERE p.estado='ACTIVA'");
    if(filters.containsKey("nombre")&&!filters.get("nombre").isBlank()){
      sql.append(" AND LOWER(p.titulo) LIKE :nombre");p.put("nombre","%"+filters.get("nombre").toLowerCase()+"%");
    }
    if(filters.containsKey("categoria")&&!filters.get("categoria").isBlank()){
      sql.append(" AND p.categoria_id=:categoria");p.put("categoria",filters.get("categoria"));
    }
    if(filters.containsKey("campus")&&!filters.get("campus").isBlank()){
      sql.append(" AND p.campus=:campus");p.put("campus",filters.get("campus"));
    }
    if(filters.containsKey("desde")&&filters.containsKey("hasta")){
      p.put("desde",toSqlValue("desde",filters.get("desde")));p.put("hasta",toSqlValue("hasta",filters.get("hasta")));
      sql.append(" AND EXISTS (SELECT 1 FROM disponibilidades_publicacion d WHERE d.publicacion_id=p.id AND d.desde<=:desde AND d.hasta>=:hasta)");
      sql.append(" AND NOT EXISTS (SELECT 1 FROM reservas r JOIN agendas_objeto a ON r.agenda_id=a.id WHERE a.publicacion_id=p.id AND r.estado='CONFIRMADA' AND r.desde<:hasta AND r.hasta>:desde)");
    }
    sql.append(" ORDER BY p.creado_en DESC LIMIT 200");return db.queryForList(sql.toString(),p);
  }
  @Transactional
  public Map<String,Object> create(String t,Map<String,Object> input){check(t);
    var p=new LinkedHashMap<String,Object>();p.put("id",UUID.randomUUID().toString());
    input.forEach((key,val)->{if(schema.columns(t).contains(key)&&!key.equals("id")&&val!=null)p.put(key,toSqlValue(key,val));});
    var missing=new ArrayList<>(schema.mandatory(t));missing.removeAll(p.keySet());
    if(!missing.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Faltan campos: "+missing);
    String cols=String.join(",",p.keySet().stream().map(x->"`"+x+"`").toList());
    String vals=String.join(",",p.keySet().stream().map(x->":"+x).toList());
    db.update("INSERT INTO `"+t+"` ("+cols+") VALUES ("+vals+")",p);
    return get(t,(String)p.get("id"));
  }
  @Transactional
  public Map<String,Object> update(String t,String id,Map<String,Object> input){return modify(t,id,input,false);}
  @Transactional
  public Map<String,Object> updateTrusted(String t,String id,Map<String,Object> input){return modify(t,id,input,true);}
  private Map<String,Object> modify(String t,String id,Map<String,Object> input,boolean allowReferences){get(t,id);
    var p=new LinkedHashMap<String,Object>();p.put("id",id);
    input.forEach((key,val)->{if(schema.columns(t).contains(key)&&!key.equals("id")&&(allowReferences||!key.endsWith("_id")))p.put(key,toSqlValue(key,val));});
    if(p.size()>1){var set=new ArrayList<String>();for(String k:p.keySet())if(!k.equals("id"))set.add("`"+k+"`=:"+k);
      db.update("UPDATE `"+t+"` SET "+String.join(",",set)+" WHERE id=:id",p);}
    return get(t,id);
  }
}
