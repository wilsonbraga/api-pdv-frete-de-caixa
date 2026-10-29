package com.pdv.modules.catalogo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CatalogoDTOs {

    @Data
    public static class CategoriaResponse {
        private UUID   id;
        private String nome;
        private String segmento;
        private String icone;
        private int    ordem;
        private long   totalProdutos;
    }

    @Data
    public static class ProdutoResponse {
        private UUID       id;
        private String     nome;
        private String     codigoBarras;
        private String     ncm;
        private String     unidade;
        private BigDecimal precoSugerido;
        private String     descricao;
        private String     categoriaNome;
        private String     categoriaSegmento;
        private UUID       categoriaId;
    }

    /** Payload enviado pelo front para importar produtos de segmentos escolhidos */
    @Data
    public static class ImportarRequest {
        private List<UUID>   categoriaIds;    // categorias específicas
        private List<String> segmentos;       // ou segmentos inteiros
        private boolean      sobrescrever;    // sobrescreve se código de barras já existe
    }

    @Data
    public static class ImportarResponse {
        private int importados;
        private int ignorados;   // já existiam e sobrescrever=false
        private int erros;
        private List<String> detalhes;
    }
}
