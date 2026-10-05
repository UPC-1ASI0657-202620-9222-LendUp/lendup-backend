package com.lendup.catalogo.interfaces.rest;
import com.lendup.shared.PublicationImagesService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/v1/objetos/{id}/imagenes")
public class PublicationImagesController {
  private final PublicationImagesService images;
  public PublicationImagesController(PublicationImagesService images){this.images=images;}
  @PostMapping(consumes="multipart/form-data") public ResponseEntity<Map<String,Object>> upload(@PathVariable String id,@RequestParam("file") MultipartFile file,@RequestParam(value="uploadId",required=false) String uploadId){return ResponseEntity.status(201).body(images.upload(id,file,uploadId));}
  @DeleteMapping("/{imageId}") public ResponseEntity<Void> delete(@PathVariable String id,@PathVariable String imageId){images.delete(id,imageId);return ResponseEntity.noContent().build();}
}
