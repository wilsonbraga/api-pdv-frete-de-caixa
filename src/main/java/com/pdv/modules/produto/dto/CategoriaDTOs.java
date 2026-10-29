package com.pdv.modules.produto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CategoriaDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100)
        private String nome;

        private UUID paiId;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100)
        private String nome;
    }

    @Data
    public static class Response {
        private UUID id;
        private String nome;
        private UUID paiId;
        private String nomePai;
        private boolean ativo;
        private List<Response> filhas;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private String nome;
        private UUID paiId;
        private boolean ativo;
    }
}
