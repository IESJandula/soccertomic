package com.worldcup.Back.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partido_votacion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoVotacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private PartidoEntity partido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votante_id", nullable = false)
    private UsuarioEntity votante;

    @Column(nullable = false)
    private Integer golesEquipoAPropuesto;

    @Column(nullable = false)
    private Integer golesEquipoBPropuesto;

    @Column(nullable = false, length = 24)
    private String intensidadPartido;

    @Column(nullable = false)
    private Boolean partidoFueParejo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "partido_votacion_diferenciales", joinColumns = @JoinColumn(name = "votacion_id"))
    @Column(name = "jugador_id")
    private List<Long> jugadoresDiferenciales = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "partido_votacion_companeros", joinColumns = @JoinColumn(name = "votacion_id"))
    private List<PartidoCompaneroValoradoEmbeddable> valoracionesCompaneros = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadaEn;
}
