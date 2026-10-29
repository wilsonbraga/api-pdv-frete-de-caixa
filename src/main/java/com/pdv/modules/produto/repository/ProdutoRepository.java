package com.pdv.modules.produto.repository;

import com.pdv.modules.produto.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

    Optional<Produto> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Produto> findByCodigoBarrasAndTenantId(String codigoBarras, UUID tenantId);
    
    // Busca por codigo de barra
    boolean existsByCodigoBarrasAndTenantId(String codigoBarras, UUID tenantId);
    
    // Busca por SKU exato
    Optional<Produto> findBySkuAndTenantId(String sku, UUID tenantId);

    // Busca por nome exato, ignorando maiúsculas/minúsculas
    //Optional<Produto> findByNomeIgnoreCaseAndTenantId(String nome, UUID tenantId);
    @Query(value = """
            SELECT * FROM produto p
            WHERE p.tenant_id = CAST(:tenantId AS uuid)
            AND p.ativo = true
            AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
            ORDER BY
                CASE WHEN LOWER(p.nome) = LOWER(:nome) THEN 0 ELSE 1 END,
                p.nome ASC
            """, nativeQuery = true)
    List<Produto> findByNomeContendoAndTenantId(@Param("nome") String nome,
                                                 @Param("tenantId") UUID tenantId,
                                                 Pageable pageable);

    boolean existsBySkuAndTenantId(String sku, UUID tenantId);

    @Query("""
            SELECT p FROM Produto p
            LEFT JOIN FETCH p.categoria
            WHERE p.tenant.id = :tenantId
            AND (CAST(:nome AS string) IS NULL
                 OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
            AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
            AND (:ativo IS NULL OR p.ativo = :ativo)
            AND (CAST(:codigoBarras AS string) IS NULL OR p.codigoBarras = CAST(:codigoBarras AS string))
            AND (:estoqueAbaixoMinimo IS NULL OR
                 (:estoqueAbaixoMinimo = true AND p.estoqueAtual <= p.estoqueMinimo))
            """)
    Page<Produto> findByFiltros(@Param("tenantId") UUID tenantId,
                                @Param("nome") String nome,
                                @Param("categoriaId") UUID categoriaId,
                                @Param("ativo") Boolean ativo,
                                @Param("codigoBarras") String codigoBarras,
                                @Param("estoqueAbaixoMinimo") Boolean estoqueAbaixoMinimo,
                                Pageable pageable);

    @Query("""
            SELECT p FROM Produto p
            WHERE p.tenant.id = :tenantId
            AND p.ativo = true
            AND p.estoqueAtual <= p.estoqueMinimo
            """)
    List<Produto> findEstoqueCritico(@Param("tenantId") UUID tenantId);

    @Modifying
    @Query("""
            UPDATE Produto p
            SET p.estoqueAtual = p.estoqueAtual + :quantidade
            WHERE p.id = :id
            """)
    void incrementarEstoque(@Param("id") UUID id, @Param("quantidade") BigDecimal quantidade);

    @Modifying
    @Query("""
            UPDATE Produto p
            SET p.estoqueAtual = p.estoqueAtual - :quantidade
            WHERE p.id = :id AND p.estoqueAtual >= :quantidade
            """)
    int decrementarEstoque(@Param("id") UUID id, @Param("quantidade") BigDecimal quantidade);
    
    
    /**
     * Retorna todos os produtos ativos de um tenant com estoque <= mínimo.
     * Usado pelo job de alerta de estoque.
     */
    @Query("""
            SELECT p FROM Produto p
            LEFT JOIN FETCH p.categoria
            WHERE p.tenant.id = :tenantId
            AND   p.ativo     = true
            AND   p.estoqueAtual <= p.estoqueMinimo
            ORDER BY p.nome
            """)
    List<Produto> findByTenantIdAndAtivoTrueAndEstoqueAbaixoMinimo(
            @Param("tenantId") UUID tenantId);

}