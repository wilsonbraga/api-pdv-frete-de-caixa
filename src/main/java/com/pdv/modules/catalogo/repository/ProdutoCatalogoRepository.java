package com.pdv.modules.catalogo.repository;

import com.pdv.modules.catalogo.entity.ProdutoCatalogo;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProdutoCatalogoRepository
        extends JpaRepository<ProdutoCatalogo, UUID>,
                JpaSpecificationExecutor<ProdutoCatalogo> {

    /**
     * Busca com filtros usando Specification/Criteria API.
     *
     * MOTIVO: JPQL com LOWER(:param) falha no PostgreSQL quando :param é null,
     * pois o driver passa o valor como bytea sem tipo explícito (erro 42883).
     * Com Criteria API os predicados nulos são simplesmente omitidos.
     */
    static Specification<ProdutoCatalogo> comFiltros(
            String segmento, UUID categoriaId, String nome) {

        return (root, query, cb) -> {
            // Eager fetch categoria para evitar N+1
            if (Long.class != query.getResultType()) {
                root.fetch("categoria", JoinType.INNER);
            }

            var predicates = new ArrayList<Predicate>();

            // Apenas ativos
            predicates.add(cb.isTrue(root.get("ativo")));

            // Filtro por segmento (String — sem problema de tipo)
            if (segmento != null && !segmento.isBlank()) {
                predicates.add(cb.equal(
                        root.get("categoria").get("segmento"), segmento));
            }

            // Filtro por categoriaId (UUID — sem problema de tipo)
            if (categoriaId != null) {
                predicates.add(cb.equal(
                        root.get("categoria").get("id"), categoriaId));
            }

            // Filtro por nome — LOWER aplicado apenas quando o valor não é nulo
            // Evita o erro "function lower(bytea) does not exist" do PostgreSQL
            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("nome")),
                        "%" + nome.toLowerCase() + "%"));
            }

            query.orderBy(
                    cb.asc(root.get("categoria").get("ordem")),
                    cb.asc(root.get("nome")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Query("""
            SELECT p FROM ProdutoCatalogo p
            JOIN FETCH p.categoria c
            WHERE p.ativo = true
            AND c.id IN :categoriaIds
            ORDER BY c.ordem, p.nome
            """)
    List<ProdutoCatalogo> findByCategoriaIds(@Param("categoriaIds") List<UUID> categoriaIds);

    @Query("""
            SELECT p FROM ProdutoCatalogo p
            JOIN FETCH p.categoria c
            WHERE p.ativo = true
            AND c.segmento IN :segmentos
            ORDER BY c.ordem, p.nome
            """)
    List<ProdutoCatalogo> findBySegmentos(@Param("segmentos") List<String> segmentos);
}
