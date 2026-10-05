package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
class CloudinaryImageClientTest {
  @Test void matchesOfficialCloudinarySignatureExamples(){
    assertEquals("a21ad0f63beb4de2e5575204b79ab90bffb02c10",CloudinaryImageClient.signature(Map.of("timestamp","1315060510"),"abcd"));
    assertEquals("bfd09f95f331f558cbd1320e67aa8d488770583e",CloudinaryImageClient.signature(Map.of("timestamp","1315060510","public_id","sample_image","eager","w_400,h_300,c_pad|w_260,h_200,c_crop"),"abcd"));
  }
  @Test void missingCredentialsDisableOnlyUploads(){
    var client=new CloudinaryImageClient("","","",JsonMapper.builder().build());
    assertEquals(503,assertThrows(ResponseStatusException.class,client::requireConfigured).getStatusCode().value());
  }
  @Test void sendsImageDataAndAcceptsSecureProviderResult(){
    var client=new CloudinaryImageClient("test","1234","secret",JsonMapper.builder().build()){
      @Override protected Map<String,Object> request(String action,Map<String,String> signed,Map<String,String> extra){
        assertEquals("upload",action);assertEquals("false",signed.get("overwrite"));
        assertEquals("data:image/png;base64,AQID",extra.get("file"));
        return Map.of("public_id",signed.get("public_id"),"secure_url","https://res.cloudinary.com/test/image/upload/photo.png");
      }
    };
    assertEquals("lendup/objetos/test/photo",client.upload(new byte[]{1,2,3},"image/png","lendup/objetos/test/photo").publicId());
  }
  @Test void deletionInvalidatesCdnAndCanRetryMissingAsset(){
    var client=new CloudinaryImageClient("test","1234","secret",JsonMapper.builder().build()){
      @Override protected Map<String,Object> request(String action,Map<String,String> signed,Map<String,String> extra){assertEquals("destroy",action);assertEquals("true",signed.get("invalidate"));return Map.of("result","not found");}
    };assertDoesNotThrow(()->client.delete("photo"));
  }
}
