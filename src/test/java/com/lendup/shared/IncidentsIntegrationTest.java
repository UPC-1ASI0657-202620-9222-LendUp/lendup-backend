package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import com.lendup.evidencias.application.EvidenciasApplicationService;
import com.lendup.evidencias.interfaces.rest.EvidenciasController;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;

class IncidentsIntegrationTest {
  NamedParameterJdbcTemplate db;
  MockMvc mvc;
  IncidentsService service;
  final String reportId="a0000000-0000-4000-8000-000000000001";
  static class Repository extends Store implements EvidenciasRepository {
    Repository(NamedParameterJdbcTemplate db){super(db,new SchemaCatalog());}
  }
  @BeforeEach void setup() {
    var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    db=new NamedParameterJdbcTemplate(ds);var jdbc=db.getJdbcTemplate();
    jdbc.execute("CREATE TABLE roles(id VARCHAR(36) PRIMARY KEY,codigo VARCHAR(30),descripcion VARCHAR(100))");
    jdbc.execute("CREATE TABLE usuarios(id VARCHAR(36) PRIMARY KEY,firebase_uid VARCHAR(100),rol_id VARCHAR(36))");
    jdbc.execute("CREATE TABLE reservas(id VARCHAR(36) PRIMARY KEY,garantia_monetaria_acordada DECIMAL(12,2),tarifa_diaria_acordada DECIMAL(12,2))");
    jdbc.execute("CREATE TABLE prestamos(id VARCHAR(36) PRIMARY KEY,reserva_id VARCHAR(36),prestatario_usuario_id VARCHAR(36),prestamista_usuario_id VARCHAR(36),estado VARCHAR(32),moneda VARCHAR(3),incidencias_pendientes INT DEFAULT 0)");
    jdbc.execute("CREATE TABLE incidencias(id VARCHAR(36) PRIMARY KEY,prestamo_id VARCHAR(36),reportada_por_usuario_id VARCHAR(36),tipo VARCHAR(24),descripcion TEXT,estado VARCHAR(20),reportada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,revision_iniciada_en TIMESTAMP,resuelta_en TIMESTAMP,resuelta_por_usuario_id VARCHAR(36),justificacion_resolucion TEXT,decision_garantia VARCHAR(24),monto_garantia_afectado DECIMAL(12,2),saldo_garantia_previsto DECIMAL(12,2),moneda VARCHAR(3),actualizada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    jdbc.execute("CREATE TABLE evidencias(id VARCHAR(36) PRIMARY KEY,prestamo_id VARCHAR(36),incidencia_id VARCHAR(36),registrada_por_usuario_id VARCHAR(36),etapa VARCHAR(20),tipo VARCHAR(20),url TEXT,cloudinary_public_id VARCHAR(255),observacion TEXT,registrada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,estado_integracion VARCHAR(24))");
    jdbc.execute("CREATE TABLE observaciones_incidencia(id VARCHAR(36) PRIMARY KEY,incidencia_id VARCHAR(36),administrador_usuario_id VARCHAR(36),contenido TEXT,registrada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    jdbc.execute("CREATE TABLE garantias(id VARCHAR(36) PRIMARY KEY,prestamo_id VARCHAR(36),monto_afectado DECIMAL(12,2),actualizada_en TIMESTAMP)");
    jdbc.execute("CREATE TABLE notificaciones(id VARCHAR(36) PRIMARY KEY,destinatario_usuario_id VARCHAR(36),clave_notificacion VARCHAR(150) UNIQUE,tipo VARCHAR(32),origen_tipo VARCHAR(32),origen_id VARCHAR(36),evento_origen_id VARCHAR(36),titulo VARCHAR(200),mensaje TEXT,estado VARCHAR(24),disponible_en TIMESTAMP)");
    jdbc.update("INSERT INTO roles VALUES ('student','ESTUDIANTE','Student'),('admin','ADMINISTRADOR','Admin')");
    jdbc.update("INSERT INTO usuarios VALUES ('borrower','borrower','student'),('lender','lender','student'),('outsider','outsider','student'),('admin','admin','admin')");
    jdbc.update("INSERT INTO reservas VALUES ('reservation',100,10)");
    jdbc.update("INSERT INTO prestamos VALUES ('loan','reservation','borrower','lender','ACTIVO','PEN',0)");
    jdbc.update("INSERT INTO garantias VALUES ('guarantee','loan',0,CURRENT_TIMESTAMP)");
    var repository=new Repository(db);var json=JsonMapper.builder().build();
    var target=new IncidentsService(repository,json,db,mock(CloudinaryImageClient.class));
    var proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);
    proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),new AnnotationTransactionAttributeSource()));
    service=(IncidentsService)proxy.getProxy();
    mvc=MockMvcBuilders.standaloneSetup(new EvidenciasController(new EvidenciasApplicationService(repository,json,mock(CloudinaryImageClient.class)),json,service)).build();
    login("lender");
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  void login(String uid){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(uid,null,List.of()));}
  String report(){return "{\"prestamo_id\":\"loan\",\"tipo\":\"DANIO\",\"descripcion\":\"La pantalla tiene una rotura visible\",\"observaciones\":[\"Se observa daño en la esquina\"],\"clave_idempotencia\":\""+reportId+"\"}";}
  int pending(){return db.getJdbcTemplate().queryForObject("SELECT incidencias_pendientes FROM prestamos WHERE id='loan'",Integer.class);}
  @Test void fullHttpLifecyclePersistsAndIsVisibleToBothParticipants() throws Exception {
    mvc.perform(post("/api/v1/incidencias").contentType("application/json").content(report())).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(reportId)).andExpect(jsonPath("$.evidencias[0].observacion").value("Se observa daño en la esquina"));
    assertEquals(1,pending());
    mvc.perform(post("/api/v1/incidencias").contentType("application/json").content(report())).andExpect(status().isCreated());assertEquals(1,pending());
    login("borrower");mvc.perform(get("/api/v1/incidencias")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(reportId));
    mvc.perform(post("/api/v1/incidencias/"+reportId+"/descargo").contentType("application/json").content("{\"contenido\":\"La pantalla ya presentaba este problema\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.descargo").value("La pantalla ya presentaba este problema"));
    mvc.perform(post("/api/v1/incidencias/"+reportId+"/descargo").contentType("application/json").content("{\"contenido\":\"La pantalla ya presentaba este problema\"}")).andExpect(status().isConflict());
    login("outsider");mvc.perform(get("/api/v1/incidencias")).andExpect(status().isOk()).andExpect(content().json("[]"));mvc.perform(get("/api/v1/incidencias/"+reportId)).andExpect(status().isForbidden());
    login("admin");mvc.perform(get("/api/v1/admin/incidencias")).andExpect(status().isOk()).andExpect(jsonPath("$[0].prestamo.id").value("loan"));
    mvc.perform(post("/api/v1/admin/incidencias/"+reportId+"/revision")).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION"));
    mvc.perform(post("/api/v1/admin/incidencias/"+reportId+"/observaciones").contentType("application/json").content("{\"contenido\":\"Se revisó la evidencia aportada\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.observaciones[0].contenido").value("Se revisó la evidencia aportada"));
    var resolution="{\"justificacion_resolucion\":\"El daño está documentado y corresponde una afectación parcial\",\"decision_garantia\":\"AFECTACION_PARCIAL\",\"monto_garantia_afectado\":20,\"saldo_garantia_previsto\":9999,\"moneda\":\"USD\"}";
    mvc.perform(post("/api/v1/admin/incidencias/"+reportId+"/resolucion").contentType("application/json").content(resolution)).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RESUELTA")).andExpect(jsonPath("$.saldo_garantia_previsto").value(80)).andExpect(jsonPath("$.moneda").value("PEN"));
    assertEquals(0,pending());assertEquals(new BigDecimal("20.00"),db.getJdbcTemplate().queryForObject("SELECT monto_afectado FROM garantias",BigDecimal.class));
    mvc.perform(post("/api/v1/admin/incidencias/"+reportId+"/resolucion").contentType("application/json").content(resolution)).andExpect(status().isConflict());
    login("borrower");mvc.perform(get("/api/v1/incidencias/"+reportId)).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RESUELTA"));
  }
  @Test void multipartObservationsAreSavedAndInvalidPhotoRollsBackEntireReport() throws Exception {
    var payload=new MockMultipartFile("reporte","","application/json",report().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var invalid=new MockMultipartFile("fotos","bad.png","image/png","<svg/>".getBytes());
    mvc.perform(multipart("/api/v1/incidencias").file(payload).file(invalid)).andExpect(status().isBadRequest());assertEquals(0,pending());assertEquals(0,db.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM incidencias",Integer.class));
    mvc.perform(multipart("/api/v1/incidencias").file(payload)).andExpect(status().isCreated()).andExpect(jsonPath("$.evidencias[0].observacion").value("Se observa daño en la esquina"));
  }
  @Test void concurrentResolutionsCannotSpendTheSameGuaranteeTwice() throws Exception {
    var secondId="a0000000-0000-4000-8000-000000000002";
    mvc.perform(post("/api/v1/incidencias").contentType("application/json").content(report())).andExpect(status().isCreated());
    mvc.perform(post("/api/v1/incidencias").contentType("application/json").content(report().replace(reportId,secondId))).andExpect(status().isCreated());
    login("admin");service.review(reportId);service.review(secondId);
    var gate=new java.util.concurrent.CountDownLatch(1);
    try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var futures=new ArrayList<java.util.concurrent.Future<Boolean>>();
      for(var id:List.of(reportId,secondId)) futures.add(executor.submit(()->{
        login("admin");gate.await();
        try {service.resolve(id,Map.of("justificacion_resolucion","Daño confirmado mediante las evidencias aportadas","decision_garantia","AFECTACION_PARCIAL","monto_garantia_afectado",new BigDecimal("60.00")));return true;}
        catch(org.springframework.web.server.ResponseStatusException expected){return false;}
        finally {SecurityContextHolder.clearContext();}
      }));
      gate.countDown();int succeeded=0;for(var result:futures) if(result.get(10,java.util.concurrent.TimeUnit.SECONDS)) succeeded++;
      assertEquals(1,succeeded);assertEquals(1,pending());
      assertEquals(new BigDecimal("60.00"),db.getJdbcTemplate().queryForObject("SELECT SUM(monto_garantia_afectado) FROM incidencias",BigDecimal.class));
    }
  }

}
