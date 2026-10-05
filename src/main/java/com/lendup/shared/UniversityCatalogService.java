package com.lendup.shared;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class UniversityCatalogService {
  public record University(String id,String shortName,String name,List<String> emailDomains) {}
  private final NamedParameterJdbcTemplate db;
  public UniversityCatalogService(NamedParameterJdbcTemplate db){this.db=db;}
  public List<University> list(){
    var rows=db.queryForList("SELECT u.codigo,u.nombre,u.sigla,d.dominio FROM universidades u JOIN dominios_universidad d ON d.universidad_codigo=u.codigo WHERE u.activa=TRUE AND d.activo=TRUE ORDER BY u.nombre,d.dominio",Map.of());
    var result=new LinkedHashMap<String,University>();
    for(var row:rows){var id=row.get("codigo").toString();var item=result.computeIfAbsent(id,key->new University(key,row.get("sigla").toString(),row.get("nombre").toString(),new ArrayList<>()));item.emailDomains().add(row.get("dominio").toString());}
    return new ArrayList<>(result.values());
  }
  public University resolve(String email){return resolve(email,list());}
  static University resolve(String email,List<University> universities){
    var normalized=Objects.toString(email,"").trim().toLowerCase(Locale.ROOT);
    int at=normalized.indexOf('@');
    if(at<1||at!=normalized.lastIndexOf('@')||at==normalized.length()-1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Correo institucional inválido");
    var domain=normalized.substring(at+1);
    return universities.stream().filter(u->u.emailDomains().contains(domain)).findFirst()
      .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Todavía no reconocemos el dominio de tu universidad. Solicita que lo añadamos."));
  }
  public static String campus(Object value){
    var campus=Objects.toString(value,"").trim();
    if(campus.length()>150)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La sede admite hasta 150 caracteres");
    return campus;
  }
}
