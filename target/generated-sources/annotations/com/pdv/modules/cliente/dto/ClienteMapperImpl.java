package com.pdv.modules.cliente.dto;

import com.pdv.modules.cliente.entity.Cliente;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-29T09:27:47-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.38.0.v20240524-2033, environment: Java 21.0.3 (Eclipse Adoptium)"
)
@Component
public class ClienteMapperImpl implements ClienteMapper {

    @Override
    public ClienteDTOs.Response toResponse(Cliente cliente) {
        if ( cliente == null ) {
            return null;
        }

        ClienteDTOs.Response response = new ClienteDTOs.Response();

        response.setAtivo( cliente.isAtivo() );
        response.setAtualizadoEm( cliente.getAtualizadoEm() );
        response.setCep( cliente.getCep() );
        response.setCidade( cliente.getCidade() );
        response.setCpfCnpj( cliente.getCpfCnpj() );
        response.setCriadoEm( cliente.getCriadoEm() );
        response.setDataNascimento( cliente.getDataNascimento() );
        response.setEmail( cliente.getEmail() );
        response.setEndereco( cliente.getEndereco() );
        response.setEstado( cliente.getEstado() );
        response.setId( cliente.getId() );
        response.setLimiteCredito( cliente.getLimiteCredito() );
        response.setNome( cliente.getNome() );
        response.setObservacao( cliente.getObservacao() );
        response.setSaldoDevedor( cliente.getSaldoDevedor() );
        response.setTelefone( cliente.getTelefone() );

        response.setCreditoDisponivel( cliente.getCreditoDisponivel() );

        return response;
    }

    @Override
    public ClienteDTOs.ResumoResponse toResumoResponse(Cliente cliente) {
        if ( cliente == null ) {
            return null;
        }

        ClienteDTOs.ResumoResponse resumoResponse = new ClienteDTOs.ResumoResponse();

        resumoResponse.setAtivo( cliente.isAtivo() );
        resumoResponse.setCpfCnpj( cliente.getCpfCnpj() );
        resumoResponse.setId( cliente.getId() );
        resumoResponse.setLimiteCredito( cliente.getLimiteCredito() );
        resumoResponse.setNome( cliente.getNome() );
        resumoResponse.setSaldoDevedor( cliente.getSaldoDevedor() );
        resumoResponse.setTelefone( cliente.getTelefone() );

        return resumoResponse;
    }

    @Override
    public Cliente toEntity(ClienteDTOs.CreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Cliente.ClienteBuilder cliente = Cliente.builder();

        cliente.cep( request.getCep() );
        cliente.cidade( request.getCidade() );
        cliente.cpfCnpj( request.getCpfCnpj() );
        cliente.dataNascimento( request.getDataNascimento() );
        cliente.email( request.getEmail() );
        cliente.endereco( request.getEndereco() );
        cliente.estado( request.getEstado() );
        cliente.limiteCredito( request.getLimiteCredito() );
        cliente.nome( request.getNome() );
        cliente.observacao( request.getObservacao() );
        cliente.telefone( request.getTelefone() );

        cliente.saldoDevedor( new BigDecimal( "0.0" ) );
        cliente.ativo( true );

        return cliente.build();
    }

    @Override
    public void updateFromRequest(ClienteDTOs.UpdateRequest request, Cliente cliente) {
        if ( request == null ) {
            return;
        }

        if ( request.getCep() != null ) {
            cliente.setCep( request.getCep() );
        }
        if ( request.getCidade() != null ) {
            cliente.setCidade( request.getCidade() );
        }
        if ( request.getDataNascimento() != null ) {
            cliente.setDataNascimento( request.getDataNascimento() );
        }
        if ( request.getEmail() != null ) {
            cliente.setEmail( request.getEmail() );
        }
        if ( request.getEndereco() != null ) {
            cliente.setEndereco( request.getEndereco() );
        }
        if ( request.getEstado() != null ) {
            cliente.setEstado( request.getEstado() );
        }
        if ( request.getLimiteCredito() != null ) {
            cliente.setLimiteCredito( request.getLimiteCredito() );
        }
        if ( request.getNome() != null ) {
            cliente.setNome( request.getNome() );
        }
        if ( request.getObservacao() != null ) {
            cliente.setObservacao( request.getObservacao() );
        }
        if ( request.getTelefone() != null ) {
            cliente.setTelefone( request.getTelefone() );
        }
    }
}
