package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class TermsDocumentServiceTest {
  @Test void publishesCompleteDocument() throws Exception {
    var service=new TermsDocumentService();var document=service.document();var content=(String)document.get("contenido");
    assertEquals(23,content.lines().filter(line->line.matches("[0-9]+\\. .*" )).count());
    assertTrue(content.contains("La tarifa diaria establecida debe ser mayor que cero."));
    assertEquals(TermsDocumentService.VERSION,document.get("version_terminos"));
    assertEquals(TermsDocumentService.VERSION,document.get("version_descargo"));
  }
  @Test void acceptsCurrentVersions() throws Exception {new TermsDocumentService().validateAcceptance(TermsDocumentService.VERSION,TermsDocumentService.VERSION);}
  @Test void rejectsOldOrInventedVersions() throws Exception {
    var service=new TermsDocumentService();
    assertThrows(ResponseStatusException.class,()->service.validateAcceptance("1.0",TermsDocumentService.VERSION));
    assertThrows(ResponseStatusException.class,()->service.validateAcceptance(TermsDocumentService.VERSION,"future"));
  }
}
