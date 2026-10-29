package com.pdv.modules.fornecedor.dto;

import com.pdv.modules.fornecedor.entity.Compra;
import com.pdv.modules.fornecedor.entity.Fornecedor;
import com.pdv.modules.fornecedor.entity.ItemCompra;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.usuario.entity.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-29T09:27:47-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.38.0.v20240524-2033, environment: Java 21.0.3 (Eclipse Adoptium)"
)
@Component
public class FornecedorMapperImpl implements FornecedorMapper {

    @Override
    public FornecedorDTOs.Response toResponse(Fornecedor fornecedor) {
        if ( fornecedor == null ) {
            return null;
        }

        FornecedorDTOs.Response response = new FornecedorDTOs.Response();

        response.setAtivo( fornecedor.isAtivo() );
        response.setAtualizadoEm( fornecedor.getAtualizadoEm() );
        response.setCep( fornecedor.getCep() );
        response.setCidade( fornecedor.getCidade() );
        response.setCnpj( fornecedor.getCnpj() );
        response.setContato( fornecedor.getContato() );
        response.setCriadoEm( fornecedor.getCriadoEm() );
        response.setEmail( fornecedor.getEmail() );
        response.setEndereco( fornecedor.getEndereco() );
        response.setEstado( fornecedor.getEstado() );
        response.setId( fornecedor.getId() );
        response.setNomeFantasia( fornecedor.getNomeFantasia() );
        response.setObservacao( fornecedor.getObservacao() );
        response.setRazaoSocial( fornecedor.getRazaoSocial() );
        response.setTelefone( fornecedor.getTelefone() );

        return response;
    }

    @Override
    public FornecedorDTOs.ResumoResponse toResumoResponse(Fornecedor fornecedor) {
        if ( fornecedor == null ) {
            return null;
        }

        FornecedorDTOs.ResumoResponse resumoResponse = new FornecedorDTOs.ResumoResponse();

        resumoResponse.setAtivo( fornecedor.isAtivo() );
        resumoResponse.setCnpj( fornecedor.getCnpj() );
        resumoResponse.setId( fornecedor.getId() );
        resumoResponse.setNomeFantasia( fornecedor.getNomeFantasia() );
        resumoResponse.setRazaoSocial( fornecedor.getRazaoSocial() );
        resumoResponse.setTelefone( fornecedor.getTelefone() );

        return resumoResponse;
    }

    @Override
    public Fornecedor toEntity(FornecedorDTOs.CreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Fornecedor.FornecedorBuilder fornecedor = Fornecedor.builder();

        fornecedor.cep( request.getCep() );
        fornecedor.cidade( request.getCidade() );
        fornecedor.cnpj( request.getCnpj() );
        fornecedor.contato( request.getContato() );
        fornecedor.email( request.getEmail() );
        fornecedor.endereco( request.getEndereco() );
        fornecedor.estado( request.getEstado() );
        fornecedor.nomeFantasia( request.getNomeFantasia() );
        fornecedor.observacao( request.getObservacao() );
        fornecedor.razaoSocial( request.getRazaoSocial() );
        fornecedor.telefone( request.getTelefone() );

        fornecedor.ativo( true );

        return fornecedor.build();
    }

    @Override
    public void updateFromRequest(FornecedorDTOs.UpdateRequest request, Fornecedor fornecedor) {
        if ( request == null ) {
            return;
        }

        if ( request.getCep() != null ) {
            fornecedor.setCep( request.getCep() );
        }
        if ( request.getCidade() != null ) {
            fornecedor.setCidade( request.getCidade() );
        }
        if ( request.getContato() != null ) {
            fornecedor.setContato( request.getContato() );
        }
        if ( request.getEmail() != null ) {
            fornecedor.setEmail( request.getEmail() );
        }
        if ( request.getEndereco() != null ) {
            fornecedor.setEndereco( request.getEndereco() );
        }
        if ( request.getEstado() != null ) {
            fornecedor.setEstado( request.getEstado() );
        }
        if ( request.getNomeFantasia() != null ) {
            fornecedor.setNomeFantasia( request.getNomeFantasia() );
        }
        if ( request.getObservacao() != null ) {
            fornecedor.setObservacao( request.getObservacao() );
        }
        if ( request.getRazaoSocial() != null ) {
            fornecedor.setRazaoSocial( request.getRazaoSocial() );
        }
        if ( request.getTelefone() != null ) {
            fornecedor.setTelefone( request.getTelefone() );
        }
    }

