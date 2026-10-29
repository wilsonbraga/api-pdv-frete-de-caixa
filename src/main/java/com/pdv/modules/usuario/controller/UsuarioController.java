package com.pdv.modules.usuario.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.usuario.dto.UsuarioDTOs;
import com.pdv.modules.usuario.service.UsuarioService;
import com.pdv.shared.dto.ApiResponse;
import com.pdv.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gestão de usuários do tenant")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria um novo usuário no tenant do solicitante")
    public ResponseEntity<ApiResponse<UsuarioDTOs.Response>> criar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UsuarioDTOs.CreateRequest request) {
        UsuarioDTOs.Response response = usuarioService.criar(principal.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuário criado com sucesso", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Busca usuário por ID dentro do tenant")
    public ResponseEntity<ApiResponse<UsuarioDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(usuarioService.buscarPorId(id, principal.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista usuários do tenant com filtros e paginação")
    public ResponseEntity<ApiResponse<PageResponse<UsuarioDTOs.ResumoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nome") String sort) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(ApiResponse.success(
                usuarioService.listar(principal.getTenantId(), nome, email, ativo, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza dados de um usuário")
    public ResponseEntity<ApiResponse<UsuarioDTOs.Response>> atualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioDTOs.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Usuário atualizado com sucesso",
                usuarioService.atualizar(id, principal.getTenantId(), request)));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativa um usuário")
    public ResponseEntity<ApiResponse<Void>> ativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        usuarioService.ativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Usuário ativado com sucesso"));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa um usuário")
    public ResponseEntity<ApiResponse<Void>> desativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        usuarioService.desativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Usuário desativado com sucesso"));
    }

    @PatchMapping("/{id}/senha")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Redefine a senha de um usuário (admin)")
    public ResponseEntity<ApiResponse<Void>> redefinirSenha(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioDTOs.RedefinirSenhaRequest request) {
        usuarioService.redefinirSenha(id, principal.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success("Senha redefinida com sucesso"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retorna dados completos do usuário autenticado")
    public ResponseEntity<ApiResponse<UsuarioDTOs.Response>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                usuarioService.buscarPorId(principal.getId(), principal.getTenantId())));
    }
}
