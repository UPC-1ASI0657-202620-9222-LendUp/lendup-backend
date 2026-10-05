package com.lendup.shared;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/universidades")
public class UniversityCatalogController {
  private final UniversityCatalogService catalog;
  public UniversityCatalogController(UniversityCatalogService catalog){this.catalog=catalog;}
  @GetMapping public List<UniversityCatalogService.University> list(){return catalog.list();}
}
