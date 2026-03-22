package com.worldcup.Back.controller;

import com.worldcup.Back.dto.response.InvitacionResponseDTO;
import com.worldcup.Back.entity.InvitacionEntity;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.InvitacionService;
import com.worldcup.Back.service.PartidoService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.Locale;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/invitaciones")
public class InvitacionController {

    private static final DateTimeFormatter FORMATO_FECHA_RESERVA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", new Locale("es", "ES"));

    @Autowired
    private InvitacionService invitacionService;

    @Autowired
    private PartidoService partidoService;

    @Autowired
    private UserService userService;
    
    // Helper to convert Entity to DTO
    private InvitacionResponseDTO entityToDTO(InvitacionEntity invitacion) {
        String mensaje = invitacion.getMensaje();
        if (invitacion.getPrecioTotalPista() != null
            && invitacion.getParteIndividual() != null
            && invitacion.getPartido() != null) {
            String lugar = invitacion.getPartido().getLugar() == null ? "Lugar no definido" : invitacion.getPartido().getLugar();
            String fecha = invitacion.getPartido().getFecha() == null
                ? "Fecha no definida"
                : invitacion.getPartido().getFecha().format(FORMATO_FECHA_RESERVA);
            String destinatarioPago = invitacion.getReservadoPorNombre() == null || invitacion.getReservadoPorNombre().isBlank()
                ? "organización"
                : invitacion.getReservadoPorNombre();
            mensaje = "Partido reservado en " + lugar + " el " + fecha
                + ". Tu parte: $" + invitacion.getParteIndividual().toPlainString()
                + ". Monto total: $" + invitacion.getPrecioTotalPista().toPlainString()
                + ". Pagar a: " + destinatarioPago + ".";
        }

        return new InvitacionResponseDTO(
            invitacion.getId(),
            invitacion.getPartido().getId(),
            userService.entityToResumenDTO(invitacion.getUsuario()),
            invitacion.getEstado().toString(),
            mensaje,
            invitacion.getPrecioTotalPista(),
            invitacion.getParteIndividual(),
            invitacion.getReservadoPorUsuarioId(),
            invitacion.getReservadoPorNombre(),
            invitacion.getPagada(),
            invitacion.getCreadaEn(),
            invitacion.getRespondidaEn()
        );
    }

    @PostMapping
    public ResponseEntity<InvitacionResponseDTO> crearInvitacion(
            @RequestParam Long partidoId,
            @RequestParam Long usuarioId
    ) {
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(partidoId);
        Optional<UsuarioEntity> usuario = userService.buscarPorId(usuarioId);

        if (partido.isEmpty() || usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        InvitacionEntity invitacion = invitacionService.crearInvitacion(partido.get(), usuario.get());
        return ResponseEntity.ok(entityToDTO(invitacion));
    }

    @GetMapping("/mis-invitaciones")
    public ResponseEntity<List<InvitacionResponseDTO>> obtenerMisInvitaciones(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<InvitacionEntity> invitaciones = invitacionService.obtenerInvitacionesDeUsuario(usuario.get());
        List<InvitacionResponseDTO> dtos = invitaciones.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<InvitacionResponseDTO>> obtenerInvitacionesPendientes(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<InvitacionEntity> invitaciones = invitacionService.obtenerInvitacionesPendientes(usuario.get());
        List<InvitacionResponseDTO> dtos = invitaciones.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<InvitacionResponseDTO> aceptarInvitacion(@PathVariable Long id) {
        try {
            InvitacionEntity invitacion = invitacionService.aceptarInvitacion(id);
            return ResponseEntity.ok(entityToDTO(invitacion));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<InvitacionResponseDTO> rechazarInvitacion(@PathVariable Long id) {
        try {
            InvitacionEntity invitacion = invitacionService.rechazarInvitacion(id);
            return ResponseEntity.ok(entityToDTO(invitacion));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInvitacion(@PathVariable Long id) {
        invitacionService.eliminarInvitacion(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/marcar-pagada")
    public ResponseEntity<?> marcarReservaComoPagada(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            InvitacionEntity invitacion = invitacionService.marcarReservaComoPagada(id, usuario.get());
            return ResponseEntity.ok(entityToDTO(invitacion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
