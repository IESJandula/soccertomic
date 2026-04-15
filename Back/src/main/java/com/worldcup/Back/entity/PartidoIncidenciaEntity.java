package com.worldcup.Back.entity;

import com.worldcup.Back.entity.enums.TipoIncidenciaPartido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "partido_incidencia")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoIncidenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private PartidoEntity partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_afectado_id", nullable = false)
    private UsuarioEntity usuarioAfectado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportado_por_id", nullable = false)
    private UsuarioEntity reportadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TipoIncidenciaPartido tipoIncidencia;

    @Column(nullable = false)
    private Integer severidad = 2;

    @Column
    private Integer minuto;

    @Column(length = 255)
    private String comentario;

    @Column(nullable = false)
    private Boolean validadaPorOrganizador = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadaEn;
}
