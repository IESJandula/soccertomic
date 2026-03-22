package com.worldcup.Back.controller;

import com.worldcup.Back.dto.response.AmistadResponseDTO;
import com.worldcup.Back.entity.AmistadEntity;
import com.worldcup.Back.entity.UsuarioEntity;
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
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuarioA = userService.buscarPorFirebaseUid(uid);
        Optional<UsuarioEntity> usuarioB = userService.buscarPorId(usuarioBId);

        if (usuarioA.isEmpty() || usuarioB.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AmistadEntity amistad = amistadService.enviarSolicitud(usuarioA.get(), usuarioB.get());
            return ResponseEntity.ok(entityToDTO(amistad));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/mis-amigos")
    public ResponseEntity<List<AmistadResponseDTO>> obtenerMisAmigos(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<AmistadEntity> amigos = amistadService.obtenerAmigosDeUsuario(usuario.get());
        List<AmistadResponseDTO> dtos = amigos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/solicitudes-pendientes")
    public ResponseEntity<List<AmistadResponseDTO>> obtenerSolicitudesPendientes(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<AmistadEntity> solicitudes = amistadService.obtenerSolicitudesPendientes(usuario.get());
        List<AmistadResponseDTO> dtos = solicitudes.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/solicitudes-enviadas")
    public ResponseEntity<List<AmistadResponseDTO>> obtenerSolicitudesEnviadas(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<AmistadEntity> solicitudes = amistadService.obtenerSolicitudesEnviadas(usuario.get());
        List<AmistadResponseDTO> dtos = solicitudes.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<AmistadResponseDTO> aceptarSolicitud(@PathVariable Long id) {
        try {
            AmistadEntity amistad = amistadService.aceptarSolicitud(id);
            return ResponseEntity.ok(entityToDTO(amistad));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<AmistadResponseDTO> rechazarSolicitud(@PathVariable Long id) {
        try {
            AmistadEntity amistad = amistadService.rechazarSolicitud(id);
            return ResponseEntity.ok(entityToDTO(amistad));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAmistad(@PathVariable Long id) {
        amistadService.eliminarAmistad(id);
        return ResponseEntity.noContent().build();
    }
}
