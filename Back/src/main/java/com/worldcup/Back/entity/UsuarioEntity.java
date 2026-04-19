package com.worldcup.Back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String nombre;

    @Transient
    private String email;

    @Column(name = "firebase_uid", unique = true, nullable = false)
    private String firebaseUid;

    @Column(nullable = true)
    private String foto;

    @Column(nullable = true)
    private String nivel = "beginner"; // beginner, intermedio, avanzado

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal ratingMu = new BigDecimal("25.00");

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal ratingSigma = new BigDecimal("8.33");

    @Column(nullable = false, length = 50)
    private String ratingVersion = "v1";

    @Column(nullable = false)
    private Integer partidosJugados = 0;

    @Column(nullable = false)
    private Integer victorias = 0;

    @Column(nullable = false)
    private Integer derrotas = 0;

    @Column(nullable = false)
    private Integer empates = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal fiabilidadScore = new BigDecimal("1.00");

    @Column(nullable = false)
    private Integer ausencias = 0;

    @Column(nullable = false)
    private Integer abandonos = 0;

    @Column(nullable = false)
    private Integer lesiones = 0;

    @Column(nullable = false)
    private Integer votosEmitidos = 0;

    @Column(nullable = false)
    private Integer votosValidos = 0;

    @Column(nullable = false)
    private Integer votosAtipicos = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_posiciones", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "posicion")
    private List<String> posiciones = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_rasgos", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rasgo")
    private List<String> rasgos = new ArrayList<>();

    @Column
    private Integer puntos = 0;

    @Column
    private Integer noShows = 0;

    @Column
    private Integer reputacionPositiva = 0;

    // Relaciones con organización de partidos
    @JsonIgnore
    @OneToMany(mappedBy = "usuario")
    private List<PartidoOrganizadorEntity> partidosOrganizados = new ArrayList<>();

    @JsonIgnore
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PlayerProfileEntity playerProfile;
}

