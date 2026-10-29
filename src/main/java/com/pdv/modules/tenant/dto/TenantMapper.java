package com.pdv.modules.tenant.dto;

import com.pdv.modules.tenant.entity.Tenant;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantDTOs.Response toResponse(Tenant tenant);

    TenantDTOs.ResumoResponse toResumoResponse(Tenant tenant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "logoUrl", ignore = true)
    Tenant toEntity(TenantDTOs.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpj", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    void updateFromRequest(TenantDTOs.UpdateRequest request, @MappingTarget Tenant tenant);
}
