package com.lendup.catalogo.domain.repositories;
import com.lendup.shared.PersistencePort;
import java.util.List;
import java.util.Map;
/** Domain repository contract for the catalogo bounded context. */
public interface CatalogoRepository extends PersistencePort {
  List<Map<String,Object>> availability(String publicationId);
  void lockPublication(String publicationId);
  boolean availabilityOverlaps(String publicationId,String excludedId,Object from,Object to);
  void deleteAvailability(String publicationId,String availabilityId);
}
