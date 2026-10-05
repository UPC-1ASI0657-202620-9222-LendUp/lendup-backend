package com.lendup.shared;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class TermsDocumentService {
  public static final String VERSION="2026-10-04";
  private final String content;
  public TermsDocumentService() throws IOException {
    content=new ClassPathResource("terminos-lendup.txt").getContentAsString(StandardCharsets.UTF_8);
  }
  public Map<String,Object> document() {
    return Map.of("titulo","Términos y Condiciones de LendUp","version_terminos",VERSION,
      "version_descargo",VERSION,"publicado_en","2026-10-04","idioma","es","contenido",content);
  }
  public void validateAcceptance(String terms,String disclaimer) {
    if(!VERSION.equals(terms)||!VERSION.equals(disclaimer))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Los términos cambiaron. Recarga el documento y acepta la versión vigente.");
  }
}
