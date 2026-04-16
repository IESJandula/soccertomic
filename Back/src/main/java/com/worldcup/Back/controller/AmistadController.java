package com.worldcup.Back.controller;

import com.worldcup.Back.dto.response.AmistadResponseDTO;
import com.worldcup.Back.entity.AmistadEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.AmistadService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/amistades")
public class AmistadController {

    @Autowired
    private AmistadService amistadService;

    @Autowired
    private UserService userService;
    
    // Helper to convert Entity to DTO
    private AmistadResponseDTO entityToDTO(AmistadEntity amistad) {
        return new AmistadResponseDTO(
            amistad.getId(),
            userService.entityToResumenDTO(amistad.getUsuarioA()),
            userService.entityToResumenDTO(amistad.getUsuarioB()),
            amistad.getEstado().toString(),
            amistad.getCreadaEn(),
            amistad.getAceptadaEn()
        );
    }

    @PostMapping
    public ResponseEntity<AmistadResponseDTO> enviarSolicitud(
            HttpServletRequest request,
            @RequestParam Long usuarioBId
    ) {
        UsuarioEntity usuarioA = userService.obtenerOCrearDesdeRequest(request);
        Optional<UsuarioEntity> usuarioB = userService.buscarPorId(usuarioBId);

        if (usuarioB.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AmistadEntity amistad = amistadService.enviarSolicitud(usuarioA, usuarioB.get());
            return ResponseEntity.ok(entityToDTO(amistad));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/mis-amigos")
    public ResponseEntity<List<AmistadResponseDTO>> obtenerMisAmigos(HttpServletRequest request) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        List<AmistadEntity> amigos = amistadService.obtenerAmigosDeUsuario(usuario);
        List<AmistadResponseDTO> dtos = amigos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/solicitudes-pendientes")
    public ResponseEntity<List<AmistadResponseDTO>> obtenerSolicitudesPendientes(HttpServletRequest request) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        List<AmistadEntity> solicitudes = amistadService.obtenerSolicitudesPendientes(usuario);
        List<AmistadResponseDTO> dtos = solicitudes.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/solicitudes-enviadas")
    public ResponseEntity<List<AmistadResponseDTO>> obtenerSolicitudesEnviadas(HttpServletRequest request) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        List<AmistadEntity> solicitudes = amistadService.obtenerSolicitudesEnviadas(usuario);
        List<AmistadResponseDTO> dtos = solicitudes.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<AmistadResponseDTO> aceptarSolicitud(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            AmistadEntity amistad = amistadService.aceptarSolicitud(id, usuario);
            return ResponseEntity.ok(entityToDTO(amistad));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<AmistadResponseDTO> rechazarSolicitud(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            AmistadEntity amistad = amistadService.rechazarSolicitud(id, usuario);
            return ResponseEntity.ok(entityToDTO(amistad));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAmistad(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            amistadService.eliminarAmistad(id, usuario);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.noContent().build();
    }
}
