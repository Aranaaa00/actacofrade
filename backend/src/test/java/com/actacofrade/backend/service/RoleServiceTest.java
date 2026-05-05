package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RoleResponse;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests unitarios de RoleService.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void findAll_devuelveRolesMapeadosADto() {
        Role admin = new Role(1, RoleCode.ADMINISTRADOR, "Administrador");
        Role responsable = new Role(2, RoleCode.RESPONSABLE, "Responsable");
        given(roleRepository.findAll()).willReturn(List.of(admin, responsable));

        List<RoleResponse> result = roleService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1);
        assertThat(result.get(0).code()).isEqualTo("ADMINISTRADOR");
        assertThat(result.get(0).description()).isEqualTo("Administrador");
        assertThat(result.get(1).code()).isEqualTo("RESPONSABLE");
    }

    @Test
    void findAll_sinRoles_devuelveListaVacia() {
        given(roleRepository.findAll()).willReturn(List.of());

        List<RoleResponse> result = roleService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_codigoNulo_seSerializaComoNull() {
        Role roleSinCodigo = new Role(99, null, "Sin código");
        given(roleRepository.findAll()).willReturn(List.of(roleSinCodigo));

        List<RoleResponse> result = roleService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isNull();
        assertThat(result.get(0).description()).isEqualTo("Sin código");
    }
}
