package com.actacofrade.backend.controller;

import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de RoleController.
 */
@ExtendWith(MockitoExtension.class)
class RoleControllerIntegrationTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleController roleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_returns200WithRoles() throws Exception {
        Role admin = new Role(1, RoleCode.ADMINISTRADOR, "Administrador");
        Role responsable = new Role(2, RoleCode.RESPONSABLE, "Responsable");
        given(roleRepository.findAll()).willReturn(List.of(admin, responsable));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$[1].code").value("RESPONSABLE"));
    }

    @Test
    void findAll_emptyList_returns200() throws Exception {
        given(roleRepository.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
