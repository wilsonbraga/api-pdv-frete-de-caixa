package com.pdv.modules.catalogo.entity;

import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "catalogo_categoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoCategoria extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(nullable = false, length = 50)
    private String segmento;

    @Column(length = 10)
    private String icone;

    @Column(nullable = false)
    private int ordem;
}
