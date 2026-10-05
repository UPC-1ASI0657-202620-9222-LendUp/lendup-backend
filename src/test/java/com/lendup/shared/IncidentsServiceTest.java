package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import tools.jackson.databind.json.JsonMapper;

class IncidentsServiceTest {
  final EvidenciasRepository store=mock(EvidenciasRepository.class);
  final NamedParameterJdbcTemplate db=mock(NamedParameterJdbcTemplate.class);
  final CloudinaryImageClient cloud=mock(CloudinaryImageClient.class);
  final IncidentsService service=new IncidentsService(store,JsonMapper.builder().build(),db,cloud);
  Map<String,Object> loan,incident;
  @BeforeEach void setup() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("firebase",null,List.of()));
    when(store.list("usuarios","firebase_uid","firebase")).thenReturn(List.of(Map.of("id","borrower","rol_id","role")));
    when(store.get("roles","role")).thenReturn(Map.of("codigo","ESTUDIANTE"));
    loan=new LinkedHashMap<>(Map.of("id","loan","reserva_id","reservation","prestatario_usuario_id","borrower","prestamista_usuario_id","lender","estado","ACTIVO","moneda","PEN","incidencias_pendientes",0));
    incident=new LinkedHashMap<>(Map.of("id","incident","prestamo_id","loan","reportada_por_usuario_id","lender","estado","PENDIENTE","tipo","DANIO","descripcion","Descripción detallada de daños"));
    when(store.get("prestamos","loan")).thenAnswer(i->loan);
    when(store.get("incidencias","incident")).thenAnswer(i->incident);
    when(store.get("reservas","reservation")).thenReturn(Map.of("garantia_monetaria_acordada",new BigDecimal("100.00")));
    when(db.queryForList(anyString(),anyMap())).thenAnswer(i->{String sql=i.getArgument(0);if(sql.contains("FROM prestamos WHERE"))return List.of(loan);if(sql.contains("COALESCE(SUM"))return List.of(Map.of("total",BigDecimal.ZERO));return List.of();});
    when(store.create(anyString(),anyMap())).thenAnswer(i->{var row=new LinkedHashMap<String,Object>(i.getArgument(1));row.put("id","incident");return row;});
    when(store.update(eq("incidencias"),anyString(),anyMap())).thenAnswer(i->{incident.putAll(i.getArgument(2));return incident;});
    when(store.updateTrusted(eq("incidencias"),anyString(),anyMap())).thenAnswer(i->{incident.putAll(i.getArgument(2));return incident;});
  }
  @AfterEach void clean(){SecurityContextHolder.clearContext();}
  void administrator(){when(store.get("roles","role")).thenReturn(Map.of("codigo","ADMINISTRADOR"));}
  Map<String,Object> report(){return Map.of("prestamo_id","loan","tipo","DANIO","descripcion","Descripción detallada de daños","observaciones",List.of("Observación de la pantalla"));}
  Map<String,Object> resolution(String decision,String amount){return Map.of("justificacion_resolucion","Justificación detallada de la decisión","decision_garantia",decision,"monto_garantia_afectado",new BigDecimal(amount),"saldo_garantia_previsto",new BigDecimal("9999"));}
  @Test void studentListIsScopedBeforeLoadingRows(){service.list(false,Map.of());verify(db).queryForList(contains("p.prestatario_usuario_id=:me OR p.prestamista_usuario_id=:me"),eq(Map.of("me","borrower")));}
  @Test void strangersCannotReadReportOrRespond(){loan.put("prestatario_usuario_id","stranger");assertEquals(403,assertThrows(ResponseStatusException.class,()->service.detail("incident")).getStatusCode().value());assertThrows(ResponseStatusException.class,()->service.report(report(),List.of()));assertThrows(ResponseStatusException.class,()->service.statement("incident",Map.of("contenido","Descargo detallado del problema")));}
  @Test void studentCannotReviewNoteResolveOrListAdmin(){assertThrows(ResponseStatusException.class,()->service.review("incident"));assertThrows(ResponseStatusException.class,()->service.note("incident",Map.of("contenido","Nota")));assertThrows(ResponseStatusException.class,()->service.resolve("incident",resolution("SIN_AFECTACION","0")));assertThrows(ResponseStatusException.class,()->service.list(true,Map.of()));}
  @Test void reportsPersistObservationsIncrementPendingAndNotifyBoth(){service.report(report(),List.of());verify(store).create(eq("evidencias"),argThat(row->row.get("incidencia_id").equals("incident")&&row.get("observacion").equals("Observación de la pantalla")));verify(db).update(contains("incidencias_pendientes+1"),anyMap());verify(store,times(2)).create(eq("notificaciones"),anyMap());}
  @Test void rejectsInvalidTypeDescriptionAndClosedLoans(){assertThrows(ResponseStatusException.class,()->service.report(Map.of("prestamo_id","loan","tipo","INVALID","descripcion","Descripción detallada de daños"),List.of()));assertThrows(ResponseStatusException.class,()->service.report(Map.of("prestamo_id","loan","tipo","DANIO","descripcion","corta"),List.of()));loan.put("estado","FINALIZADO");assertThrows(ResponseStatusException.class,()->service.report(report(),List.of()));verify(store,never()).create(eq("incidencias"),anyMap());}
  @Test void invalidPhotoRejectedBeforeReportIsCreated(){var file=new MockMultipartFile("fotos","fake.png","image/png","<svg/>".getBytes());assertThrows(ResponseStatusException.class,()->service.report(report(),List.of(file)));verify(store,never()).create(eq("incidencias"),anyMap());verifyNoInteractions(cloud);}
  @Test void photoIsLinkedAndCloudAssetCleanedOnDatabaseFailure(){var file=new MockMultipartFile("fotos","photo.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,1});when(cloud.upload(any(),anyString(),anyString())).thenReturn(new CloudinaryImageClient.Asset("asset","https://res.cloudinary.com/test/photo.png"));when(store.create(eq("evidencias"),argThat(row->"FOTO".equals(row.get("tipo"))))).thenThrow(new IllegalStateException("db"));assertThrows(IllegalStateException.class,()->service.report(report(),List.of(file)));verify(cloud).delete("asset");}
  @Test void counterpartCanRespondOnceButReporterCannot(){service.statement("incident",Map.of("contenido","Descargo detallado del problema"));verify(store).create(eq("evidencias"),argThat(row->"borrower".equals(row.get("registrada_por_usuario_id"))));incident.put("reportada_por_usuario_id","borrower");assertThrows(ResponseStatusException.class,()->service.statement("incident",Map.of("contenido","Descargo detallado del problema")));}
  @Test void repeatedStatementRejected(){when(db.queryForList(contains("registrada_por_usuario_id=:me"),anyMap())).thenReturn(List.of(Map.of("id","statement")));assertEquals(409,assertThrows(ResponseStatusException.class,()->service.statement("incident",Map.of("contenido","Descargo detallado del problema"))).getStatusCode().value());}
  @Test void reviewEnablesNotesAndResolvedCasesAreImmutable(){administrator();assertThrows(ResponseStatusException.class,()->service.note("incident",Map.of("contenido","Nota")));service.review("incident");assertEquals("EN_REVISION",incident.get("estado"));service.note("incident",Map.of("contenido","Nota administrativa"));verify(store).create(eq("observaciones_incidencia"),argThat(row->row.get("contenido").equals("Nota administrativa")));incident.put("estado","RESUELTA");assertThrows(ResponseStatusException.class,()->service.review("incident"));assertThrows(ResponseStatusException.class,()->service.note("incident",Map.of("contenido","Otra nota")));assertThrows(ResponseStatusException.class,()->service.statement("incident",Map.of("contenido","Descargo detallado del problema")));}
  @Test void resolutionRequiresReviewAndConsistentDecision(){administrator();assertThrows(ResponseStatusException.class,()->service.resolve("incident",resolution("SIN_AFECTACION","0")));incident.put("estado","EN_REVISION");for(var values:List.of(new String[]{"SIN_AFECTACION","1"},new String[]{"AFECTACION_PARCIAL","0"},new String[]{"AFECTACION_PARCIAL","100"},new String[]{"AFECTACION_TOTAL","99"},new String[]{"OTHER","0"},new String[]{"AFECTACION_PARCIAL","-1"},new String[]{"AFECTACION_PARCIAL","0.001"}))assertThrows(ResponseStatusException.class,()->service.resolve("incident",resolution(values[0],values[1])));verify(store,never()).updateTrusted(eq("incidencias"),anyString(),anyMap());}
  @Test void resolutionUsesLockedLoanAndServerComputedBalance(){administrator();incident.put("estado","EN_REVISION");when(db.queryForList(contains("COALESCE(SUM"),anyMap())).thenReturn(List.of(Map.of("total",new BigDecimal("30"))));service.resolve("incident",resolution("AFECTACION_PARCIAL","20"));assertEquals(new BigDecimal("50.00"),incident.get("saldo_garantia_previsto"));verify(db).queryForList(contains("FOR UPDATE"),eq(Map.of("id","loan")));verify(db).update(contains("incidencias_pendientes=(SELECT COUNT"),anyMap());assertEquals("RESUELTA",incident.get("estado"));assertThrows(ResponseStatusException.class,()->service.resolve("incident",resolution("AFECTACION_PARCIAL","20")));}
  @Test void cannotExceedBalanceAfterPreviousResolution(){administrator();incident.put("estado","EN_REVISION");when(db.queryForList(contains("COALESCE(SUM"),anyMap())).thenReturn(List.of(Map.of("total",new BigDecimal("90"))));assertThrows(ResponseStatusException.class,()->service.resolve("incident",resolution("AFECTACION_PARCIAL","20")));service.resolve("incident",resolution("AFECTACION_TOTAL","10"));assertEquals(new BigDecimal("0.00"),incident.get("saldo_garantia_previsto"));}
  @Test void detailReturnsEvidenceStatementNotesAndLoan(){when(db.queryForList(contains("AND registrada_por_usuario_id<>"),anyMap())).thenReturn(List.of(Map.of("observacion","Descargo guardado","registrada_en","2026-10-05T10:00:00")));var detail=service.detail("incident");assertEquals("Descargo guardado",detail.get("descargo"));assertTrue(detail.containsKey("evidencias"));assertTrue(detail.containsKey("observaciones"));assertTrue(detail.containsKey("prestamo"));assertEquals(new BigDecimal("100.00"),detail.get("garantia_monetaria_acordada"));}
}
