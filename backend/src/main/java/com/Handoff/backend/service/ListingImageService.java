package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.storage.ImageStorage;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListingImageService {
  private final ImageStorage storage;

  public ListingImageService(ImageStorage storage) {
    this.storage = storage;
  }

  public String upload(Student uploader, byte[] bytes) {
    if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024) {
      throw new InvalidImageException();
    }
    String extension;
    String contentType;
    if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
      extension = "jpg";
      contentType = "image/jpeg";
    } else if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
        && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
      extension = "png";
      contentType = "image/png";
    } else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
        && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
      extension = "webp";
      contentType = "image/webp";
    } else {
      throw new InvalidImageException();
    }
    String key = "listings/" + uploader.getId() + "/" + UUID.randomUUID() + "." + extension;
    return storage.store(key, bytes, contentType);
  }
}
