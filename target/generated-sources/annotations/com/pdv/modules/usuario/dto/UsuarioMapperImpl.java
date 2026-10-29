package com.pdv.modules.usuario.dto;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.usuario.entity.Usuario;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-29T09:27:47-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.38.0.v20240524-2033, environment: Java 21.0.3 (Eclipse Adoptium)"
)
@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public UsuarioDTOs.Response toResponse(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UsuarioDTOs.Response response = new UsuarioDTOs.Response();

        response.setTenantId( usuarioTenantId( usuario ) );
        response.setAtivo( usuario.isAtivo() );
        response.setAtualizadoEm( usuario.getAtualizadoEm() );
        response.setCriadoEm( usuario.getCriadoEm() );
        response.setDescontoMax( usuario.getDescontoMax() );
        response.setEmail( usuario.getEmail() );
        response.setId( usuario.getId() );
        response.setNome( usuario.getNome() );
        response.setRole( usuario.getRole() );
        response.setUltimoLogin( usuario.getUltimoLogin() );

        response.setPossuiPin( usuario.getPinHash() != null );

        return response;
    }

    @Override
    public UsuarioDTOs.ResumoResponse toResumoResponse(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UsuarioDTOs.ResumoResponse resumoResponse = new UsuarioDTOs.ResumoResponse();

        resumoResponse.setAtivo( usuario.isAtivo() );
        resumoResponse.setEmail( usuario.getEmail() );
        resumoResponse.setId( usuario.getId() );
        resumoResponse.setNome( usuario.getNome() );
        resumoResponse.setRole( usuario.getRole() );

        return resumoResponse;
    }

    @Override
    public Usuario toEntity(UsuarioDTOs.CreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Usuario.UsuarioBuilder usuario = Usuario.builder();

        usuario.descontoMax( request.getDescontoMax() );
        usuario.email( request.getEmail() );
        usuario.nome( request.getNome() );
        usuario.role( request.getRole() );

        usuario.ativo( true );

        return usuario.build();
    }

    @Override
    public void updateFromRequest(UsuarioDTOs.UpdateRequest request, Usuario usuario) {
        if ( request == null ) {
            return;
        }

        if ( request.getDescontoMax() != null ) {
            usuario.setDescontoMax( request.getDescontoMax() );
        }
        if ( request.getEmail() != null ) {
            usuario.setEmail( request.getEmail() );
        }
        if ( request.getNome() != null ) {
            usuario.setNome( request.getNome() );
        }
        if ( request.getRole() != null ) {
            usuario.setRole( request.getRole() );
        }
    }

    private UUID usuarioTenantId(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }
        Tenant tenant = usuario.getTenant();
        if ( tenant == null ) {
            return null;
        }
        UUID id = tenant.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
