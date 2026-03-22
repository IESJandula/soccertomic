package com.worldcup.Back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "firebase_uid", unique = true, nullable = false)
    private String firebaseUid;

    @Column(nullable = true)
    private String password;

    @Column(nullable = true)
    private String foto;

    @Column(nullable = true)
    private String nivel = "beginner"; // beginner, intermedio, avanzado

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

