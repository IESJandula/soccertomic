package com.worldcup.Back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.worldcup.Back.entity.enums.TipoPartido;
import com.worldcup.Back.entity.enums.EstadoPartido;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Partido")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String lugar;

    @Column(nullable = false)
    private Integer jugadoresPorEquipo = 6; // Por defecto 6vs6

    @Column(nullable = false)
    private Integer duracionMinutos = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPartido tipo = TipoPartido.PRIVADO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPartido estado = EstadoPartido.BORRADOR;

    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PartidoOrganizadorEntity> organizadores = new ArrayList<>();

    // Jugadores inscritos (sin equipo asignado aún)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "partido_jugadores_inscritos",
            joinColumns = @JoinColumn(name = "partido_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<UsuarioEntity> jugadoresInscritos = new ArrayList<>();

    // Equipos (ManyToMany sin tabla intermedia dedicada, usando @ElementCollection o ManyToMany con fetch EAGER)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "partido_equipo_a",
            joinColumns = @JoinColumn(name = "partido_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<UsuarioEntity> equipoA = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "partido_equipo_b",
            joinColumns = @JoinColumn(name = "partido_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<UsuarioEntity> equipoB = new ArrayList<>();

    // Chat del partido
    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ChatPartidoEntity> chat = new ArrayList<>();

    // Resultado (post-partido)
    @Column
    private Integer golesEquipoA = null;

    @Column
    private Integer golesEquipoB = null;

    @Column
    private String ganador = null; // "EQUIPO_A", "EQUIPO_B", null (en curso)

    @Column
    private LocalDateTime canceladoEn;

    @Column(length = 255)
    private String motivoCancelacion;

    @Column(nullable = false)
    private Boolean archivado = false;

    // Colores de equipos
    @Column(nullable = false)
    private String colorEquipoA = "Blanco"; // Color del equipo A (por defecto Blanco)

    @Column(nullable = false)
    private String colorEquipoB = "Oscuro"; // Color del equipo B (por defecto Oscuro)

    // Timestamps
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column
    private LocalDateTime actualizadoEn;

    // Inicialización después de cargar desde BD (para partidos antiguos)
    @PostLoad
    private void initializeCollections() {
        if (jugadoresInscritos == null) {
            jugadoresInscritos = new ArrayList<>();
        }
        if (equipoA == null) {
            equipoA = new ArrayList<>();
        }
        if (equipoB == null) {
            equipoB = new ArrayList<>();
        }
        if (organizadores == null) {
            organizadores = new ArrayList<>();
        }
    }

    // Métodos de utilidad
    public boolean estaConfirmado() {
        return equipoA.size() + equipoB.size() == jugadoresPorEquipo * 2;
    }

    public Integer getTotalJugadores() {
        return jugadoresInscritos.size() + equipoA.size() + equipoB.size();
    }
    
    public Integer getTotalJugadoresEnEquipos() {
        return equipoA.size() + equipoB.size();
    }
}
