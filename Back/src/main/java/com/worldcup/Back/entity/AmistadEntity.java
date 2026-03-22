package com.worldcup.Back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.worldcup.Back.entity.enums.EstadoAmistad;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Amistad")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AmistadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_a_id", nullable = false)
    private UsuarioEntity usuarioA;

    @ManyToOne
    @JoinColumn(name = "usuario_b_id", nullable = false)
    private UsuarioEntity usuarioB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAmistad estado = EstadoAmistad.SOLICITADA;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column
    private LocalDateTime aceptadaEn;
}
