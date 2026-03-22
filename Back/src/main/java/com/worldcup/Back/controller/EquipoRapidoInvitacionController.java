package com.worldcup.Back.controller;

import com.worldcup.Back.dto.response.EquipoRapidoInvitacionResponseDTO;
import com.worldcup.Back.entity.EquipoRapidoInvitacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.EquipoRapidoInvitacionService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/equipos-rapidos/invitaciones")
public class EquipoRapidoInvitacionController {

    @Autowired
    private EquipoRapidoInvitacionService equipoRapidoInvitacionService;

    @Autowired
    private UserService userService;

    private EquipoRapidoInvitacionResponseDTO toDTO(EquipoRapidoInvitacionEntity invitacion) {
        return new EquipoRapidoInvitacionResponseDTO(
                invitacion.getId(),
                invitacion.getEquipoRapido().getId(),
                invitacion.getEquipoRapido().getNombre(),
                userService.entityToResumenDTO(invitacion.getEmisor()),
                userService.entityToResumenDTO(invitacion.getDestinatario()),
                invitacion.getEstado().toString(),
                invitacion.getMensaje(),
                invitacion.getCreadaEn(),
                invitacion.getRespondidaEn()
        );
    }

    @PostMapping
    public ResponseEntity<EquipoRapidoInvitacionResponseDTO> invitarAEquipo(
            HttpServletRequest request,
            @RequestParam Long equipoRapidoId,
            @RequestParam Long usuarioId
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> emisor = userService.buscarPorFirebaseUid(uid);

        if (emisor.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            EquipoRapidoInvitacionEntity invitacion = equipoRapidoInvitacionService.crearInvitacion(equipoRapidoId, usuarioId, emisor.get());
            return ResponseEntity.ok(toDTO(invitacion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/mis-invitaciones")
    public ResponseEntity<List<EquipoRapidoInvitacionResponseDTO>> obtenerMisInvitaciones(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<EquipoRapidoInvitacionResponseDTO> dtos = equipoRapidoInvitacionService.obtenerInvitacionesDeUsuario(usuario.get())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<EquipoRapidoInvitacionResponseDTO> aceptarInvitacion(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            EquipoRapidoInvitacionEntity invitacion = equipoRapidoInvitacionService.aceptarInvitacion(id, usuario.get());
            return ResponseEntity.ok(toDTO(invitacion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<EquipoRapidoInvitacionResponseDTO> rechazarInvitacion(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            EquipoRapidoInvitacionEntity invitacion = equipoRapidoInvitacionService.rechazarInvitacion(id, usuario.get());
            return ResponseEntity.ok(toDTO(invitacion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
