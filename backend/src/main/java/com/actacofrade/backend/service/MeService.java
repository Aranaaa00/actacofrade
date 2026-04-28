package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.ChangePasswordRequest;
import com.actacofrade.backend.dto.UpdateProfileRequest;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.entity.UserAvatar;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.SanitizationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class MeService {

    private final UserRepository userRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final PasswordEncoder passwordEncoder;
    private final long avatarMaxBytes;
    private final Set<String> allowedAvatarTypes;

    public MeService(UserRepository userRepository,
                     UserAvatarRepository userAvatarRepository,
                     PasswordEncoder passwordEncoder,
                     @Value("${profile.avatar.max-bytes:2097152}") long avatarMaxBytes,
                     @Value("${profile.avatar.allowed-types:image/png,image/jpeg,image/webp,image/gif}") String allowedTypes) {
        this.userRepository = userRepository;
        this.userAvatarRepository = userAvatarRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarMaxBytes = avatarMaxBytes;
        this.allowedAvatarTypes = Set.of(allowedTypes.split(","));
    }

    public UserResponse me(String authenticatedEmail) {
        User user = loadUser(authenticatedEmail);
        return toResponse(user);
    }

    public UserResponse updateProfile(UpdateProfileRequest request, String authenticatedEmail) {
        User user = loadUser(authenticatedEmail);

        String newEmail = SanitizationUtils.sanitize(request.email()).toLowerCase();
        String newName = SanitizationUtils.sanitize(request.fullName());

        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }

        user.setFullName(newName);
        user.setEmail(newEmail);
        userRepository.save(user);
        return toResponse(user);
    }

    public void changePassword(ChangePasswordRequest request, String authenticatedEmail) {
        User user = loadUser(authenticatedEmail);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AccessDeniedException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalStateException("La nueva contraseña debe ser distinta de la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public UserResponse uploadAvatar(MultipartFile file, String authenticatedEmail) {
        User user = loadUser(authenticatedEmail);
        validateAvatar(file);

        UserAvatar avatar = userAvatarRepository.findByUserId(user.getId())
                .orElseGet(UserAvatar::new);
        avatar.setUser(user);
        avatar.setContentType(file.getContentType());
        try {
            avatar.setData(file.getBytes());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo de imagen");
        }
        avatar.setUpdatedAt(OffsetDateTime.now());
        userAvatarRepository.save(avatar);

        return toResponse(user);
    }

    public void deleteAvatar(String authenticatedEmail) {
        User user = loadUser(authenticatedEmail);
        userAvatarRepository.deleteByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<UserAvatar> getAvatar(Integer targetUserId, String authenticatedEmail) {
        User requester = loadUser(authenticatedEmail);
        if (requester.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        User target = userRepository.findByIdAndHermandadId(targetUserId, requester.getHermandad().getId())
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado o no pertenece a tu hermandad"));
        return userAvatarRepository.findByUserId(target.getId());
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío o inexistente");
        }
        if (file.getSize() > avatarMaxBytes) {
            throw new IllegalStateException("El archivo supera el tamaño máximo permitido");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedAvatarTypes.contains(contentType)) {
            throw new IllegalStateException("Tipo de archivo no permitido");
        }
    }

    private User loadUser(String authenticatedEmail) {
        return userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .toList();
        boolean hasAvatar = userAvatarRepository.existsByUserId(user.getId());
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roles,
                user.getActive(),
                user.getLastLogin(),
                hasAvatar
        );
    }
}