    @Override
    public FornecedorDTOs.CompraResponse toCompraResponse(Compra compra) {
        if ( compra == null ) {
            return null;
        }

        FornecedorDTOs.CompraResponse compraResponse = new FornecedorDTOs.CompraResponse();

        compraResponse.setTenantId( compraTenantId( compra ) );
        compraResponse.setFornecedor( toFornecedorResumo( compra.getFornecedor() ) );
        compraResponse.setNomeUsuario( compraUsuarioNome( compra ) );
        compraResponse.setItens( itemCompraListToItemCompraResponseList( compra.getItens() ) );
        compraResponse.setCriadoEm( compra.getCriadoEm() );
        compraResponse.setDtCompra( compra.getDtCompra() );
        compraResponse.setDtEntrega( compra.getDtEntrega() );
        compraResponse.setId( compra.getId() );
        compraResponse.setNumeroNota( compra.getNumeroNota() );
        compraResponse.setObservacao( compra.getObservacao() );
        compraResponse.setStatus( compra.getStatus() );
        compraResponse.setTotal( compra.getTotal() );

        return compraResponse;
    }

    @Override
    public FornecedorDTOs.CompraResumoResponse toCompraResumoResponse(Compra compra) {
        if ( compra == null ) {
            return null;
        }

        FornecedorDTOs.CompraResumoResponse compraResumoResponse = new FornecedorDTOs.CompraResumoResponse();

        compraResumoResponse.setFornecedor( toFornecedorResumo( compra.getFornecedor() ) );
        compraResumoResponse.setDtCompra( compra.getDtCompra() );
        compraResumoResponse.setId( compra.getId() );
        compraResumoResponse.setNumeroNota( compra.getNumeroNota() );
        compraResumoResponse.setStatus( compra.getStatus() );
        compraResumoResponse.setTotal( compra.getTotal() );

        return compraResumoResponse;
    }

    @Override
    public FornecedorDTOs.ItemCompraResponse toItemCompraResponse(ItemCompra item) {
        if ( item == null ) {
            return null;
        }

        FornecedorDTOs.ItemCompraResponse itemCompraResponse = new FornecedorDTOs.ItemCompraResponse();

        itemCompraResponse.setId( item.getId() );
        itemCompraResponse.setProdutoId( itemProdutoId( item ) );
        itemCompraResponse.setNomeProduto( itemProdutoNome( item ) );
        itemCompraResponse.setPrecoCusto( item.getPrecoCusto() );
        itemCompraResponse.setQuantidade( item.getQuantidade() );
        itemCompraResponse.setTotalItem( item.getTotalItem() );

        return itemCompraResponse;
    }

    @Override
    public FornecedorDTOs.FornecedorResumo toFornecedorResumo(Fornecedor fornecedor) {
        if ( fornecedor == null ) {
            return null;
        }

        FornecedorDTOs.FornecedorResumo fornecedorResumo = new FornecedorDTOs.FornecedorResumo();

        fornecedorResumo.setId( fornecedor.getId() );
        fornecedorResumo.setNomeFantasia( fornecedor.getNomeFantasia() );
        fornecedorResumo.setCnpj( fornecedor.getCnpj() );
        fornecedorResumo.setRazaoSocial( fornecedor.getRazaoSocial() );

        return fornecedorResumo;
    }

    private UUID compraTenantId(Compra compra) {
        if ( compra == null ) {
            return null;
        }
        Tenant tenant = compra.getTenant();
        if ( tenant == null ) {
            return null;
        }
        UUID id = tenant.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String compraUsuarioNome(Compra compra) {
        if ( compra == null ) {
            return null;
        }
        Usuario usuario = compra.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        String nome = usuario.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }

    protected List<FornecedorDTOs.ItemCompraResponse> itemCompraListToItemCompraResponseList(List<ItemCompra> list) {
        if ( list == null ) {
            return null;
        }

        List<FornecedorDTOs.ItemCompraResponse> list1 = new ArrayList<FornecedorDTOs.ItemCompraResponse>( list.size() );
        for ( ItemCompra itemCompra : list ) {
            list1.add( toItemCompraResponse( itemCompra ) );
        }

        return list1;
    }

    private UUID itemProdutoId(ItemCompra itemCompra) {
        if ( itemCompra == null ) {
            return null;
        }
        Produto produto = itemCompra.getProduto();
        if ( produto == null ) {
            return null;
        }
        UUID id = produto.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String itemProdutoNome(ItemCompra itemCompra) {
        if ( itemCompra == null ) {
            return null;
        }
        Produto produto = itemCompra.getProduto();
        if ( produto == null ) {
            return null;
        }
        String nome = produto.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }
}
