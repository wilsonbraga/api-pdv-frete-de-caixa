package com.pdv.modules.relatorio.repository;

import com.pdv.modules.relatorio.dto.RelatorioDTOs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class RelatorioRepository {

    @PersistenceContext
    private final EntityManager em;

    // =====================================================================
    // KPIs
    // =====================================================================

    public BigDecimal sumFaturamento(UUID tenantId, LocalDateTime inicio, LocalDateTime fim) {
        Number result = (Number) em.createQuery("""
                SELECT COALESCE(SUM(v.total), 0)
                FROM Venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getSingleResult();
        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }

    public long countVendas(UUID tenantId, LocalDateTime inicio, LocalDateTime fim) {
        Number result = (Number) em.createQuery("""
                SELECT COUNT(v)
                FROM Venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    public BigDecimal ticketMedio(UUID tenantId, LocalDateTime inicio, LocalDateTime fim) {
        Object[] result = (Object[]) em.createQuery("""
                SELECT COALESCE(SUM(v.total), 0), COUNT(v)
                FROM Venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getSingleResult();

        BigDecimal soma = new BigDecimal(result[0].toString());
        long count = ((Number) result[1]).longValue();
        if (count == 0) return BigDecimal.ZERO;
        return soma.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    public long countCaixasAbertos(UUID tenantId) {
        Number result = (Number) em.createQuery("""
                SELECT COUNT(c) FROM Caixa c
                WHERE c.tenant.id = :tenantId AND c.status = 'ABERTO'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    public long countEstoqueCritico(UUID tenantId) {
        Number result = (Number) em.createQuery("""
                SELECT COUNT(p) FROM Produto p
                WHERE p.tenant.id = :tenantId
                AND p.ativo = true
                AND p.estoqueAtual <= p.estoqueMinimo
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    // =====================================================================
    // VENDAS POR HORA (hoje)
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<RelatorioDTOs.VendasPorHora> vendasPorHora(UUID tenantId,
                                                             LocalDateTime inicio,
                                                             LocalDateTime fim) {
        List<Object[]> rows = em.createQuery("""
                SELECT FUNCTION('date_part', 'hour', v.dtVenda), COUNT(v), COALESCE(SUM(v.total), 0)
                FROM Venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                GROUP BY FUNCTION('date_part', 'hour', v.dtVenda)
                ORDER BY 1
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();

        List<RelatorioDTOs.VendasPorHora> result = new ArrayList<>();
        for (Object[] row : rows) {
            int hora = ((Number) row[0]).intValue();
            long qtd = ((Number) row[1]).longValue();
            BigDecimal total = new BigDecimal(row[2].toString());
            result.add(new RelatorioDTOs.VendasPorHora(hora, qtd, total));
        }
        return result;
    }

    // =====================================================================
    // VENDAS POR DIA (últimos N dias)
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<RelatorioDTOs.VendasPorDia> vendasPorDia(UUID tenantId,
                                                           LocalDateTime inicio,
                                                           LocalDateTime fim) {
        List<Object[]> rows = em.createQuery("""
                SELECT CAST(v.dtVenda AS LocalDate), COUNT(v), COALESCE(SUM(v.total), 0)
                FROM Venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                GROUP BY CAST(v.dtVenda AS LocalDate)
                ORDER BY 1
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();

        List<RelatorioDTOs.VendasPorDia> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate data = (LocalDate) row[0];
            long qtd = ((Number) row[1]).longValue();
            BigDecimal total = new BigDecimal(row[2].toString());
            result.add(new RelatorioDTOs.VendasPorDia(data, qtd, total));
        }
        return result;
    }

    // =====================================================================
    // BREAKDOWN POR FORMA DE PAGAMENTO
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<RelatorioDTOs.FormaPagamentoBreakdown> breakdownPagamento(
            UUID tenantId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> rows = em.createQuery("""
                SELECT p.forma, COALESCE(SUM(p.valor), 0), COUNT(p)
                FROM PagamentoVenda p
                JOIN p.venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                AND p.status = 'CONFIRMADO'
                GROUP BY p.forma
                ORDER BY 2 DESC
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();

        BigDecimal totalGeral = rows.stream()
                .map(r -> new BigDecimal(r[1].toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RelatorioDTOs.FormaPagamentoBreakdown> result = new ArrayList<>();
        for (Object[] row : rows) {
            String forma = row[0].toString();
            BigDecimal total = new BigDecimal(row[1].toString());
            long qtd = ((Number) row[2]).longValue();
            BigDecimal percentual = totalGeral.compareTo(BigDecimal.ZERO) > 0
                    ? total.divide(totalGeral, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            result.add(new RelatorioDTOs.FormaPagamentoBreakdown(forma, total, qtd, percentual));
        }
        return result;
    }

    // =====================================================================
    // TOP PRODUTOS MAIS VENDIDOS
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<RelatorioDTOs.ProdutoMaisVendido> topProdutos(UUID tenantId,
                                                               LocalDateTime inicio,
                                                               LocalDateTime fim,
                                                               int limite) {
        List<Object[]> rows = em.createQuery("""
                SELECT i.produto.id, i.nomeProduto, i.codigoBarras,
                       SUM(i.quantidade), SUM(i.totalItem), COUNT(DISTINCT i.venda.id)
                FROM ItemVenda i
                JOIN i.venda v
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND i.cancelado = false
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                GROUP BY i.produto.id, i.nomeProduto, i.codigoBarras
                ORDER BY 4 DESC
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .setMaxResults(limite)
                .getResultList();

        List<RelatorioDTOs.ProdutoMaisVendido> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new RelatorioDTOs.ProdutoMaisVendido(
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    new BigDecimal(row[3].toString()),
                    new BigDecimal(row[4].toString()),
                    ((Number) row[5]).longValue()
            ));
        }
        return result;
    }

    // =====================================================================
    // RELATÓRIO DE VENDAS (detalhado)
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<RelatorioDTOs.ItemRelatorioVenda> relatorioVendasDetalhado(
            UUID tenantId, LocalDateTime inicio, LocalDateTime fim,
            UUID operadorId, UUID pdvId, String status) {

        StringBuilder jpql = new StringBuilder("""
                SELECT v.numeroVenda, v.dtVenda,
                       COALESCE(cli.nome, 'Sem cliente'), op.nome, pdv.nome,
                       v.subtotal, v.desconto, v.total, v.status
                FROM Venda v
                JOIN v.operador op
                JOIN v.pdv pdv
                LEFT JOIN v.cliente cli
                WHERE v.tenant.id = :tenantId
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                """);

        if (operadorId != null) jpql.append(" AND v.operador.id = :operadorId");
        if (pdvId != null) jpql.append(" AND v.pdv.id = :pdvId");
        if (status != null && !status.isBlank()) jpql.append(" AND v.status = :status");
        jpql.append(" ORDER BY v.dtVenda DESC");

        var query = em.createQuery(jpql.toString())
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim);

        if (operadorId != null) query.setParameter("operadorId", operadorId);
        if (pdvId != null) query.setParameter("pdvId", pdvId);
        if (status != null && !status.isBlank()) query.setParameter("status", status);

        List<Object[]> rows = query.getResultList();
        List<RelatorioDTOs.ItemRelatorioVenda> result = new ArrayList<>();

        for (Object[] row : rows) {
            result.add(new RelatorioDTOs.ItemRelatorioVenda(
                    (String) row[0],
                    (LocalDateTime) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    new BigDecimal(row[5].toString()),
                    new BigDecimal(row[6].toString()),
                    new BigDecimal(row[7].toString()),
                    row[8].toString(),
                    "" // formas de pagamento — populado no service
            ));
        }
        return result;
    }

    // =====================================================================
    // CURVA ABC
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<Object[]> curvaAbcRaw(UUID tenantId, LocalDateTime inicio, LocalDateTime fim) {
        return em.createQuery("""
                SELECT i.produto.id, i.nomeProduto,
                       COALESCE(cat.nome, 'Sem categoria'),
                       SUM(i.totalItem)
                FROM ItemVenda i
                JOIN i.venda v
                LEFT JOIN i.produto.categoria cat
                WHERE v.tenant.id = :tenantId
                AND v.status = 'FINALIZADA'
                AND i.cancelado = false
                AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
                GROUP BY i.produto.id, i.nomeProduto, cat.nome
                ORDER BY 4 DESC
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();
    }

    // =====================================================================
    // RELATÓRIO DE ESTOQUE
    // =====================================================================

    @SuppressWarnings("unchecked")
    public List<RelatorioDTOs.RelatorioEstoqueItem> relatorioEstoque(UUID tenantId,
                                                                       UUID categoriaId,
                                                                       Boolean apenasAbaixoMinimo) {
        StringBuilder jpql = new StringBuilder("""
                SELECT p.id, p.nome, p.codigoBarras,
                       COALESCE(cat.nome, 'Sem categoria'),
                       p.unidade, p.estoqueAtual, p.estoqueMinimo,
                       p.precoCusto, p.precoVenda
                FROM Produto p
                LEFT JOIN p.categoria cat
                WHERE p.tenant.id = :tenantId
                AND p.ativo = true
                """);

        if (categoriaId != null) jpql.append(" AND p.categoria.id = :categoriaId");
        if (Boolean.TRUE.equals(apenasAbaixoMinimo))
            jpql.append(" AND p.estoqueAtual <= p.estoqueMinimo");
        jpql.append(" ORDER BY p.nome");

        var query = em.createQuery(jpql.toString())
                .setParameter("tenantId", tenantId);
        if (categoriaId != null) query.setParameter("categoriaId", categoriaId);

        List<Object[]> rows = query.getResultList();
        List<RelatorioDTOs.RelatorioEstoqueItem> result = new ArrayList<>();

        for (Object[] row : rows) {
            BigDecimal precoCusto = new BigDecimal(row[7].toString());
            BigDecimal estoqueAtual = new BigDecimal(row[5].toString());
            BigDecimal estoqueMinimo = new BigDecimal(row[6].toString());
            BigDecimal valorEstoque = precoCusto.multiply(estoqueAtual)
                    .setScale(2, RoundingMode.HALF_UP);

            result.add(new RelatorioDTOs.RelatorioEstoqueItem(
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    estoqueAtual,
                    estoqueMinimo,
                    precoCusto,
                    new BigDecimal(row[8].toString()),
                    valorEstoque,
                    estoqueAtual.compareTo(estoqueMinimo) <= 0
            ));
        }
        return result;
    }
}
