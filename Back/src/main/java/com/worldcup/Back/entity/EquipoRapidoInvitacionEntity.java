package com.worldcup.Back.entity;

import com.worldcup.Back.entity.enums.EstadoInvitacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipo_rapido_invitacion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EquipoRapidoInvitacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_rapido_id", nullable = false)
    private EquipoRapidoEntity equipoRapido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_usuario_id", nullable = false)
    private UsuarioEntity emisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_usuario_id", nullable = false)
    private UsuarioEntity destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInvitacion estado = EstadoInvitacion.PENDIENTE;

    @Column(length = 500)
    private String mensaje;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column
    private LocalDateTime respondidaEn;
}
