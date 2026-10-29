package com.pdv.modules.catalogo.repository;

import com.pdv.modules.catalogo.entity.CatalogoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogoCategoriaRepository extends JpaRepository<CatalogoCategoria, UUID> {
    List<CatalogoCategoria> findAllByOrderByOrdemAsc();
    List<CatalogoCategoria> findBySegmentoOrderByOrdemAsc(String segmento);
}
