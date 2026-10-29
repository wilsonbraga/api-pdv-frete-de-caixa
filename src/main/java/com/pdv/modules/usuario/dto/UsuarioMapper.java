package com.pdv.modules.usuario.dto;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.pdv.modules.usuario.entity.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "possuiPin", expression = "java(usuario.getPinHash() != null)")
    UsuarioDTOs.Response toResponse(Usuario usuario);
    
    //TODO: COMENTEI POIS ESTAVA DANDO ERRO, MAS AINDA ESTA O TRIBUTO NA CLASS UsuarioDTOs.ResumoResponse
   // @Mapping(target = "tenantId", source = "tenant.id")
    UsuarioDTOs.ResumoResponse toResumoResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "senhaHash", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "pinHash", ignore = true)
    @Mapping(target = "ultimoLogin", ignore = true)
    @Mapping(target = "version", ignore = true)
    Usuario toEntity(UsuarioDTOs.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "senhaHash", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "pinHash", ignore = true)
    @Mapping(target = "ultimoLogin", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateFromRequest(UsuarioDTOs.UpdateRequest request, @MappingTarget Usuario usuario);
}
