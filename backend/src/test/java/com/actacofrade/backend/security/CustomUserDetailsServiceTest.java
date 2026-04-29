package com.actacofrade.backend.security;

import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadByUsername_returnsAuthorities() {
        User u = TestFixtures.user(1, "u@e.com", TestFixtures.hermandad(1, "H"), RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("u@e.com");

        assertThat(details.getUsername()).isEqualTo("u@e.com");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMINISTRADOR");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadByUsername_inactiveUser_disabled() {
        User u = TestFixtures.user(1, "u@e.com", TestFixtures.hermandad(1, "H"), RoleCode.COLABORADOR);
        u.setActive(false);
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("u@e.com");
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadByUsername_missing_throwsUsernameNotFound() {
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("x@e.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
