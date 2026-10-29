package com.pdv.modules.produto.repository;

import com.pdv.modules.produto.entity.HistoricoPreco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HistoricoPrecoRepository extends JpaRepository<HistoricoPreco, UUID> {

    Page<HistoricoPreco> findByProdutoIdOrderByCriadoEmDesc(UUID produtoId, Pageable pageable);
}
