package com.Handoff.backend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.InvalidImageException;
import com.Handoff.backend.service.ListingImageService;
import com.Handoff.backend.service.NotAuthenticatedException;
import com.Handoff.backend.service.storage.ImageStorageException;
import com.Handoff.backend.service.storage.ImageStorageNotConfiguredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ListingImageControllerTest {
  private final ListingImageService service = mock(ListingImageService.class);
  private final CurrentStudentResolver resolver = mock(CurrentStudentResolver.class);
  private MockMvc mvc;
  private final MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{(byte)0xff, (byte)0xd8, (byte)0xff});

  @BeforeEach
  void setUp() {
    reset(service, resolver);
    mvc = MockMvcBuilders.standaloneSetup(new ListingImageController(service, resolver))
        .setControllerAdvice(new ApiExceptionHandler()).build();
  }

  @Test
  void upload_returns401_whenNotLoggedIn() throws Exception {
    when(resolver.require(any(), anyString())).thenThrow(new NotAuthenticatedException("You must be logged in to upload photos"));
    mvc.perform(multipart("/listings/images").file(file)).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("You must be logged in to upload photos"));
  }

  @Test
  void upload_returns201WithUrl() throws Exception {
    when(resolver.require(any(), anyString())).thenReturn(new Student());
    when(service.upload(any(), any())).thenReturn("https://example.com/photo.jpg");
    mvc.perform(multipart("/listings/images").file(file)).andExpect(status().isCreated())
        .andExpect(jsonPath("$.url").value("https://example.com/photo.jpg"));
  }

  @Test
  void upload_returns400_forBadFile() throws Exception {
    when(resolver.require(any(), anyString())).thenReturn(new Student());
    when(service.upload(any(), any())).thenThrow(new InvalidImageException());
    mvc.perform(multipart("/listings/images").file(file)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Photos must be JPEG, PNG or WebP and at most 5 MB"));
  }

  @Test
  void upload_returns503_whenStorageNotConfigured() throws Exception {
    when(resolver.require(any(), anyString())).thenReturn(new Student());
    when(service.upload(any(), any())).thenThrow(new ImageStorageNotConfiguredException());
    mvc.perform(multipart("/listings/images").file(file)).andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Photo uploads are not configured"));
  }

  @Test
  void upload_returns502_whenStorageFails() throws Exception {
    when(resolver.require(any(), anyString())).thenReturn(new Student());
    when(service.upload(any(), any())).thenThrow(new ImageStorageException(400));
    mvc.perform(multipart("/listings/images").file(file)).andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Couldn't store the photo. Please try again."));
  }
}
