package com.Handoff.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.storage.ImageStorage;
import org.junit.jupiter.api.Test;

class ListingImageServiceTest {
  @Test
  void upload_detectsJpegPngWebp_byMagicBytes() {
    ImageStorage storage = mock(ImageStorage.class);
    Student student = new Student();
    student.setId(7L);
    ListingImageService service = new ListingImageService(storage);
    byte[][] samples = { {(byte)0xff, (byte)0xd8, (byte)0xff},
        {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
        {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'} };
    String[] extensions = {"jpg", "png", "webp"};
    String[] types = {"image/jpeg", "image/png", "image/webp"};
    for (int i = 0; i < samples.length; i++) {
      service.upload(student, samples[i]);
      verify(storage).store(matches("listings/7/[0-9a-f-]{36}\\." + extensions[i]), eq(samples[i]), eq(types[i]));
    }
  }

  @Test
  void upload_rejectsEmpty() {
    assertThatThrownBy(() -> new ListingImageService(mock(ImageStorage.class))
        .upload(new Student(), new byte[0])).isInstanceOf(InvalidImageException.class);
  }

  @Test
  void upload_rejectsOver5MB() {
    assertThatThrownBy(() -> new ListingImageService(mock(ImageStorage.class))
        .upload(new Student(), new byte[5 * 1024 * 1024 + 1])).isInstanceOf(InvalidImageException.class);
  }

  @Test
  void upload_rejectsFileWhoseBytesAreNotAnImage() {
    assertThatThrownBy(() -> new ListingImageService(mock(ImageStorage.class))
        .upload(new Student(), "hello".getBytes())).isInstanceOf(InvalidImageException.class);
  }
}
