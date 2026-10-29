package com.pdv.modules.cliente.dto;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.pdv.modules.cliente.entity.Cliente;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    @Mapping(target = "creditoDisponivel",
             expression = "java(cliente.getCreditoDisponivel())")
    ClienteDTOs.Response toResponse(Cliente cliente);

    ClienteDTOs.ResumoResponse toResumoResponse(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "saldoDevedor", constant = "0.0")
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "version", ignore = true)
    Cliente toEntity(ClienteDTOs.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "cpfCnpj", ignore = true)
    @Mapping(target = "saldoDevedor", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateFromRequest(ClienteDTOs.UpdateRequest request, @MappingTarget Cliente cliente);
}
