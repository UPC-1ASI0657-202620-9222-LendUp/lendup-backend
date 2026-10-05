package com.lendup.shared;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class StoreCatalogFiltersTest {
  private final NamedParameterJdbcTemplate db=mock(NamedParameterJdbcTemplate.class);
  private final Store store=new Store(db,new SchemaCatalog());

  @Test void combinesAndNormalizesAllCatalogFilters(){
    when(db.queryForList(anyString(),anyMap())).thenReturn(List.of());
    var filters=new LinkedHashMap<String,String>();
    filters.put("nombre","  Cámara   científica ");
    filters.put("categoria","CAMERAS");
    filters.put("universidad"," UPC ");
    filters.put("campus"," Monterrico ");
    filters.put("ubicacion"," Santiago de Surco ");
    filters.put("desde","2026-10-06T12:00:00");
    filters.put("hasta","2026-10-08T12:00:00");

    store.searchPublications(filters);

    var sql=ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    var params=(ArgumentCaptor<Map<String,?>>)(ArgumentCaptor<?>)ArgumentCaptor.forClass(Map.class);
    verify(db).queryForList(sql.capture(),params.capture());
    assertTrue(sql.getValue().contains("p.descripcion"));
    assertTrue(sql.getValue().contains("c.codigo"));
    assertTrue(sql.getValue().contains("LOWER(p.universidad)=LOWER(:universidad)"));
    assertTrue(sql.getValue().contains("LOWER(p.campus)=LOWER(:campus)"));
    assertTrue(sql.getValue().contains("LOCATE(:ubicacion,LOWER(p.ubicacion))>0"));
    assertTrue(sql.getValue().contains("EXISTS (SELECT 1 FROM disponibilidades_publicacion"));
    assertTrue(sql.getValue().contains("NOT EXISTS (SELECT 1 FROM reservas"));
    assertEquals("cámara",params.getValue().get("nombre0"));
    assertEquals("científica",params.getValue().get("nombre1"));
    assertEquals("CAMERAS",params.getValue().get("categoria"));
    assertEquals("UPC",params.getValue().get("universidad"));
    assertEquals("Monterrico",params.getValue().get("campus"));
    assertEquals("santiago de surco",params.getValue().get("ubicacion"));
    assertEquals(LocalDateTime.parse("2026-10-06T12:00:00"),params.getValue().get("desde"));
  }

  @Test void rejectsIncompleteOrReversedPeriodsBeforeQuerying(){
    var incomplete=assertThrows(ResponseStatusException.class,()->store.searchPublications(Map.of("desde","2026-10-06T12:00:00")));
    assertEquals(400,incomplete.getStatusCode().value());
    var reversed=assertThrows(ResponseStatusException.class,()->store.searchPublications(Map.of(
      "desde","2026-10-08T12:00:00","hasta","2026-10-06T12:00:00")));
    assertEquals(400,reversed.getStatusCode().value());
    verifyNoInteractions(db);
  }

  @Test void loadsAvailabilityForAResultPageInOneQueryAndGroupsIt(){
    var available=new LinkedHashMap<String,Object>();
    available.put("publicacion_id","p1");available.put("id","d1");
    available.put("desde",LocalDateTime.parse("2026-10-06T12:00:00"));
    available.put("hasta",LocalDateTime.parse("2026-10-08T12:00:00"));available.put("estado","DISPONIBLE");
    var reserved=new LinkedHashMap<String,Object>();
    reserved.put("publicacion_id","p2");reserved.put("id",null);
    reserved.put("desde",LocalDateTime.parse("2026-10-09T12:00:00"));
    reserved.put("hasta",LocalDateTime.parse("2026-10-10T12:00:00"));reserved.put("estado","RESERVADA");
    when(db.queryForList(anyString(),anyMap())).thenReturn(List.of(available,reserved));

    var result=store.availabilityForPublications(List.of("p1","p2"));

    assertEquals("d1",result.get("p1").getFirst().get("id"));
    assertEquals("RESERVADA",result.get("p2").getFirst().get("estado"));
    assertFalse(result.get("p1").getFirst().containsKey("publicacion_id"));
    verify(db,times(1)).queryForList(anyString(),anyMap());
  }
}
