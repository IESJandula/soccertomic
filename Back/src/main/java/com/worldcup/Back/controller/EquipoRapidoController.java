package com.worldcup.Back.controller;

import com.worldcup.Back.dto.request.EquipoRapidoRequestDTO;
import com.worldcup.Back.dto.response.EquipoRapidoResponseDTO;
import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.EquipoRapidoService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/equipos-rapidos")
public class EquipoRapidoController {

    @Autowired
    private EquipoRapidoService equipoRapidoService;

    @Autowired
    private UserService userService;

    private EquipoRapidoResponseDTO toDTO(EquipoRapidoEntity equipo) {
        return new EquipoRapidoResponseDTO(
                equipo.getId(),
                equipo.getNombre(),
                userService.entityToResumenDTO(equipo.getOwner()),
                equipo.getMiembros().stream().map(userService::entityToResumenDTO).collect(Collectors.toList()),
                equipo.getCapacidad(),
                1 + equipo.getMiembros().size(),
                equipo.getCreadoEn(),
                equipo.getActualizadoEn()
        );
    }

    @GetMapping("/mis-equipos")
    public ResponseEntity<List<EquipoRapidoResponseDTO>> obtenerMisEquipos(HttpServletRequest request) {
        UsuarioEntity owner = userService.obtenerOCrearDesdeRequest(request);

        List<EquipoRapidoResponseDTO> equipos = equipoRapidoService.listarMisEquipos(owner)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(equipos);
    }

    @PostMapping
    public ResponseEntity<EquipoRapidoResponseDTO> crearEquipoRapido(
            HttpServletRequest request,
            @Valid @RequestBody EquipoRapidoRequestDTO body
    ) {
        UsuarioEntity owner = userService.obtenerOCrearDesdeRequest(request);

        EquipoRapidoEntity creado = equipoRapidoService.crearEquipoRapido(owner, body);
        return ResponseEntity.ok(toDTO(creado));
    }

    @DeleteMapping("/{equipoId}")
    public ResponseEntity<Void> eliminarEquipoRapido(
            HttpServletRequest request,
            @PathVariable Long equipoId
    ) {
        UsuarioEntity owner = userService.obtenerOCrearDesdeRequest(request);

        equipoRapidoService.eliminarEquipoRapido(owner, equipoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{equipoId}")
    public ResponseEntity<EquipoRapidoResponseDTO> actualizarEquipoRapido(
            HttpServletRequest request,
            @PathVariable Long equipoId,
            @Valid @RequestBody EquipoRapidoRequestDTO body
    ) {
        UsuarioEntity owner = userService.obtenerOCrearDesdeRequest(request);

        EquipoRapidoEntity actualizado = equipoRapidoService.actualizarEquipoRapido(owner, equipoId, body);
        return ResponseEntity.ok(toDTO(actualizado));
    }

    @PostMapping("/{equipoId}/miembros")
    public ResponseEntity<EquipoRapidoResponseDTO> agregarMiembro(
            HttpServletRequest request,
            @PathVariable Long equipoId,
            @RequestParam Long usuarioId
    ) {
        UsuarioEntity owner = userService.obtenerOCrearDesdeRequest(request);

        EquipoRapidoEntity actualizado = equipoRapidoService.agregarMiembro(owner, equipoId, usuarioId);
        return ResponseEntity.ok(toDTO(actualizado));
    }

    @DeleteMapping("/{equipoId}/miembros/{usuarioId}")
    public ResponseEntity<EquipoRapidoResponseDTO> quitarMiembro(
            HttpServletRequest request,
            @PathVariable Long equipoId,
            @PathVariable Long usuarioId
    ) {
        UsuarioEntity owner = userService.obtenerOCrearDesdeRequest(request);

        EquipoRapidoEntity actualizado = equipoRapidoService.quitarMiembro(owner, equipoId, usuarioId);
        return ResponseEntity.ok(toDTO(actualizado));
    }

    @PostMapping("/{equipoId}/salir")
    public ResponseEntity<EquipoRapidoResponseDTO> salirDeEquipo(
            HttpServletRequest request,
            @PathVariable Long equipoId
    ) {
        UsuarioEntity usuario = userService.obtenerOCrearDesdeRequest(request);

        EquipoRapidoEntity actualizado = equipoRapidoService.salirDeEquipo(usuario, equipoId);
        return ResponseEntity.ok(toDTO(actualizado));
    }
}
