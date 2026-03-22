package com.worldcup.Back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.worldcup.Back.entity.enums.EstadoInvitacion;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Invitacion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvitacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "partido_id", nullable = false)
    private PartidoEntity partido;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInvitacion estado = EstadoInvitacion.PENDIENTE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column
    private LocalDateTime respondidaEn;

    @Column(length = 500)
    private String mensaje;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioTotalPista;

    @Column(precision = 10, scale = 2)
    private BigDecimal parteIndividual;

    @Column
    private Long reservadoPorUsuarioId;

    @Column(length = 120)
    private String reservadoPorNombre;

    @Column(nullable = false)
    private Boolean pagada = false;
}
