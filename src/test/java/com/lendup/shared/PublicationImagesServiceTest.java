package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
class PublicationImagesServiceTest {
  private final NamedParameterJdbcTemplate db=mock(NamedParameterJdbcTemplate.class);
  private final CloudinaryImageClient cloud=mock(CloudinaryImageClient.class);
  private final PublicationImagesService service=new PublicationImagesService(db,cloud);
  private final MockMultipartFile file=new MockMultipartFile("file","photo.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,1});
  @BeforeEach void setup(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("student",null,List.of()));}
  @AfterEach void clean(){SecurityContextHolder.clearContext();}
  private void owned(){when(db.queryForList(contains("SELECT p.id"),anyMap())).thenReturn(List.of(Map.of("id","publication")));}
  @Test void rejectsForgedTypesEmptyAndOversize() throws Exception {
    for(var bytes:List.of(new byte[0],new byte[PublicationImagesService.MAX_BYTES+1],"<svg/>".getBytes()))assertThrows(ResponseStatusException.class,()->PublicationImagesService.mime(bytes));
    assertEquals("image/png",PublicationImagesService.mime(file.getBytes()));
  }
  @Test void anotherUserCannotUploadOrDelete(){
    assertEquals(403,assertThrows(ResponseStatusException.class,()->service.upload("publication",file,null)).getStatusCode().value());
    assertThrows(ResponseStatusException.class,()->service.delete("publication","image"));verifyNoInteractions(cloud);
  }
  @Test void stopsAtSixBeforeCallingCloudinary(){owned();when(db.queryForList(startsWith("SELECT orden"),anyMap())).thenReturn(java.util.Collections.nCopies(6,Map.of("orden",0)));
    assertEquals(409,assertThrows(ResponseStatusException.class,()->service.upload("publication",file,null)).getStatusCode().value());verify(cloud,never()).upload(any(),anyString(),anyString());
  }
  @Test void uploadStoresProviderReferenceAndOrder(){owned();when(db.queryForList(startsWith("SELECT orden"),anyMap())).thenReturn(List.of(Map.of("orden",3)));
    when(cloud.upload(any(),eq("image/png"),anyString())).thenAnswer(invocation->new CloudinaryImageClient.Asset(invocation.getArgument(2),"https://res.cloudinary.com/test/photo.png"));
    var image=service.upload("publication",file,null);assertEquals(4,image.get("orden"));assertEquals("https://res.cloudinary.com/test/photo.png",image.get("url"));verify(db).update(startsWith("INSERT INTO imagenes"),anyMap());
  }
  @Test void retryDoesNotDuplicateUploadedImage(){owned();var id=UUID.randomUUID().toString();when(db.queryForList(startsWith("SELECT id,url,orden"),anyMap())).thenReturn(List.of(Map.of("id",id,"url","https://res.cloudinary.com/test/photo.png","orden",0)));
    assertEquals(id,service.upload("publication",file,id).get("id"));verify(cloud,never()).upload(any(),anyString(),anyString());
  }
  @Test void dbFailureCleansNewCloudinaryAsset(){owned();when(cloud.upload(any(),anyString(),anyString())).thenAnswer(i->new CloudinaryImageClient.Asset(i.getArgument(2),"https://res.cloudinary.com/test/photo.png"));when(db.update(startsWith("INSERT INTO imagenes"),anyMap())).thenThrow(new IllegalStateException("db failed"));
    assertThrows(IllegalStateException.class,()->service.upload("publication",file,null));verify(cloud).delete(startsWith("lendup/objetos/publication/"));
  }
  @Test void deletionOnlyTargetsImageInOwnedPublication(){owned();when(db.queryForList(startsWith("SELECT cloudinary"),anyMap())).thenReturn(List.of(Map.of("cloudinary_public_id","lendup/objetos/publication/photo")));service.delete("publication","image");verify(cloud).delete("lendup/objetos/publication/photo");verify(db).update(startsWith("DELETE FROM imagenes"),eq(Map.of("publication","publication","id","image")));}
}
