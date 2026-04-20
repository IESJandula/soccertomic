package com.worldcup.Back.controller;

import com.worldcup.Back.dto.response.EquipoRapidoNotificacionResponseDTO;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.service.EquipoRapidoNotificacionService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipos-rapidos/notificaciones")
public class EquipoRapidoNotificacionController {

    @Autowired
    private EquipoRapidoNotificacionService equipoRapidoNotificacionService;

    @Autowired
    private UserService userService;

    @GetMapping("/mis-notificaciones")
    public ResponseEntity<List<EquipoRapidoNotificacionResponseDTO>> obtenerMisNotificaciones(HttpServletRequest request) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);
        return ResponseEntity.ok(equipoRapidoNotificacionService.obtenerNotificacionesDeUsuario(usuario));
    }

    @DeleteMapping("/{notificacionId}")
    public ResponseEntity<Void> eliminarNotificacion(HttpServletRequest request, @PathVariable Long notificacionId) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);
        equipoRapidoNotificacionService.eliminarNotificacion(usuario, notificacionId);
        return ResponseEntity.noContent().build();
    }
}