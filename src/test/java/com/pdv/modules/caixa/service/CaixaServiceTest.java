package com.pdv.modules.caixa.service;

import com.pdv.modules.caixa.dto.CaixaDTOs;
import com.pdv.modules.caixa.entity.Caixa;
import com.pdv.modules.caixa.entity.StatusCaixa;
import com.pdv.modules.caixa.repository.CaixaRepository;
import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.pdv.repository.PDVRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.RoleUsuario;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.modules.venda.entity.FormaPagamento;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaixaService")
class CaixaServiceTest {

    @Mock private CaixaRepository caixaRepository;
    @Mock private PDVRepository pdvRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CaixaService caixaService;

    private UUID tenantId, operadorId, pdvId, caixaId;
    private Tenant tenant;
    private PDV pdv;
    private Usuario operador;
    private Caixa caixaAberto;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        operadorId = UUID.randomUUID();
        pdvId = UUID.randomUUID();
        caixaId = UUID.randomUUID();

        tenant = Tenant.builder().id(tenantId).nome("Loja").ativo(true).plano("BASICO").build();
        pdv = PDV.builder().id(pdvId).tenant(tenant).nome("Caixa 01").ativo(true).build();
        operador = Usuario.builder().id(operadorId).tenant(tenant).nome("Op")
                .role(RoleUsuario.OPERADOR).ativo(true).descontoMax(0.0).build();

        caixaAberto = Caixa.builder()
                .id(caixaId)
                .tenant(tenant)
                .pdv(pdv)
                .operador(operador)
                .dtAbertura(LocalDateTime.now())
                .valorInicial(BigDecimal.valueOf(200))
                .totalSangria(BigDecimal.ZERO)
                .totalSuprimento(BigDecimal.ZERO)
                .status(StatusCaixa.ABERTO)
                .movimentacoes(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("abrir()")
    class Abrir {

        @Test
        @DisplayName("deve abrir caixa com valor inicial correto")
        void deveAbrirCaixa_comValorInicial() {
            CaixaDTOs.AbrirRequest request = new CaixaDTOs.AbrirRequest();
            request.setPdvId(pdvId);
            request.setValorInicial(BigDecimal.valueOf(150));

            when(pdvRepository.findByIdAndTenantId(pdvId, tenantId)).thenReturn(Optional.of(pdv));
            when(caixaRepository.findByPdvIdAndStatus(pdvId, StatusCaixa.ABERTO))
                    .thenReturn(Optional.empty());
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(operador));
            when(caixaRepository.save(any(Caixa.class))).thenAnswer(inv -> {
                Caixa c = inv.getArgument(0);
                c = Caixa.builder().id(UUID.randomUUID()).tenant(tenant).pdv(pdv)
                        .operador(operador).dtAbertura(LocalDateTime.now())
                        .valorInicial(BigDecimal.valueOf(150))
                        .totalSangria(BigDecimal.ZERO).totalSuprimento(BigDecimal.ZERO)
                        .status(StatusCaixa.ABERTO).movimentacoes(new ArrayList<>()).build();
                return c;
            });

            CaixaDTOs.Response response = caixaService.abrir(tenantId, operadorId, request);

            assertThat(response.getStatus()).isEqualTo(StatusCaixa.ABERTO);
            assertThat(response.getValorInicial()).isEqualByComparingTo(BigDecimal.valueOf(150));
        }

        @Test
        @DisplayName("deve lançar BusinessException quando já há caixa aberto para o PDV")
        void deveLancarBusiness_quandoCaixaJaAberto() {
            CaixaDTOs.AbrirRequest request = new CaixaDTOs.AbrirRequest();
            request.setPdvId(pdvId);
            request.setValorInicial(BigDecimal.valueOf(100));

            when(pdvRepository.findByIdAndTenantId(pdvId, tenantId)).thenReturn(Optional.of(pdv));
            when(caixaRepository.findByPdvIdAndStatus(pdvId, StatusCaixa.ABERTO))
                    .thenReturn(Optional.of(caixaAberto));

            assertThatThrownBy(() -> caixaService.abrir(tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Já existe um caixa aberto");
        }
    }

    @Nested
    @DisplayName("sangria()")
    class Sangria {

        @Test
        @DisplayName("deve realizar sangria quando há saldo suficiente")
        void deveRealizarSangria_quandoSaldoSuficiente() {
            caixaAberto.setTotalDinheiro(BigDecimal.valueOf(500));

            CaixaDTOs.SangriaSuprimentoRequest request = new CaixaDTOs.SangriaSuprimentoRequest();
            request.setValor(BigDecimal.valueOf(100));
            request.setMotivo("Sangria teste");

            when(caixaRepository.findByIdAndTenantId(caixaId, tenantId))
                    .thenReturn(Optional.of(caixaAberto));
            when(usuarioRepository.findByIdAndTenantId(operadorId, tenantId))
                    .thenReturn(Optional.of(operador));
            when(caixaRepository.save(any(Caixa.class))).thenReturn(caixaAberto);

            CaixaDTOs.MovimentacaoResponse response =
                    caixaService.sangria(caixaId, tenantId, operadorId, request);

            assertThat(caixaAberto.getTotalSangria())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(response.getMotivo()).isEqualTo("Sangria teste");
        }

        @Test
        @DisplayName("deve lançar BusinessException quando saldo dinheiro insuficiente")
        void deveLancarBusiness_quandoSaldoInsuficiente() {
            // valorInicial=200, totalDinheiro=null → saldoDinheiro = 200
            CaixaDTOs.SangriaSuprimentoRequest request = new CaixaDTOs.SangriaSuprimentoRequest();
            request.setValor(BigDecimal.valueOf(500)); // maior que 200
            request.setMotivo("Teste");

            when(caixaRepository.findByIdAndTenantId(caixaId, tenantId))
                    .thenReturn(Optional.of(caixaAberto));

            assertThatThrownBy(() -> caixaService.sangria(caixaId, tenantId, operadorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("insuficiente");
        }
    }

    @Nested
    @DisplayName("adicionarTotalVenda()")
    class AdicionarTotal {

        @Test
        @DisplayName("deve acumular corretamente os totais por forma de pagamento")
        void deveAcumularTotaisPorForma() {
            when(caixaRepository.findById(caixaId)).thenReturn(Optional.of(caixaAberto));
            when(caixaRepository.save(any(Caixa.class))).thenReturn(caixaAberto);

            caixaService.adicionarTotalVenda(caixaId, FormaPagamento.DINHEIRO, BigDecimal.valueOf(50));
            caixaService.adicionarTotalVenda(caixaId, FormaPagamento.PIX, BigDecimal.valueOf(30));
            caixaService.adicionarTotalVenda(caixaId, FormaPagamento.DINHEIRO, BigDecimal.valueOf(20));

            assertThat(caixaAberto.getTotalDinheiro()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(caixaAberto.getTotalPix()).isEqualByComparingTo(BigDecimal.valueOf(30));
        }
    }
}
