package com.worldcup.Back.controller;

import com.worldcup.Back.dto.request.PartidoVotacionRequestDTO;
import com.worldcup.Back.dto.response.PartidoCompaneroAsignadoDTO;
import com.worldcup.Back.dto.response.PartidoVotacionPanelCompartidoDTO;
import com.worldcup.Back.dto.response.PartidoVotacionResponseDTO;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.PartidoVotacionService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/partidos/{partidoId}/votaciones")
public class PartidoVotacionController {

    @Autowired
    private PartidoVotacionService partidoVotacionService;

    @Autowired
    private UserService userService;

    @PostMapping("/me")
    public ResponseEntity<PartidoVotacionResponseDTO> guardarMiVoto(
            @PathVariable Long partidoId,
            HttpServletRequest request,
            @Valid @RequestBody PartidoVotacionRequestDTO dto
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(partidoVotacionService.guardarVoto(partidoId, usuario.get(), dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<PartidoVotacionResponseDTO> obtenerMiVoto(
            @PathVariable Long partidoId,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(partidoVotacionService.obtenerMiVoto(partidoId, usuario.get()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

            @GetMapping("/panel-compartido")
        public ResponseEntity<PartidoVotacionPanelCompartidoDTO> obtenerPanelCompartido(
            @PathVariable Long partidoId,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(partidoVotacionService.obtenerPanelCompartido(partidoId, usuario.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/asignacion")
    public ResponseEntity<List<PartidoCompaneroAsignadoDTO>> obtenerAsignacion(
            @PathVariable Long partidoId,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(partidoVotacionService.obtenerAsignacion(partidoId, usuario.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
