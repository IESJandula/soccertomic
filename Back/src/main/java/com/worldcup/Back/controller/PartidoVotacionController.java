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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/partidos/{partidoId}/votaciones")
public class PartidoVotacionController {

    @Autowired
    private PartidoVotacionService partidoVotacionService;

    @Autowired
    private UserService userService;

    @PostMapping("/me")
    public ResponseEntity<?> guardarMiVoto(
            @PathVariable Long partidoId,
            HttpServletRequest request,
            @Valid @RequestBody PartidoVotacionRequestDTO dto
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            return ResponseEntity.ok(partidoVotacionService.guardarVoto(partidoId, usuario, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage() == null || e.getMessage().isBlank()) {
                throw e;
            }
            return ResponseEntity.status(403).body(errorBody(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerMiVoto(
            @PathVariable Long partidoId,
            HttpServletRequest request
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            return ResponseEntity.ok(partidoVotacionService.obtenerMiVoto(partidoId, usuario));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() == null || e.getMessage().isBlank()) {
                throw e;
            }
            return ResponseEntity.status(403).body(errorBody(e.getMessage()));
        }
    }

    @GetMapping("/panel-compartido")
    public ResponseEntity<?> obtenerPanelCompartido(
            @PathVariable Long partidoId,
            HttpServletRequest request
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            return ResponseEntity.ok(partidoVotacionService.obtenerPanelCompartido(partidoId, usuario));
        } catch (RuntimeException e) {
            if (e.getMessage() == null || e.getMessage().isBlank()) {
                throw e;
            }
            return ResponseEntity.status(403).body(errorBody(e.getMessage()));
        }
    }

    @GetMapping("/asignacion")
        public ResponseEntity<?> obtenerAsignacion(
            @PathVariable Long partidoId,
            HttpServletRequest request
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        try {
            return ResponseEntity.ok(partidoVotacionService.obtenerAsignacion(partidoId, usuario));
        } catch (RuntimeException e) {
            if (e.getMessage() == null || e.getMessage().isBlank()) {
                throw e;
            }
            return ResponseEntity.status(403).body(errorBody(e.getMessage()));
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message != null ? message : "Unexpected error occurred");
        return body;
    }
}
