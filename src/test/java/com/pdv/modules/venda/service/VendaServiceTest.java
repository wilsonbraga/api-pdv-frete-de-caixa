package com.pdv.modules.venda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pdv.modules.caixa.entity.Caixa;
import com.pdv.modules.caixa.entity.StatusCaixa;
import com.pdv.modules.caixa.repository.CaixaRepository;
import com.pdv.modules.caixa.service.CaixaService;
import com.pdv.modules.cliente.repository.ClienteRepository;
import com.pdv.modules.cliente.service.ClienteService;
import com.pdv.modules.estoque.service.EstoqueService;
import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.RoleUsuario;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.modules.venda.dto.VendaDTOs;
import com.pdv.modules.venda.entity.FormaPagamento;
import com.pdv.modules.venda.entity.ItemVenda;
import com.pdv.modules.venda.entity.PagamentoVenda;
import com.pdv.modules.venda.entity.StatusPagamento;
import com.pdv.modules.venda.entity.StatusVenda;
import com.pdv.modules.venda.entity.Venda;
import com.pdv.modules.venda.repository.VendaRepository;
import com.pdv.shared.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendaService")
class VendaServiceTest {

    @Mock private VendaRepository vendaRepository;
    @Mock private CaixaRepository caixaRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private EstoqueService estoqueService;
    @Mock private CaixaService caixaService;
    @Mock private ClienteService clienteService;
    @Mock private NumeracaoVendaService numeracaoVendaService;

    @InjectMocks
    private VendaService vendaService;

    private UUID tenantId;
    private UUID operadorId;
    private UUID caixaId;
    private Tenant tenant;
    private Usuario operador;
    private PDV pdv;
    private Caixa caixa;
    private Produto produto;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        operadorId = UUID.randomUUID();
        caixaId = UUID.randomUUID();

        tenant = Tenant.builder().id(tenantId).nome("Loja").ativo(true).plano("BASICO").build();

        operador = Usuario.builder()
                .id(operadorId)
                .tenant(tenant)
                .nome("Operador")
                .email("op@teste.com")
                .role(RoleUsuario.OPERADOR)
                .ativo(true)
                .descontoMax(10.0)
                .build();

        pdv = PDV.builder().id(UUID.randomUUID()).tenant(tenant).nome("Caixa 01").ativo(true).build();

        caixa = Caixa.builder()
                .id(caixaId)
                .tenant(tenant)
                .pdv(pdv)
                .operador(operador)
                .dtAbertura(LocalDateTime.now())
                .valorInicial(BigDecimal.valueOf(100))
                .totalSangria(BigDecimal.ZERO)
                .totalSuprimento(BigDecimal.ZERO)
                .status(StatusCaixa.ABERTO)
                .build();

        produto = Produto.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .nome("Produto Teste")
                .codigoBarras("789001")
                .unidade("UN")
                .precoCusto(BigDecimal.valueOf(5.00))
                .precoVenda(BigDecimal.valueOf(10.00))
                .estoqueAtual(BigDecimal.valueOf(100))
                .estoqueMinimo(BigDecimal.valueOf(5))
                .ativo(true)
                .composto(false)
                .componentes(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("iniciar()")
    class Iniciar {

        @Test
        @DisplayName("deve criar venda com status ABERTA quando caixa está aberto")
        void deveCriarVenda_quandoCaixaAberto() {
            VendaDTOs.IniciarRequest request = new VendaDTOs.IniciarRequest();
            request.setCaixaId(caixaId);

            when(caixaRepository.findByIdAndTenantId(caixaId, tenantId))
                    .thenReturn(Optional.of(caixa));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(operador));
            when(numeracaoVendaService.gerarNumero(tenantId, "Caixa 01"))
                    .thenReturn("TENANT-CX0101-000001");
            when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> {
                Venda v = inv.getArgument(0);
                v = Venda.builder()
                        .id(UUID.randomUUID())
                        .tenant(tenant)
                        .caixa(caixa)
                        .pdv(pdv)
                        .operador(operador)
                        .numeroVenda("TENANT-CX0101-000001")
                        .dtVenda(LocalDateTime.now())
                        .subtotal(BigDecimal.ZERO)
                        .desconto(BigDecimal.ZERO)
                        .total(BigDecimal.ZERO)
                        .status(StatusVenda.ABERTA)
                        .sincronizado(false)
                        .itens(new LinkedHashSet<>())
                        .pagamentos(new LinkedHashSet<>())
                        .build();
                return v;
            });

            VendaDTOs.Response response = vendaService.iniciar(tenantId, operadorId, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusVenda.ABERTA);
            assertThat(response.getNumeroVenda()).isEqualTo("TENANT-CX0101-000001");
        }

