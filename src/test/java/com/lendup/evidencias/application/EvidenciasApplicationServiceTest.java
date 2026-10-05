package com.lendup.evidencias.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import com.lendup.evidencias.domain.repositories.EvidenciasRepository;
import com.lendup.shared.CloudinaryImageClient;
import tools.jackson.databind.json.JsonMapper;

class EvidenciasApplicationServiceTest {
  final EvidenciasRepository store=mock(EvidenciasRepository.class);
  final CloudinaryImageClient cloud=mock(CloudinaryImageClient.class);
  final EvidenciasApplicationService service=new EvidenciasApplicationService(store,JsonMapper.builder().build(),cloud);
  final Map<String,Object> loan=new LinkedHashMap<>(Map.of("id","loan","prestatario_usuario_id","borrower","prestamista_usuario_id","lender","estado","RESERVADO"));
  @BeforeEach void setup(){
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("firebase",null,List.of()));
    when(store.list("usuarios","firebase_uid","firebase")).thenReturn(List.of(Map.of("id","lender")));
    when(store.get("prestamos","loan")).thenReturn(loan);
    when(store.list(eq("evidencias"),eq("cloudinary_public_id"),any())).thenReturn(List.of());
    when(store.list("evidencias","prestamo_id","loan")).thenReturn(List.of());
    when(store.create(eq("evidencias"),anyMap())).thenAnswer(i->{var row=new LinkedHashMap<String,Object>(i.getArgument(1));row.put("id","evidence");return row;});
  }
  @AfterEach void clean(){SecurityContextHolder.clearContext();}

  @Test void detectsSupportedPhotoAndVideoSignatures(){
    assertEquals("image/png",EvidenciasApplicationService.mediaMime(new byte[]{(byte)137,80,78,71,13,10,26,10,1},"image/png"));
    assertEquals("video/mp4",EvidenciasApplicationService.mediaMime(new byte[]{0,0,0,24,'f','t','y','p','i','s','o','m'},"video/mp4"));
    assertEquals("video/quicktime",EvidenciasApplicationService.mediaMime(new byte[]{0,0,0,24,'f','t','y','p','q','t',' ',' '},"video/quicktime"));
    assertEquals("video/webm",EvidenciasApplicationService.mediaMime(new byte[]{0x1A,0x45,(byte)0xDF,(byte)0xA3,1},"video/webm"));
  }

  @Test void rejectsEmptyForgedAndOversizedFiles(){
    assertThrows(ResponseStatusException.class,()->EvidenciasApplicationService.mediaMime(new byte[0],"image/png"));
    assertThrows(ResponseStatusException.class,()->EvidenciasApplicationService.mediaMime("<svg/>".getBytes(),"image/png"));
    assertThrows(ResponseStatusException.class,()->EvidenciasApplicationService.mediaMime(new byte[EvidenciasApplicationService.MAX_BYTES+1],"video/mp4"));
  }

  @Test void uploadsInitialEvidenceAndStoresTheCloudinaryReference(){
    var bytes=new byte[]{(byte)137,80,78,71,13,10,26,10,1};
    var file=new MockMultipartFile("file","condition.png","image/png",bytes);
    when(cloud.uploadMedia(any(byte[].class),eq("image/png"),anyString())).thenAnswer(i->new CloudinaryImageClient.Asset(i.getArgument(2),"https://res.cloudinary.com/test/evidence.png"));
    var saved=service.upload("loan","ENTREGA",file,"a0000000-0000-4000-8000-000000000001");
    assertEquals("FOTO",saved.get("tipo"));assertEquals("COMPLETADA",saved.get("estado_integracion"));
    assertEquals("https://res.cloudinary.com/test/evidence.png",saved.get("url"));
    verify(cloud).requireConfigured();verify(cloud).uploadMedia(any(byte[].class),eq("image/png"),contains("lendup/evidencias/loan/entrega/"));
  }
}
