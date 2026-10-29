package com.pdv.modules.fornecedor.dto;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.pdv.modules.fornecedor.entity.Compra;
import com.pdv.modules.fornecedor.entity.Fornecedor;
import com.pdv.modules.fornecedor.entity.ItemCompra;

@Mapper(componentModel = "spring")
public interface FornecedorMapper {

    FornecedorDTOs.Response toResponse(Fornecedor fornecedor);

    FornecedorDTOs.ResumoResponse toResumoResponse(Fornecedor fornecedor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    Fornecedor toEntity(FornecedorDTOs.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "cnpj", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    void updateFromRequest(FornecedorDTOs.UpdateRequest request,
                           @MappingTarget Fornecedor fornecedor);

    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "fornecedor", source = "fornecedor")
    @Mapping(target = "nomeUsuario", source = "usuario.nome")
    @Mapping(target = "itens", source = "itens")
    FornecedorDTOs.CompraResponse toCompraResponse(Compra compra);

    @Mapping(target = "fornecedor", source = "fornecedor")
    FornecedorDTOs.CompraResumoResponse toCompraResumoResponse(Compra compra);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "produtoId", source = "produto.id")
    @Mapping(target = "nomeProduto", source = "produto.nome")
    FornecedorDTOs.ItemCompraResponse toItemCompraResponse(ItemCompra item);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nomeFantasia", source = "nomeFantasia")
    FornecedorDTOs.FornecedorResumo toFornecedorResumo(Fornecedor fornecedor);
}
