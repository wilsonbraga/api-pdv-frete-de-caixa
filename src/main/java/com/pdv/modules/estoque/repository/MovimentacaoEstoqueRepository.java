package com.pdv.modules.estoque.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pdv.modules.estoque.entity.MovimentacaoEstoque;

@Repository
public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, UUID>,
                JpaSpecificationExecutor<MovimentacaoEstoque> {

    Page<MovimentacaoEstoque> findByTenantIdAndProdutoIdOrderByCriadoEmDesc(
            UUID tenantId, UUID produtoId, Pageable pageable);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.referenciaId   = :referenciaId
            AND   m.referenciaTipo = :referenciaTipo
            """)
    List<MovimentacaoEstoque> findByReferencia(
            @Param("referenciaId")   UUID referenciaId,
            @Param("referenciaTipo") String referenciaTipo);
}
