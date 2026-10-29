package com.pdv.modules.produto.dto;

import com.pdv.modules.fornecedor.entity.Fornecedor;
import com.pdv.modules.produto.entity.CategoriaProduto;
import com.pdv.modules.produto.entity.HistoricoPreco;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.entity.ProdutoComponente;
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
public class ProdutoMapperImpl implements ProdutoMapper {

    @Override
    public ProdutoDTOs.Response toResponse(Produto produto) {
        if ( produto == null ) {
            return null;
        }

        ProdutoDTOs.Response response = new ProdutoDTOs.Response();

        response.setTenantId( produtoTenantId( produto ) );
        response.setCategoria( toCategoriaResumo( produto.getCategoria() ) );
        response.setFornecedor( toFornecedorResumo( produto.getFornecedor() ) );
        response.setComponentes( produtoComponenteListToComponenteResponseList( produto.getComponentes() ) );
        response.setAtivo( produto.isAtivo() );
        response.setAtualizadoEm( produto.getAtualizadoEm() );
        response.setCodigoBarras( produto.getCodigoBarras() );
        response.setComposto( produto.isComposto() );
        response.setCriadoEm( produto.getCriadoEm() );
        response.setDescricao( produto.getDescricao() );
        response.setEstoqueAtual( produto.getEstoqueAtual() );
        response.setEstoqueMinimo( produto.getEstoqueMinimo() );
        response.setFotoUrl( produto.getFotoUrl() );
        response.setId( produto.getId() );
        response.setMargemLucro( produto.getMargemLucro() );
        response.setNcm( produto.getNcm() );
        response.setNome( produto.getNome() );
        response.setPrecoCusto( produto.getPrecoCusto() );
        response.setPrecoVenda( produto.getPrecoVenda() );
        response.setSku( produto.getSku() );
        response.setUnidade( produto.getUnidade() );

        response.setEstoqueAbaixoMinimo( produto.estoqueAbaixoMinimo() );

        return response;
    }

    @Override
    public ProdutoDTOs.ResumoResponse toResumoResponse(Produto produto) {
        if ( produto == null ) {
            return null;
        }

        ProdutoDTOs.ResumoResponse resumoResponse = new ProdutoDTOs.ResumoResponse();

        resumoResponse.setCategoria( toCategoriaResumo( produto.getCategoria() ) );
        resumoResponse.setFornecedor( toFornecedorResumo( produto.getFornecedor() ) );
        resumoResponse.setAtivo( produto.isAtivo() );
        resumoResponse.setCodigoBarras( produto.getCodigoBarras() );
        resumoResponse.setEstoqueAtual( produto.getEstoqueAtual() );
        resumoResponse.setEstoqueMinimo( produto.getEstoqueMinimo() );
        resumoResponse.setId( produto.getId() );
        resumoResponse.setNome( produto.getNome() );
        resumoResponse.setPrecoVenda( produto.getPrecoVenda() );
        resumoResponse.setSku( produto.getSku() );
        resumoResponse.setUnidade( produto.getUnidade() );

        resumoResponse.setEstoqueAbaixoMinimo( produto.estoqueAbaixoMinimo() );

        return resumoResponse;
    }

    @Override
    public ProdutoDTOs.CategoriaResumo toCategoriaResumo(CategoriaProduto categoria) {
        if ( categoria == null ) {
            return null;
        }

        ProdutoDTOs.CategoriaResumo categoriaResumo = new ProdutoDTOs.CategoriaResumo();

        categoriaResumo.setId( categoria.getId() );
        categoriaResumo.setNome( categoria.getNome() );

        return categoriaResumo;
    }

    @Override
    public ProdutoDTOs.FornecedorResumo toFornecedorResumo(Fornecedor fornecedor) {
        if ( fornecedor == null ) {
            return null;
        }

        ProdutoDTOs.FornecedorResumo fornecedorResumo = new ProdutoDTOs.FornecedorResumo();

        fornecedorResumo.setId( fornecedor.getId() );
        fornecedorResumo.setNomeFantasia( fornecedor.getNomeFantasia() );
        fornecedorResumo.setRazaoSocial( fornecedor.getRazaoSocial() );
        fornecedorResumo.setTelefone( fornecedor.getTelefone() );

        fornecedorResumo.setWhatsappUrl( buildWhatsappUrl(fornecedor.getTelefone()) );

        return fornecedorResumo;
    }

    @Override
    public ProdutoDTOs.ComponenteResponse toComponenteResponse(ProdutoComponente componente) {
        if ( componente == null ) {
            return null;
        }

        ProdutoDTOs.ComponenteResponse componenteResponse = new ProdutoDTOs.ComponenteResponse();

        componenteResponse.setId( componente.getId() );
        componenteResponse.setProdutoFilhoId( componenteProdutoFilhoId( componente ) );
        componenteResponse.setNomeProdutoFilho( componenteProdutoFilhoNome( componente ) );
        componenteResponse.setQuantidade( componente.getQuantidade() );

        return componenteResponse;
    }

    @Override
    public Produto toEntity(ProdutoDTOs.CreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Produto.ProdutoBuilder produto = Produto.builder();

        produto.codigoBarras( request.getCodigoBarras() );
        produto.composto( request.isComposto() );
        produto.descricao( request.getDescricao() );
        produto.estoqueAtual( request.getEstoqueAtual() );
        produto.estoqueMinimo( request.getEstoqueMinimo() );
        produto.ncm( request.getNcm() );
        produto.nome( request.getNome() );
        produto.precoCusto( request.getPrecoCusto() );
        produto.precoVenda( request.getPrecoVenda() );
        produto.sku( request.getSku() );
        produto.unidade( request.getUnidade() );

        produto.ativo( true );

        return produto.build();
    }

