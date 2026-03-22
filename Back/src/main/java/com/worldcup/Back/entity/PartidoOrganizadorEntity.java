package com.worldcup.Back.entity;

import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "partido_organizador",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_partido_organizador_partido_usuario", columnNames = {"partido_id", "usuario_id"})
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoOrganizadorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private PartidoEntity partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartidoOrganizadorRol rol;

    @CreationTimestamp
    @Column(name = "asignado_en", nullable = false, updatable = false)
    private LocalDateTime asignadoEn;
}
