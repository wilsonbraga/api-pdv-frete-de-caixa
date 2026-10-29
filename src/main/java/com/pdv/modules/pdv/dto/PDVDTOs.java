package com.pdv.modules.pdv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

public class PDVDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100)
        private String nome;

        @Size(max = 255)
        private String descricao;

        @Size(max = 100)
        private String deviceId;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100)
        private String nome;

        @Size(max = 255)
        private String descricao;
    }

    @Data
    public static class Response {
        private UUID id;
        private String nome;
        private String descricao;
        private String deviceId;
        private boolean ativo;
    }
}