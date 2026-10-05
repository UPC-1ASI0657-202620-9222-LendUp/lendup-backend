package com.lendup.shared;

import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class FirebaseTokenFilter extends OncePerRequestFilter {
  private final FirebaseAuth auth;
  public FirebaseTokenFilter(FirebaseAuth auth) { this.auth=auth; }
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
    if(req.getMethod().equals("GET")&&(req.getServletPath().equals("/api/v1/terminos")||req.getServletPath().equals("/api/v1/universidades"))){chain.doFilter(req,res);return;}
    var header=req.getHeader("Authorization");
    if(header!=null&&header.startsWith("Bearer ")) {
      com.google.firebase.auth.FirebaseToken token;
      try { token=auth.verifyIdToken(header.substring(7)); }
      catch(Exception ignored) { res.sendError(401,"Token Firebase no válido");return; }
      boolean onboarding=(req.getMethod().equals("POST")&&req.getServletPath().equals("/api/v1/estudiantes"))
        ||(req.getMethod().equals("GET")&&req.getServletPath().equals("/api/v1/estudiantes/me"));
      boolean publicTerms=req.getMethod().equals("GET")&&req.getServletPath().equals("/api/v1/terminos");
      boolean webhook=req.getMethod().equals("POST")&&req.getServletPath().equals("/api/v1/webhooks/mercado-pago");
      if(req.getServletPath().startsWith("/api/v1/")&&!onboarding&&!webhook&&!publicTerms&&!Boolean.TRUE.equals(token.getClaims().get("email_verified"))) {
        res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"status\":403,\"error\":\"EMAIL_NOT_VERIFIED\",\"message\":\"Verifica tu correo para ingresar a LendUp\"}");return;
      }
      var authentication=new UsernamePasswordAuthenticationToken(token.getUid(),null,List.of());
      authentication.setDetails(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    chain.doFilter(req,res);
  }
}
