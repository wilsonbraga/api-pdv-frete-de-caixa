package com.pdv.modules.produto.dto;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.pdv.modules.fornecedor.entity.Fornecedor;
import com.pdv.modules.produto.entity.CategoriaProduto;
import com.pdv.modules.produto.entity.HistoricoPreco;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.entity.ProdutoComponente;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

	@Mapping(target = "tenantId",           source = "tenant.id")
    @Mapping(target = "categoria",          source = "categoria")
    // Forçamos o mapeamento do fornecedor a usar o método específico de construção
    @Mapping(target = "fornecedor",         source = "fornecedor", qualifiedByName = "toFornecedorResumo")
    @Mapping(target = "estoqueAbaixoMinimo", expression = "java(produto.estoqueAbaixoMinimo())")
    @Mapping(target = "componentes",        source = "componentes")
    ProdutoDTOs.Response toResponse(Produto produto);

	@Mapping(target = "categoria",          source = "categoria")
    @Mapping(target = "fornecedor",         source = "fornecedor", qualifiedByName = "toFornecedorResumo")
    @Mapping(target = "estoqueAbaixoMinimo", expression = "java(produto.estoqueAbaixoMinimo())")
    ProdutoDTOs.ResumoResponse toResumoResponse(Produto produto);

    // ── Categoria ─────────────────────────────────────────────────────────
    @Mapping(target = "id",   source = "id")
    @Mapping(target = "nome", source = "nome")
    ProdutoDTOs.CategoriaResumo toCategoriaResumo(CategoriaProduto categoria);

    // Mapeamento específico para o Fornecedor para evitar confusão global de Strings
    @Named("toFornecedorResumo")
    @Mapping(target = "whatsappUrl", expression = "java(buildWhatsappUrl(fornecedor.getTelefone()))")
    ProdutoDTOs.FornecedorResumo toFornecedorResumo(Fornecedor fornecedor);

    // ── Componente ────────────────────────────────────────────────────────
    @Mapping(target = "id",              source = "id")
    @Mapping(target = "produtoFilhoId",  source = "produtoFilho.id")
    @Mapping(target = "nomeProdutoFilho",source = "produtoFilho.nome")
    ProdutoDTOs.ComponenteResponse toComponenteResponse(ProdutoComponente componente);

    // ── Entity → CREATE ───────────────────────────────────────────────────
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "tenant",      ignore = true)
    @Mapping(target = "categoria",   ignore = true)
    @Mapping(target = "fornecedor",  ignore = true)
    @Mapping(target = "margemLucro", ignore = true)
    @Mapping(target = "fotoUrl",     ignore = true)
    @Mapping(target = "ativo",       constant = "true")
    @Mapping(target = "componentes", ignore = true)
    @Mapping(target = "version",     ignore = true)
    Produto toEntity(ProdutoDTOs.CreateRequest request);

    // ── Entity → UPDATE ───────────────────────────────────────────────────
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "tenant",      ignore = true)
    @Mapping(target = "categoria",   ignore = true)
    @Mapping(target = "fornecedor",  ignore = true)
    @Mapping(target = "margemLucro", ignore = true)
    @Mapping(target = "estoqueAtual",ignore = true)
    @Mapping(target = "fotoUrl",     ignore = true)
    @Mapping(target = "ativo",       ignore = true)
    @Mapping(target = "componentes", ignore = true)
    @Mapping(target = "version",     ignore = true)
    void updateFromRequest(ProdutoDTOs.UpdateRequest request,
                           @MappingTarget Produto produto);

    // ── Histórico de preço ────────────────────────────────────────────────
    @Mapping(target = "nomeUsuario", source = "usuario.nome")
    ProdutoDTOs.HistoricoPrecoResponse toHistoricoResponse(HistoricoPreco historico);

    // ===== CATEGORIA =====
    @Mapping(target = "paiId",   source = "pai.id")
    @Mapping(target = "nomePai", source = "pai.nome")
    @Mapping(target = "filhas",  ignore = true)
    CategoriaDTOs.Response toCategoriaResponse(CategoriaProduto categoria);

    @Mapping(target = "paiId", source = "pai.id")
    CategoriaDTOs.ResumoResponse toCategoriaResumoResponse(CategoriaProduto categoria);

    @Mapping(target = "id",     ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "pai",    ignore = true)
    @Mapping(target = "ativo",  constant = "true")
    CategoriaProduto toCategoriaEntity(CategoriaDTOs.CreateRequest request);

    // ── Helper: NÃO é um método de mapeamento automático do MapStruct ─────
    // Prefixo "build" impede que o MapStruct o use como conversão String→String.
    // Adicione @Named aqui para que o MapStruct não o use automaticamente em outros campos String
    @Named("buildWhatsappUrl")
    default String buildWhatsappUrl(String telefone) {
        if (telefone == null || telefone.isBlank()) return null;
        String digits = telefone.replaceAll("\\D", "");
        if (!digits.startsWith("55") && digits.length() >= 10) {
            digits = "55" + digits;
        }
        return "https://wa.me/" + digits;
    }
}
