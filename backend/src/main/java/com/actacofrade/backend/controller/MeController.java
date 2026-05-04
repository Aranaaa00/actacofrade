package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.ChangePasswordRequest;
import com.actacofrade.backend.dto.UpdateProfileRequest;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.UserAvatar;
import com.actacofrade.backend.service.MeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/me")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Me", description = "Operaciones del usuario autenticado")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(meService.me(userDetails.getUsername()));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(meService.updateProfile(request, userDetails.getUsername()));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        meService.changePassword(request, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(meService.uploadAvatar(file, userDetails.getUsername()));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal UserDetails userDetails) {
        meService.deleteAvatar(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getUserAvatar(@org.springframework.web.bind.annotation.PathVariable Integer userId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        Optional<UserAvatar> avatar = meService.getAvatar(userId, userDetails.getUsername());
        if (avatar.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserAvatar a = avatar.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, a.getContentType())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(a.getData());
    }
}
