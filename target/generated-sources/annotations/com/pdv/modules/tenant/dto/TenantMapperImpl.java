package com.pdv.modules.tenant.dto;

import com.pdv.modules.tenant.entity.Tenant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-29T09:27:47-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.38.0.v20240524-2033, environment: Java 21.0.3 (Eclipse Adoptium)"
)
@Component
public class TenantMapperImpl implements TenantMapper {

    @Override
    public TenantDTOs.Response toResponse(Tenant tenant) {
        if ( tenant == null ) {
            return null;
        }

        TenantDTOs.Response response = new TenantDTOs.Response();

        response.setAtivo( tenant.isAtivo() );
        response.setAtualizadoEm( tenant.getAtualizadoEm() );
        response.setCep( tenant.getCep() );
        response.setCidade( tenant.getCidade() );
        response.setCnpj( tenant.getCnpj() );
        response.setCriadoEm( tenant.getCriadoEm() );
        response.setEmail( tenant.getEmail() );
        response.setEndereco( tenant.getEndereco() );
        response.setEstado( tenant.getEstado() );
        response.setId( tenant.getId() );
        response.setLogoUrl( tenant.getLogoUrl() );
        response.setNome( tenant.getNome() );
        response.setPlano( tenant.getPlano() );
        response.setTelefone( tenant.getTelefone() );

        return response;
    }

    @Override
    public TenantDTOs.ResumoResponse toResumoResponse(Tenant tenant) {
        if ( tenant == null ) {
            return null;
        }

        TenantDTOs.ResumoResponse resumoResponse = new TenantDTOs.ResumoResponse();

        resumoResponse.setAtivo( tenant.isAtivo() );
        resumoResponse.setCnpj( tenant.getCnpj() );
        resumoResponse.setId( tenant.getId() );
        resumoResponse.setNome( tenant.getNome() );
        resumoResponse.setPlano( tenant.getPlano() );

        return resumoResponse;
    }

    @Override
    public Tenant toEntity(TenantDTOs.CreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Tenant.TenantBuilder tenant = Tenant.builder();

        tenant.cep( request.getCep() );
        tenant.cidade( request.getCidade() );
        tenant.cnpj( request.getCnpj() );
        tenant.email( request.getEmail() );
        tenant.endereco( request.getEndereco() );
        tenant.estado( request.getEstado() );
        tenant.nome( request.getNome() );
        tenant.plano( request.getPlano() );
        tenant.telefone( request.getTelefone() );

        tenant.ativo( true );

        return tenant.build();
    }

    @Override
    public void updateFromRequest(TenantDTOs.UpdateRequest request, Tenant tenant) {
        if ( request == null ) {
            return;
        }

        if ( request.getCep() != null ) {
            tenant.setCep( request.getCep() );
        }
        if ( request.getCidade() != null ) {
            tenant.setCidade( request.getCidade() );
        }
        if ( request.getEmail() != null ) {
            tenant.setEmail( request.getEmail() );
        }
        if ( request.getEndereco() != null ) {
            tenant.setEndereco( request.getEndereco() );
        }
        if ( request.getEstado() != null ) {
            tenant.setEstado( request.getEstado() );
        }
        if ( request.getNome() != null ) {
            tenant.setNome( request.getNome() );
        }
        if ( request.getPlano() != null ) {
            tenant.setPlano( request.getPlano() );
        }
        if ( request.getTelefone() != null ) {
            tenant.setTelefone( request.getTelefone() );
        }
    }
}
