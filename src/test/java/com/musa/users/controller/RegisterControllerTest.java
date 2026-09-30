package com.musa.users.controller;

import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.RegisterService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegisterController.class)
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterService registerService;

    @Test
    @WithMockUser
    void register_debeResponder200ConMensajeExitoso() throws Exception {

        // ARRANGE: simula la respuesta del servicio
        when(registerService.register(any())).thenReturn(new MessageResponseDto("Registro exitoso."));

        String json = """
                {
                    "fullName": "Ana López",
                    "email": "ana@usi.com",
                    "password": "Password123!",
                    "roleName": "USER"
                }
                """;

        // ACT + ASSERT: envia una petición HTTP simulada
        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Registro exitoso."));
    }
}