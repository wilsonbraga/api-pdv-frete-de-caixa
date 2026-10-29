package com.pdv.modules.usuario.dto;

import com.pdv.modules.usuario.entity.RoleUsuario;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class UsuarioDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        private String nome;

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        private String senha;

        @NotNull(message = "Role é obrigatório")
        private RoleUsuario role;

        @DecimalMin(value = "0.0", message = "Desconto máximo não pode ser negativo")
        @DecimalMax(value = "100.0", message = "Desconto máximo não pode ultrapassar 100%")
        private Double descontoMax = 0.0;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        private String nome;

        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @NotNull(message = "Role é obrigatório")
        private RoleUsuario role;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private Double descontoMax;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID tenantId;
        private String nome;
        private String email;
        private RoleUsuario role;
        private boolean ativo;
        private Double descontoMax;
        private boolean possuiPin;
        private LocalDateTime ultimoLogin;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        //TODO: obs: esta dando erro na classe UsuarioMapper
        private UUID tenantId;
        private String nome;
        private String email;
        private RoleUsuario role;
        private boolean ativo;
    }

    @Data
    public static class RedefinirSenhaRequest {
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6)
        private String novaSenha;
    }
}
