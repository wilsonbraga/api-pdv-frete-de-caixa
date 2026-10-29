package com.pdv.modules.estoque.repository;

import com.pdv.modules.estoque.entity.MovimentacaoEstoque;
import com.pdv.modules.estoque.entity.TipoMovimentacaoEstoque;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specification para filtro de movimentações.
 *
 * Resolve o erro PostgreSQL "could not determine data type of parameter $N"
 * que ocorre quando JPQL passa null para parâmetros de tipos não-varchar
 * (UUID, Enum, Timestamp). Com Specification/Criteria API os predicados
 * nulos são simplesmente omitidos — sem parâmetros ambíguos.
 */
public class MovimentacaoEstoqueSpecification {

    private MovimentacaoEstoqueSpecification() {}

    public static Specification<MovimentacaoEstoque> filtros(
            UUID tenantId,
            UUID produtoId,
            TipoMovimentacaoEstoque tipo,
            LocalDateTime inicio,
            LocalDateTime fim) {

        return (root, query, cb) -> {
            // Eager fetch para evitar N+1
            if (query.getResultType().equals(MovimentacaoEstoque.class)) {
                root.fetch("produto");
                root.fetch("usuario", jakarta.persistence.criteria.JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // tenantId — sempre obrigatório
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            // produtoId — opcional
            if (produtoId != null) {
                predicates.add(cb.equal(root.get("produto").get("id"), produtoId));
            }

            // tipo — opcional (enum sem cast problemático)
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }

            // inicio — opcional
            if (inicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio));
            }

            // fim — opcional
            if (fim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("criadoEm"), fim));
            }

            query.orderBy(cb.desc(root.get("criadoEm")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
