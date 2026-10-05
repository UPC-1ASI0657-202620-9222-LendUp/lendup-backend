package com.lendup.shared;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class UniversityCatalogServiceTest {
  private final List<UniversityCatalogService.University> catalog=List.of(
    new UniversityCatalogService.University("PUCP","PUCP","Pontificia",List.of("pucp.edu.pe","pucp.pe")));
  @Test void resolvesAllExactAliasesAndNormalizesEmail(){
    assertEquals("PUCP",UniversityCatalogService.resolve(" alumno@pucp.pe ",catalog).id());
    assertEquals("PUCP",UniversityCatalogService.resolve("ALUMNO@PUCP.EDU.PE",catalog).id());
  }
  @Test void rejectsUnknownMalformedAndLookalikeDomains(){
    for(var email:List.of("a@gmail.com","a@sub.pucp.pe","a@pucp.pe.evil.test","a@evilpucp.pe","a@@pucp.pe","@pucp.pe","a"))
      assertThrows(ResponseStatusException.class,()->UniversityCatalogService.resolve(email,catalog));
  }
  @Test void campusCanBeOmittedOrFreelyEntered(){
    assertEquals("",UniversityCatalogService.campus(null));
    assertEquals("",UniversityCatalogService.campus("  "));
    assertEquals("Nueva sede",UniversityCatalogService.campus(" Nueva sede "));
    assertThrows(ResponseStatusException.class,()->UniversityCatalogService.campus("x".repeat(151)));
  }
}
