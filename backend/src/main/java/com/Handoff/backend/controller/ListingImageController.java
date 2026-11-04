package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ImageUploadResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.ListingImageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/listings/images")
public class ListingImageController {
  private final ListingImageService imageService;
  private final CurrentStudentResolver currentStudentResolver;

  public ListingImageController(ListingImageService imageService, CurrentStudentResolver currentStudentResolver) {
    this.imageService = imageService;
    this.currentStudentResolver = currentStudentResolver;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                     HttpServletRequest request) throws IOException {
    Student uploader = currentStudentResolver.require(request, "You must be logged in to upload photos");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ImageUploadResponse(imageService.upload(uploader, file.getBytes())));
  }
}
