package com.pdv.modules.auth.controller;

import com.pdv.modules.auth.dto.AuthDTOs;
import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.auth.service.AuthService;
import com.pdv.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login, refresh e logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna tokens JWT")
    public ResponseEntity<ApiResponse<AuthDTOs.TokenResponse>> login(
            @Valid @RequestBody AuthDTOs.LoginRequest request) {
        AuthDTOs.TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login realizado com sucesso", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token usando o refresh token")
    public ResponseEntity<ApiResponse<AuthDTOs.TokenResponse>> refresh(
            @Valid @RequestBody AuthDTOs.RefreshTokenRequest request) {
        AuthDTOs.TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token renovado com sucesso", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token atual")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody AuthDTOs.LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logout realizado com sucesso"));
    }

    @PostMapping("/logout/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoga todos os refresh tokens do usuário (todos os dispositivos)")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logoutTodosDispositivos(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Logout em todos os dispositivos realizado"));
    }

    @PatchMapping("/senha")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Altera a senha do usuário autenticado")
    public ResponseEntity<ApiResponse<Void>> alterarSenha(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthDTOs.AlterarSenhaRequest request) {
        authService.alterarSenha(principal.getId(), principal.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success("Senha alterada com sucesso"));
    }

    @PostMapping("/pin/validar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Valida o PIN do supervisor para operações críticas offline")
    public ResponseEntity<ApiResponse<AuthDTOs.ValidarPinResponse>> validarPin(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthDTOs.ValidarPinRequest request) {
        AuthDTOs.ValidarPinResponse response = authService.validarPin(
                principal.getId(), principal.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/pin/cadastrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cadastra ou atualiza o PIN do supervisor")
    public ResponseEntity<ApiResponse<Void>> cadastrarPin(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String pin) {
        if (pin.length() != 4 && pin.length() != 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PIN deve ter 4 ou 6 dígitos"));
        }
        if (!pin.matches("\\d+")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PIN deve conter apenas números"));
        }
        authService.cadastrarPin(principal.getId(), principal.getTenantId(), pin);
        return ResponseEntity.ok(ApiResponse.success("PIN cadastrado com sucesso"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retorna dados do usuário autenticado")
    public ResponseEntity<ApiResponse<AuthDTOs.TokenResponse.UsuarioInfo>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        AuthDTOs.TokenResponse.UsuarioInfo info = new AuthDTOs.TokenResponse.UsuarioInfo();
        info.setId(principal.getId().toString());
        info.setNome(principal.getNome());
        info.setEmail(principal.getEmail());
        info.setRole(principal.getRole());
        info.setTenantId(principal.getTenantId().toString());
        info.setDescontoMax(principal.getDescontoMax());
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}
