package com.pdv.modules.estoque.service;

import com.pdv.modules.estoque.dto.EstoqueDTOs;
import com.pdv.modules.estoque.entity.MovimentacaoEstoque;
import com.pdv.modules.estoque.entity.TipoMovimentacaoEstoque;
import com.pdv.modules.estoque.repository.MovimentacaoEstoqueRepository;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstoqueService")
class EstoqueServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    private UUID tenantId;
    private UUID usuarioId;
    private UUID produtoId;
    private Tenant tenant;
    private Produto produto;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        produtoId = UUID.randomUUID();

        tenant = Tenant.builder().id(tenantId).nome("Loja").ativo(true).plano("BASICO").build();

        produto = Produto.builder()
                .id(produtoId)
                .tenant(tenant)
                .nome("Produto Teste")
                .unidade("UN")
                .precoCusto(BigDecimal.valueOf(5))
                .precoVenda(BigDecimal.valueOf(10))
                .estoqueAtual(BigDecimal.valueOf(50))
                .estoqueMinimo(BigDecimal.valueOf(5))
                .ativo(true)
                .composto(false)
                .componentes(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("ajustar()")
    class Ajustar {

        @Test
        @DisplayName("deve aumentar estoque em ajuste de ENTRADA")
        void deveAumentarEstoque_emEntrada() {
            EstoqueDTOs.AjusteRequest request = new EstoqueDTOs.AjusteRequest();
            request.setProdutoId(produtoId);
            request.setTipo(TipoMovimentacaoEstoque.ENTRADA);
            request.setQuantidade(BigDecimal.valueOf(10));
            request.setMotivo("Reposição manual");

            when(produtoRepository.findByIdAndTenantId(produtoId, tenantId))
                    .thenReturn(Optional.of(produto));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByIdAndTenantId(usuarioId, tenantId))
                    .thenReturn(Optional.empty());
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(i -> i.getArgument(0));

            EstoqueDTOs.MovimentacaoResponse response =
                    estoqueService.ajustar(tenantId, usuarioId, request);

            assertThat(produto.getEstoqueAtual()).isEqualByComparingTo(BigDecimal.valueOf(60));
            assertThat(response.getQuantidadeAnterior()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(response.getQuantidadePosterior()).isEqualByComparingTo(BigDecimal.valueOf(60));
        }

        @Test
        @DisplayName("deve reduzir estoque em ajuste de SAIDA")
        void deveReduzirEstoque_emSaida() {
            EstoqueDTOs.AjusteRequest request = new EstoqueDTOs.AjusteRequest();
            request.setProdutoId(produtoId);
            request.setTipo(TipoMovimentacaoEstoque.SAIDA);
            request.setQuantidade(BigDecimal.valueOf(10));
            request.setMotivo("Saída manual");

            when(produtoRepository.findByIdAndTenantId(produtoId, tenantId))
                    .thenReturn(Optional.of(produto));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByIdAndTenantId(usuarioId, tenantId))
                    .thenReturn(Optional.empty());
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(i -> i.getArgument(0));

            estoqueService.ajustar(tenantId, usuarioId, request);

            assertThat(produto.getEstoqueAtual()).isEqualByComparingTo(BigDecimal.valueOf(40));
        }

        @Test
        @DisplayName("deve lançar BusinessException quando estoque insuficiente para SAIDA")
        void deveLancarBusiness_quandoEstoqueInsuficienteParaSaida() {
            produto.setEstoqueAtual(BigDecimal.valueOf(3));

            EstoqueDTOs.AjusteRequest request = new EstoqueDTOs.AjusteRequest();
            request.setProdutoId(produtoId);
            request.setTipo(TipoMovimentacaoEstoque.SAIDA);
            request.setQuantidade(BigDecimal.valueOf(10));
            request.setMotivo("Teste");

            when(produtoRepository.findByIdAndTenantId(produtoId, tenantId))
                    .thenReturn(Optional.of(produto));

            assertThatThrownBy(() -> estoqueService.ajustar(tenantId, usuarioId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Estoque insuficiente");
        }

        @Test
        @DisplayName("deve substituir estoque em ajuste INVENTARIO")
        void deveSubstituirEstoque_emInventario() {
            EstoqueDTOs.AjusteRequest request = new EstoqueDTOs.AjusteRequest();
            request.setProdutoId(produtoId);
            request.setTipo(TipoMovimentacaoEstoque.INVENTARIO);
            request.setQuantidade(BigDecimal.valueOf(35)); // nova quantidade
            request.setMotivo("Inventário periódico");

            when(produtoRepository.findByIdAndTenantId(produtoId, tenantId))
                    .thenReturn(Optional.of(produto));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByIdAndTenantId(usuarioId, tenantId))
                    .thenReturn(Optional.empty());
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(i -> i.getArgument(0));

            estoqueService.ajustar(tenantId, usuarioId, request);

            assertThat(produto.getEstoqueAtual()).isEqualByComparingTo(BigDecimal.valueOf(35));
        }
    }
}
