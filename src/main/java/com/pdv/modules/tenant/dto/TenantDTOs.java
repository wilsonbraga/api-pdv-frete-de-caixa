package com.pdv.modules.tenant.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.br.CNPJ;

import java.time.LocalDateTime;
import java.util.UUID;

public class TenantDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        private String nome;

        @CNPJ(message = "CNPJ inválido")
        private String cnpj;

        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 20)
        private String telefone;

        @Size(max = 255)
        private String endereco;

        @Size(max = 100)
        private String cidade;

        @Size(max = 2)
        private String estado;

        @Size(max = 10)
        private String cep;

        @NotBlank(message = "Plano é obrigatório")
        private String plano;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        private String nome;

        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 20)
        private String telefone;

        @Size(max = 255)
        private String endereco;

        @Size(max = 100)
        private String cidade;

        @Size(max = 2)
        private String estado;

        @Size(max = 10)
        private String cep;

        private String plano;
    }

    @Data
    public static class Response {
        private UUID id;
        private String nome;
        private String cnpj;
        private String email;
        private String telefone;
        private String endereco;
        private String cidade;
        private String estado;
        private String cep;
        private String logoUrl;
        private boolean ativo;
        private String plano;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private String nome;
        private String cnpj;
        private boolean ativo;
        private String plano;
    }
}
