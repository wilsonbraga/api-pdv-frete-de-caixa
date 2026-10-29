package com.pdv.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDTOs {

    @Data
    public static class LoginRequest {
        
    	@NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        private String senha;

        private String tenantId;

        private String deviceId;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token é obrigatório")
        private String refreshToken;
    }

    @Data
    public static class LogoutRequest {
        @NotBlank(message = "Refresh token é obrigatório")
        private String refreshToken;
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;
        private UsuarioInfo usuario;

        @Data
        public static class UsuarioInfo {
            private String id;
            private String nome;
            private String email;
            private String role;
            private String tenantId;
            private Double descontoMax;
        }
    }

    @Data
    public static class AlterarSenhaRequest {
        @NotBlank(message = "Senha atual é obrigatória")
        private String senhaAtual;

        @NotBlank(message = "Nova senha é obrigatória")
        private String novaSenha;
    }

    @Data
    public static class ValidarPinRequest {
        @NotBlank(message = "PIN é obrigatório")
        private String pin;

        @NotBlank(message = "Ação é obrigatória")
        private String acao;
    }

    @Data
    public static class ValidarPinResponse {
        private boolean valido;
        private String mensagem;
    }
}
