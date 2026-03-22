package com.worldcup.Back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_profile")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    // ===== ATRIBUTOS NUMÉRICOS (0-5 escala) =====
    @Column(nullable = false)
    private Integer shooting = 3;

    @Column(nullable = false)
    private Integer speed = 3;

    @Column(nullable = false)
    private Integer dribbling = 3;

    @Column(nullable = false)
    private Integer defense = 3;

    @Column(nullable = false)
    private Integer strength = 3;

    @Column(nullable = false)
    private Integer stamina = 3;

    @Column(nullable = false)
    private Integer aerial = 3;

    // ===== POSICIÓN Y ESTILO =====
    @Column(nullable = false)
    private Boolean goalkeeper = false;

    @Column(nullable = false, length = 20)
    private String posicionPreferida = "MEDIOCAMPISTA"; // DELANTERO, MEDIOCAMPISTA, DEFENSA, PORTERO

    @Column(nullable = false, length = 1)
    private String playStyle = "A"; // O (Ofensivo), D (Defensivo), A (Adaptable)

    // ===== NIVEL DE HABILIDAD (TIER) =====
    @Column(nullable = false, length = 20)
    private String skillTier = "BRONCE"; // BRONCE, PLATA, ORO, DIAMANTE

    // ===== TENDENCIA DE JUEGO (calculada) =====
    @Column(nullable = true, length = 20)
    private String playTendency; // OFENSIVA, DEFENSIVA, ADAPTABLE

    // ===== RANGO DE EDAD =====
    @Column(name = "experience_level", nullable = false, length = 20)
    private String ageRange = "18_25"; // UNDER_18, 18_25, 25_35, 35_50, OVER_50

    // ===== RATING GLOBAL =====
    @Column(nullable = false)
    private Float globalRating = 3.0f;

    // ===== NUEVOS CAMPOS: PIERNA BUENA Y DISPONIBILIDAD =====
    @Column(nullable = true, length = 20)
    private String piernaBuena; // Izquierda, Derecha, Ambidiestra

    @Column(nullable = true, columnDefinition = "TEXT")
    private String disponibilidad; // Stored as JSON array or comma-separated string

    // ===== TIMESTAMPS =====
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadoEn;
}
