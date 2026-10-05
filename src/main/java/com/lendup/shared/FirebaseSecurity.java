package com.lendup.shared;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;
@Configuration
public class FirebaseSecurity {
  @Value("${firebase.project-id}") String projectId;
  @Bean FirebaseAuth firebaseAuth() throws IOException {
    if(FirebaseApp.getApps().isEmpty()) {
      var emulator=System.getenv("FIREBASE_AUTH_EMULATOR_HOST")!=null;
      GoogleCredentials credentials;
      if(emulator){
        credentials=GoogleCredentials.create(new AccessToken("emulator-only",new Date(System.currentTimeMillis()+86400000L)));
      }else{
        try{credentials=GoogleCredentials.getApplicationDefault();}
        catch(IOException e){throw new IllegalStateException(
          "Firebase no tiene credenciales. Para pruebas locales inicia Authentication Emulator y configura " +
          "FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 y FIREBASE_PROJECT_ID=demo-lendup; " +
          "para Firebase real configura GOOGLE_APPLICATION_CREDENTIALS con un archivo de credenciales válido.",e);}
      }
      FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).setProjectId(projectId).build());
    }
    return FirebaseAuth.getInstance();
  }
  @Bean SecurityFilterChain security(HttpSecurity http,FirebaseAuth auth) throws Exception {
    var filter=new FirebaseTokenFilter(auth);
    return http.csrf(c->c.disable())
      .cors(c->c.configurationSource(request->{
        var cfg=new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(System.getenv().getOrDefault("CORS_ORIGINS","http://localhost:5173")));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
        return cfg;
      }))
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .exceptionHandling(e->e.authenticationEntryPoint((request,response,exception)->response.sendError(401,"Autenticación requerida")))
      .authorizeHttpRequests(a->a
      .requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
      .requestMatchers(HttpMethod.GET,"/api/v1/terminos").permitAll()
      .requestMatchers(HttpMethod.POST,"/api/v1/webhooks/mercado-pago").permitAll()
      .anyRequest().authenticated())
      .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
  }
}
