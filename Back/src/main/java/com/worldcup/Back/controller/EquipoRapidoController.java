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
}
