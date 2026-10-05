package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class FirebaseTokenFilterTest {
  @AfterEach void clean(){SecurityContextHolder.clearContext();}
  private void request(String method,String path,Boolean verified,int status,boolean expected) throws Exception {
    var auth=mock(FirebaseAuth.class);var token=mock(FirebaseToken.class);
    when(auth.verifyIdToken("test-token")).thenReturn(token);when(token.getUid()).thenReturn("student");
    when(token.getClaims()).thenReturn(verified==null?Map.of():Map.of("email_verified",verified));
    var req=new MockHttpServletRequest(method,path);req.setServletPath(path);req.addHeader("Authorization","Bearer test-token");
    var res=new MockHttpServletResponse();var passed=new AtomicBoolean();
    new FirebaseTokenFilter(auth).doFilter(req,res,(a,b)->passed.set(true));
    assertEquals(status,res.getStatus());assertEquals(expected,passed.get());
    if(status==403)assertTrue(res.getContentAsString().contains("EMAIL_NOT_VERIFIED"));
  }
  @Test void unverifiedCannotUseApp() throws Exception {request("GET","/api/v1/objetos",false,403,false);}
  @Test void missingClaimCannotUseApp() throws Exception {request("GET","/api/v1/prestamos",null,403,false);}
  @Test void verifiedCanUseApp() throws Exception {request("GET","/api/v1/objetos",true,200,true);}
  @Test void unverifiedCanRegister() throws Exception {request("POST","/api/v1/estudiantes",false,200,true);}
  @Test void unverifiedCanReadProfile() throws Exception {request("GET","/api/v1/estudiantes/me",false,200,true);}
  @Test void unverifiedCannotEditProfile() throws Exception {request("PUT","/api/v1/estudiantes/me",false,403,false);}
}