        @Test
        @DisplayName("deve lançar BusinessException quando caixa está fechado")
        void deveLancarBusiness_quandoCaixaFechado() {
            caixa.setStatus(StatusCaixa.FECHADO);
            VendaDTOs.IniciarRequest request = new VendaDTOs.IniciarRequest();
            request.setCaixaId(caixaId);

            when(caixaRepository.findByIdAndTenantId(caixaId, tenantId))
                    .thenReturn(Optional.of(caixa));

            assertThatThrownBy(() -> vendaService.iniciar(tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Caixa está fechado");
        }
    }

    @Nested
    @DisplayName("adicionarItem()")
    class AdicionarItem {

        @Test
        @DisplayName("deve adicionar item quando produto tem estoque suficiente")
        void deveAdicionarItem_quandoEstoqueSuficiente() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaAberta(vendaId);

            VendaDTOs.AdicionarItemRequest request = new VendaDTOs.AdicionarItemRequest();
            request.setProdutoId(produto.getId());
            request.setQuantidade(BigDecimal.valueOf(2));
            request.setDescontoItem(BigDecimal.ZERO);

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));
            when(produtoRepository.findByIdAndTenantId(produto.getId(), tenantId))
                    .thenReturn(Optional.of(produto));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(operador));
            when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));

            VendaDTOs.Response response = vendaService.adicionarItem(
                    vendaId, tenantId, operadorId, request);

            assertThat(response).isNotNull();
            assertThat(response.getItens()).hasSize(1);
            assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        }

        @Test
        @DisplayName("deve lançar BusinessException quando estoque insuficiente")
        void deveLancarBusiness_quandoEstoqueInsuficiente() {
            produto.setEstoqueAtual(BigDecimal.valueOf(1));
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaAberta(vendaId);

            VendaDTOs.AdicionarItemRequest request = new VendaDTOs.AdicionarItemRequest();
            request.setProdutoId(produto.getId());
            request.setQuantidade(BigDecimal.valueOf(5));
            request.setDescontoItem(BigDecimal.ZERO);

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));
            when(produtoRepository.findByIdAndTenantId(produto.getId(), tenantId))
                    .thenReturn(Optional.of(produto));

            assertThatThrownBy(() -> vendaService.adicionarItem(
                    vendaId, tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Estoque insuficiente");
        }

        @Test
        @DisplayName("deve lançar BusinessException quando desconto excede limite do operador")
        void deveLancarBusiness_quandoDescontoExcedeLimite() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaAberta(vendaId);

            VendaDTOs.AdicionarItemRequest request = new VendaDTOs.AdicionarItemRequest();
            request.setProdutoId(produto.getId());
            request.setQuantidade(BigDecimal.valueOf(1));
            request.setDescontoItem(BigDecimal.valueOf(8.00)); // 80% de desconto, limite é 10%

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));
            when(produtoRepository.findByIdAndTenantId(produto.getId(), tenantId))
                    .thenReturn(Optional.of(produto));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(operador));

            assertThatThrownBy(() -> vendaService.adicionarItem(
                    vendaId, tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("excede o limite");
        }
    }

    @Nested
    @DisplayName("finalizar()")
    class Finalizar {

        @Test
        @DisplayName("deve finalizar venda com pagamento em dinheiro e calcular troco")
        void deveFinalizarVenda_comDinheiro() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaComItem(vendaId, BigDecimal.valueOf(20.00));

            VendaDTOs.PagamentoRequest pag = new VendaDTOs.PagamentoRequest();
            pag.setForma(FormaPagamento.DINHEIRO);
            pag.setValor(BigDecimal.valueOf(50.00));

            VendaDTOs.FinalizarRequest request = new VendaDTOs.FinalizarRequest();
            request.setPagamentos(List.of(pag));

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));
            when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));

            VendaDTOs.Response response = vendaService.finalizar(
                    vendaId, tenantId, operadorId, request);

            assertThat(response.getStatus()).isEqualTo(StatusVenda.FINALIZADA);
            assertThat(response.getTotalPago()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
            verify(estoqueService).baixarEstoqueVenda(
                    eq(tenantId), eq(operadorId), eq(vendaId), any(), any());
            verify(caixaService).adicionarTotalVenda(
                    eq(caixaId), eq(FormaPagamento.DINHEIRO), eq(BigDecimal.valueOf(50.00)));
        }

        @Test
        @DisplayName("deve lançar BusinessException quando valor pago é insuficiente")
        void deveLancarBusiness_quandoValorInsuficiente() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaComItem(vendaId, BigDecimal.valueOf(50.00));

            VendaDTOs.PagamentoRequest pag = new VendaDTOs.PagamentoRequest();
            pag.setForma(FormaPagamento.DINHEIRO);
            pag.setValor(BigDecimal.valueOf(30.00));

            VendaDTOs.FinalizarRequest request = new VendaDTOs.FinalizarRequest();
            request.setPagamentos(List.of(pag));

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));

            assertThatThrownBy(() -> vendaService.finalizar(
                    vendaId, tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("insuficiente");
        }

        @Test
        @DisplayName("deve lançar BusinessException quando fiado sem cliente selecionado")
        void deveLancarBusiness_quandoFiadoSemCliente() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaComItem(vendaId, BigDecimal.valueOf(20.00));

            VendaDTOs.PagamentoRequest pag = new VendaDTOs.PagamentoRequest();
            pag.setForma(FormaPagamento.FIADO);
            pag.setValor(BigDecimal.valueOf(20.00));

            VendaDTOs.FinalizarRequest request = new VendaDTOs.FinalizarRequest();
            request.setPagamentos(List.of(pag));

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));

            assertThatThrownBy(() -> vendaService.finalizar(
                    vendaId, tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("fiado exige cliente");
        }
    }

    @Nested
    @DisplayName("cancelar()")
    class Cancelar {

        @Test
        @DisplayName("deve cancelar venda finalizada e repor estoque")
        void deveCancelarVenda_eReporEstoque() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaFinalizada(vendaId);

            Usuario gerente = Usuario.builder()
                    .id(operadorId)
                    .tenant(tenant)
                    .nome("Gerente")
                    .role(RoleUsuario.GERENTE)
                    .ativo(true)
                    .descontoMax(100.0)
                    .build();

            VendaDTOs.CancelarRequest request = new VendaDTOs.CancelarRequest();
            request.setMotivo("Erro na venda");

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(gerente));
            when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));

            VendaDTOs.Response response = vendaService.cancelar(
                    vendaId, tenantId, operadorId, request);

            assertThat(response.getStatus()).isEqualTo(StatusVenda.CANCELADA);
            assertThat(response.getMotivoCancelamento()).isEqualTo("Erro na venda");
            verify(estoqueService).reporEstoqueCancelamento(
                    eq(tenantId), eq(operadorId), eq(vendaId), any(), any());
        }

        @Test
        @DisplayName("deve lançar BusinessException quando operador não tem permissão")
        void deveLancarBusiness_quandoOperadorSemPermissao() {
            UUID vendaId = UUID.randomUUID();
            Venda venda = criarVendaFinalizada(vendaId);

            VendaDTOs.CancelarRequest request = new VendaDTOs.CancelarRequest();
            request.setMotivo("Teste");

            when(vendaRepository.findByIdComItens(vendaId, tenantId))
                    .thenReturn(Optional.of(venda));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(operador)); // OPERADOR, não GERENTE

            assertThatThrownBy(() -> vendaService.cancelar(
                    vendaId, tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Apenas Gerente ou Administrador");
        }
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private Venda criarVendaAberta(UUID vendaId) {
        return Venda.builder()
                .id(vendaId)
                .tenant(tenant)
                .caixa(caixa)
                .pdv(pdv)
                .operador(operador)
                .numeroVenda("TEST-CX01-000001")
                .dtVenda(LocalDateTime.now())
                .subtotal(BigDecimal.ZERO)
                .desconto(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .status(StatusVenda.ABERTA)
                .sincronizado(false)
                .itens(new LinkedHashSet<>())
                .pagamentos(new LinkedHashSet<>())
                .build();
    }

    private Venda criarVendaComItem(UUID vendaId, BigDecimal totalItem) {
        Venda venda = criarVendaAberta(vendaId);
        ItemVenda item = ItemVenda.builder()
                .id(UUID.randomUUID())
                .venda(venda)
                .produto(produto)
                .nomeProduto(produto.getNome())
                .quantidade(BigDecimal.valueOf(2))
                .precoUnitario(totalItem.divide(BigDecimal.valueOf(2)))
                .descontoItem(BigDecimal.ZERO)
                .totalItem(totalItem)
                .cancelado(false)
                .build();
        venda.getItens().add(item);
        venda.setSubtotal(totalItem);
        venda.setTotal(totalItem);
        return venda;
    }

    private Venda criarVendaFinalizada(UUID vendaId) {
        Venda venda = criarVendaComItem(vendaId, BigDecimal.valueOf(20.00));
        venda.setStatus(StatusVenda.FINALIZADA);

        PagamentoVenda pag = PagamentoVenda.builder()
                .id(UUID.randomUUID())
                .venda(venda)
                .forma(FormaPagamento.DINHEIRO)
                .valor(BigDecimal.valueOf(20.00))
                .troco(BigDecimal.ZERO)
                .status(StatusPagamento.CONFIRMADO)
                .build();
        venda.getPagamentos().add(pag);
        return venda;
    }
}
