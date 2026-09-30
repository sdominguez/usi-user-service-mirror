package com.musa.users.controller;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.RegisterService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {

        MessageResponseDto response = registerService.register(request);

        return ResponseEntity.ok(response);
    }
}