    @Override
    public void updateFromRequest(ProdutoDTOs.UpdateRequest request, Produto produto) {
        if ( request == null ) {
            return;
        }

        if ( request.getCodigoBarras() != null ) {
            produto.setCodigoBarras( request.getCodigoBarras() );
        }
        if ( request.getDescricao() != null ) {
            produto.setDescricao( request.getDescricao() );
        }
        if ( request.getEstoqueMinimo() != null ) {
            produto.setEstoqueMinimo( request.getEstoqueMinimo() );
        }
        if ( request.getNcm() != null ) {
            produto.setNcm( request.getNcm() );
        }
        if ( request.getNome() != null ) {
            produto.setNome( request.getNome() );
        }
        if ( request.getPrecoCusto() != null ) {
            produto.setPrecoCusto( request.getPrecoCusto() );
        }
        if ( request.getPrecoVenda() != null ) {
            produto.setPrecoVenda( request.getPrecoVenda() );
        }
        if ( request.getSku() != null ) {
            produto.setSku( request.getSku() );
        }
        if ( request.getUnidade() != null ) {
            produto.setUnidade( request.getUnidade() );
        }
    }

    @Override
    public ProdutoDTOs.HistoricoPrecoResponse toHistoricoResponse(HistoricoPreco historico) {
        if ( historico == null ) {
            return null;
        }

        ProdutoDTOs.HistoricoPrecoResponse historicoPrecoResponse = new ProdutoDTOs.HistoricoPrecoResponse();

        historicoPrecoResponse.setNomeUsuario( historicoUsuarioNome( historico ) );
        historicoPrecoResponse.setCriadoEm( historico.getCriadoEm() );
        historicoPrecoResponse.setId( historico.getId() );
        historicoPrecoResponse.setPrecoCusto( historico.getPrecoCusto() );
        historicoPrecoResponse.setPrecoVenda( historico.getPrecoVenda() );

        return historicoPrecoResponse;
    }

    @Override
    public CategoriaDTOs.Response toCategoriaResponse(CategoriaProduto categoria) {
        if ( categoria == null ) {
            return null;
        }

        CategoriaDTOs.Response response = new CategoriaDTOs.Response();

        response.setPaiId( categoriaPaiId( categoria ) );
        response.setNomePai( categoriaPaiNome( categoria ) );
        response.setAtivo( categoria.isAtivo() );
        response.setCriadoEm( categoria.getCriadoEm() );
        response.setId( categoria.getId() );
        response.setNome( categoria.getNome() );

        return response;
    }

    @Override
    public CategoriaDTOs.ResumoResponse toCategoriaResumoResponse(CategoriaProduto categoria) {
        if ( categoria == null ) {
            return null;
        }

        CategoriaDTOs.ResumoResponse resumoResponse = new CategoriaDTOs.ResumoResponse();

        resumoResponse.setPaiId( categoriaPaiId( categoria ) );
        resumoResponse.setAtivo( categoria.isAtivo() );
        resumoResponse.setId( categoria.getId() );
        resumoResponse.setNome( categoria.getNome() );

        return resumoResponse;
    }

    @Override
    public CategoriaProduto toCategoriaEntity(CategoriaDTOs.CreateRequest request) {
        if ( request == null ) {
            return null;
        }

        CategoriaProduto.CategoriaProdutoBuilder categoriaProduto = CategoriaProduto.builder();

        categoriaProduto.nome( request.getNome() );

        categoriaProduto.ativo( true );

        return categoriaProduto.build();
    }

    private UUID produtoTenantId(Produto produto) {
        if ( produto == null ) {
            return null;
        }
        Tenant tenant = produto.getTenant();
        if ( tenant == null ) {
            return null;
        }
        UUID id = tenant.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected List<ProdutoDTOs.ComponenteResponse> produtoComponenteListToComponenteResponseList(List<ProdutoComponente> list) {
        if ( list == null ) {
            return null;
        }

        List<ProdutoDTOs.ComponenteResponse> list1 = new ArrayList<ProdutoDTOs.ComponenteResponse>( list.size() );
        for ( ProdutoComponente produtoComponente : list ) {
            list1.add( toComponenteResponse( produtoComponente ) );
        }

        return list1;
    }

    private UUID componenteProdutoFilhoId(ProdutoComponente produtoComponente) {
        if ( produtoComponente == null ) {
            return null;
        }
        Produto produtoFilho = produtoComponente.getProdutoFilho();
        if ( produtoFilho == null ) {
            return null;
        }
        UUID id = produtoFilho.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String componenteProdutoFilhoNome(ProdutoComponente produtoComponente) {
        if ( produtoComponente == null ) {
            return null;
        }
        Produto produtoFilho = produtoComponente.getProdutoFilho();
        if ( produtoFilho == null ) {
            return null;
        }
        String nome = produtoFilho.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }

    private String historicoUsuarioNome(HistoricoPreco historicoPreco) {
        if ( historicoPreco == null ) {
            return null;
        }
        Usuario usuario = historicoPreco.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        String nome = usuario.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }

    private UUID categoriaPaiId(CategoriaProduto categoriaProduto) {
        if ( categoriaProduto == null ) {
            return null;
        }
        CategoriaProduto pai = categoriaProduto.getPai();
        if ( pai == null ) {
            return null;
        }
        UUID id = pai.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String categoriaPaiNome(CategoriaProduto categoriaProduto) {
        if ( categoriaProduto == null ) {
            return null;
        }
        CategoriaProduto pai = categoriaProduto.getPai();
        if ( pai == null ) {
            return null;
        }
        String nome = pai.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }
}
