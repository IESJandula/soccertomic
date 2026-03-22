package com.worldcup.Back.controller;

import com.worldcup.Back.dto.request.BalanceTeamsRequestDTO;
import com.worldcup.Back.dto.request.PartidoRequestDTO;
import com.worldcup.Back.dto.response.EquiposBalanceadosResponseDTO;
import com.worldcup.Back.dto.response.PartidoBalanceAnalisisResponseDTO;
import com.worldcup.Back.dto.response.PartidoResponseDTO;
import com.worldcup.Back.dto.response.PartidoDetalleDTO;
import com.worldcup.Back.dto.response.PartidoHistorialDTO;
import com.worldcup.Back.dto.response.PartidoOrganizadorDTO;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoPartido;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.AmistadService;
import com.worldcup.Back.service.PartidoService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/partidos")
public class PartidoController {

    @Autowired
    private PartidoService partidoService;

    @Autowired
    private UserService userService;

    @Autowired
    private AmistadService amistadService;

    private List<PartidoOrganizadorDTO> mapOrganizadores(PartidoEntity partido) {
        List<PartidoOrganizadorEntity> relaciones = partidoService.obtenerOrganizadores(partido.getId());
        return relaciones.stream()
                .map(rel -> new PartidoOrganizadorDTO(
                        userService.entityToResumenDTO(rel.getUsuario()),
                        rel.getRol().toString(),
                        rel.getAsignadoEn()
                ))
                .collect(Collectors.toList());
    }

    // Helper para convertir Entity a DTO
    private PartidoResponseDTO entityToDTO(PartidoEntity partido) {
        int totalJugadores = partido.getJugadoresInscritos().size() + partido.getEquipoA().size() + partido.getEquipoB().size();
        UsuarioEntity owner = partidoService.obtenerOwnerDelPartido(partido);
        List<PartidoOrganizadorDTO> organizadores = mapOrganizadores(partido);
        return new PartidoResponseDTO(
                partido.getId(),
                userService.entityToResumenDTO(owner),
                userService.entityToResumenDTO(owner),
                organizadores,
                partido.getFecha(),
                partido.getLugar(),
                partido.getJugadoresPorEquipo(),
                partido.getTipo().toString(),
                partido.getEstado().toString(),
                partido.getEquipoA().size(),
                partido.getEquipoB().size(),
                totalJugadores,
                partido.getColorEquipoA(),
                partido.getColorEquipoB(),
                partido.getCreadoEn(),
                partido.getActualizadoEn()
        );
    }

    private PartidoDetalleDTO entityToDetalleDTO(PartidoEntity partido, UsuarioEntity usuarioActual) {
        UsuarioEntity owner = partidoService.obtenerOwnerDelPartido(partido);
        List<PartidoOrganizadorDTO> organizadores = mapOrganizadores(partido);
        boolean estaInscrito = usuarioActual != null && (
                partido.getJugadoresInscritos().contains(usuarioActual)
                        || partido.getEquipoA().contains(usuarioActual)
                        || partido.getEquipoB().contains(usuarioActual)
        );
        return new PartidoDetalleDTO(
                partido.getId(),
                userService.entityToResumenDTO(owner),
                userService.entityToResumenDTO(owner),
                organizadores,
                partido.getFecha(),
                partido.getLugar(),
                partido.getJugadoresPorEquipo(),
                partido.getTipo().toString(),
                partido.getEstado().toString(),
                partido.getJugadoresInscritos().stream().map(userService::entityToResumenDTO).collect(Collectors.toList()),
                partido.getEquipoA().stream().map(userService::entityToResumenDTO).collect(Collectors.toList()),
                partido.getEquipoB().stream().map(userService::entityToResumenDTO).collect(Collectors.toList()),
                partido.getJugadoresInscritos().size() + partido.getEquipoA().size() + partido.getEquipoB().size(),
                partido.getJugadoresInscritos().size() + partido.getEquipoA().size() + partido.getEquipoB().size() < partido.getJugadoresPorEquipo() * 2,
                estaInscrito,
                partido.getColorEquipoA(),
                partido.getColorEquipoB(),
                partido.getCreadoEn(),
                partido.getActualizadoEn()
        );
    }

    @PostMapping
    public ResponseEntity<PartidoResponseDTO> crearPartido(
            HttpServletRequest request,
            @RequestBody PartidoRequestDTO dto
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> creador = userService.buscarPorFirebaseUid(uid);
        
        if (creador.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        PartidoEntity partido = new PartidoEntity();
        partido.setFecha(dto.getFecha());
        partido.setLugar(dto.getLugar());
        partido.setJugadoresPorEquipo(dto.getJugadoresPorEquipo());
        partido.setDuracionMinutos(dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : 60);
        if (dto.getTipo() != null) {
            partido.setTipo("PUBLICO".equalsIgnoreCase(dto.getTipo()) ? 
                com.worldcup.Back.entity.enums.TipoPartido.PUBLICO : 
                com.worldcup.Back.entity.enums.TipoPartido.PRIVADO);
        }
        // Asignar colores de equipos (por defecto Blanco y Oscuro si no se especifican)
        partido.setColorEquipoA(dto.getColorEquipoA() != null ? dto.getColorEquipoA() : "Blanco");
        partido.setColorEquipoB(dto.getColorEquipoB() != null ? dto.getColorEquipoB() : "Oscuro");
        
        PartidoEntity creado = partidoService.crearPartido(partido, creador.get());
        creado = partidoService.inscribirOwnerInicial(creado.getId(), creador.get());
        
        return ResponseEntity.ok(entityToDTO(creado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidoResponseDTO> obtenerPartido(@PathVariable Long id) {
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(id);
        return partido.map(p -> ResponseEntity.ok(entityToDTO(p)))
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mis-partidos")
    public ResponseEntity<List<PartidoResponseDTO>> obtenerMisPartidos(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);
        
        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<PartidoEntity> partidos = partidoService.obtenerPartidosInscritos(usuario.get());
        List<PartidoResponseDTO> dtos = partidos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/mi-historial")
    public ResponseEntity<List<PartidoHistorialDTO>> obtenerMiHistorial(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(partidoService.obtenerHistorialPartidosFinalizados(usuario.get()));
    }

    @GetMapping("/historial/public/{usuarioId}")
    public ResponseEntity<List<PartidoHistorialDTO>> obtenerHistorialPublicoDeAmigo(
            HttpServletRequest request,
            @PathVariable Long usuarioId
    ) {
        String uid = FirebaseRequestContext.requireUid(request);

        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        Optional<UsuarioEntity> objetivo = userService.buscarPorId(usuarioId);

        if (solicitante.isEmpty() || objetivo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!amistadService.sonAmigos(solicitante.get(), objetivo.get())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(partidoService.obtenerHistorialPartidosFinalizados(objetivo.get()));
    }

    @GetMapping("/futuros")
    public ResponseEntity<List<PartidoResponseDTO>> obtenerPartidosFuturos() {
        List<PartidoEntity> partidos = partidoService.obtenerPartidosFuturos();
        List<PartidoResponseDTO> dtos = partidos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartidoResponseDTO> actualizarPartido(
            @PathVariable Long id,
            @RequestBody PartidoRequestDTO dto
    ) {
        try {
            PartidoEntity datosActualizados = new PartidoEntity();
            datosActualizados.setFecha(dto.getFecha());
            datosActualizados.setLugar(dto.getLugar());
            datosActualizados.setJugadoresPorEquipo(dto.getJugadoresPorEquipo());
            datosActualizados.setDuracionMinutos(dto.getDuracionMinutos());
            
            PartidoEntity actualizado = partidoService.actualizarPartido(id, datosActualizados);
            return ResponseEntity.ok(entityToDTO(actualizado));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/tipo")
    public ResponseEntity<PartidoResponseDTO> cambiarTipoPartido(
            @PathVariable Long id,
            @RequestParam String tipo,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);

        if (solicitante.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PartidoEntity actualizado = partidoService.cambiarATipo(id, tipo, solicitante.get());
            return ResponseEntity.ok(entityToDTO(actualizado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PartidoResponseDTO> cambiarEstadoPartido(
            @PathVariable Long id,
            @RequestParam EstadoPartido estado
    ) {
        try {
            PartidoEntity actualizado = partidoService.actualizarEstado(id, estado);
            return ResponseEntity.ok(entityToDTO(actualizado));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/balanceado")
    public ResponseEntity<Boolean> verificarBalanceado(@PathVariable Long id) {
        boolean balanceado = partidoService.estaBalanceado(id);
        return ResponseEntity.ok(balanceado);
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<PartidoDetalleDTO> obtenerDetallePartido(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(id);
        if (partido.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PartidoEntity p = partido.get();
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuarioActual = userService.buscarPorFirebaseUid(uid);

        // Inicializar colecciones si son null (para partidos antiguos)
        if (p.getJugadoresInscritos() == null) {
            p.setJugadoresInscritos(new ArrayList<>());
        }
        if (p.getEquipoA() == null) {
            p.setEquipoA(new ArrayList<>());
        }
        if (p.getEquipoB() == null) {
            p.setEquipoB(new ArrayList<>());
        }

        PartidoDetalleDTO detalle = entityToDetalleDTO(p, usuarioActual.orElse(null));

        return ResponseEntity.ok(detalle);
    }

    @PostMapping("/{id}/inscribirse")
    public ResponseEntity<PartidoDetalleDTO> inscribirse(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PartidoEntity actualizado = partidoService.inscribirseAPartido(id, usuario.get());
            // Retornar el detalle actualizado
            PartidoDetalleDTO detalle = entityToDetalleDTO(actualizado, usuario.get());
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

        @PostMapping("/{id}/desinscribirse")
        public ResponseEntity<PartidoDetalleDTO> desinscribirse(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            partidoService.desinscribirseDePartido(id, usuario.get());
            Optional<PartidoEntity> actualizado = partidoService.obtenerPorId(id);
            if (actualizado.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PartidoEntity p = actualizado.get();
            PartidoDetalleDTO detalle = entityToDetalleDTO(p, usuario.get());
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/jugadores/{usuarioId}/equipo")
    public ResponseEntity<PartidoDetalleDTO> moverJugadorAEquipo(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @RequestParam String equipo,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> creador = userService.buscarPorFirebaseUid(uid);
        
        if (creador.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(id);

        if (partido.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            PartidoEntity actualizado = partidoService.asignarJugadorAEquipo(id, usuarioId, equipo, creador.get());
            PartidoDetalleDTO detalle = entityToDetalleDTO(actualizado, creador.get());
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/jugadores/{usuarioId}/sin-equipo")
    public ResponseEntity<PartidoDetalleDTO> moverJugadorASinEquipo(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> creador = userService.buscarPorFirebaseUid(uid);
        
        if (creador.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(id);

        if (partido.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            PartidoEntity actualizado = partidoService.moverJugadorASinEquipo(id, usuarioId, creador.get());
            PartidoDetalleDTO detalle = entityToDetalleDTO(actualizado, creador.get());
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/jugadores/{usuarioId}/cambiar-equipo")
    public ResponseEntity<PartidoDetalleDTO> cambiarJugadorDeEquipo(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @RequestParam String equipoDestino,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> creador = userService.buscarPorFirebaseUid(uid);
        
        if (creador.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(id);

        if (partido.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            PartidoEntity actualizado = partidoService.moverJugador(id, usuarioId, equipoDestino, creador.get());
            PartidoDetalleDTO detalle = entityToDetalleDTO(actualizado, creador.get());
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPartido(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> creador = userService.buscarPorFirebaseUid(uid);

        if (creador.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            partidoService.eliminarPartido(id, creador.get());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/balancear-equipos")
    public ResponseEntity<EquiposBalanceadosResponseDTO> balancearEquipos(
            @PathVariable Long id,
            HttpServletRequest request,
            @Valid @RequestBody BalanceTeamsRequestDTO body
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            EquiposBalanceadosResponseDTO response = partidoService.balancearEquipos(id, body.getPlayerIds(), usuario.get());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/{id}/analisis-balance")
    public ResponseEntity<PartidoBalanceAnalisisResponseDTO> obtenerAnalisisBalance(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(partidoService.analizarBalancePartido(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/transferir-creador")
    public ResponseEntity<PartidoDetalleDTO> transferirCreador(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody Map<String, Long> body
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> creadorActual = userService.buscarPorFirebaseUid(uid);
        
        if (creadorActual.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Long nuevoCreadorId = body.get("nuevoCreadorId");
        if (nuevoCreadorId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PartidoEntity actualizado = partidoService.transferirCreador(id, creadorActual.get(), nuevoCreadorId);
            return ResponseEntity.ok(entityToDetalleDTO(actualizado, creadorActual.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/organizadores")
    public ResponseEntity<PartidoDetalleDTO> asignarCoOrganizador(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody Map<String, Long> body
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        if (solicitante.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Long usuarioId = body.get("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PartidoEntity actualizado = partidoService.asignarCoOrganizador(id, solicitante.get(), usuarioId);
            return ResponseEntity.ok(entityToDetalleDTO(actualizado, solicitante.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/organizadores/{usuarioId}")
    public ResponseEntity<PartidoDetalleDTO> revocarOrganizador(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        if (solicitante.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PartidoEntity actualizado = partidoService.revocarOrganizador(id, solicitante.get(), usuarioId);
            return ResponseEntity.ok(entityToDetalleDTO(actualizado, solicitante.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/organizadores/salir")
    public ResponseEntity<?> salirComoOrganizador(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        if (solicitante.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean confirmarEliminacion = body != null && Boolean.TRUE.equals(body.get("confirmarEliminacion"));
        Long nuevoOwnerId = null;
        if (body != null && body.get("nuevoOwnerId") != null) {
            Object raw = body.get("nuevoOwnerId");
            if (raw instanceof Number num) {
                nuevoOwnerId = num.longValue();
            }
        }

        try {
            PartidoEntity actualizado = partidoService.salirComoOrganizador(id, solicitante.get(), confirmarEliminacion, nuevoOwnerId);
            return ResponseEntity.ok(entityToDetalleDTO(actualizado, solicitante.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reservar-pista")
    public ResponseEntity<?> reservarPista(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        if (solicitante.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        BigDecimal precioPista = null;
        if (body != null && body.get("precioPista") != null) {
            Object raw = body.get("precioPista");
            if (raw instanceof Number num) {
                precioPista = BigDecimal.valueOf(num.doubleValue());
            } else if (raw instanceof String text && !text.isBlank()) {
                try {
                    precioPista = new BigDecimal(text.replace(',', '.'));
                } catch (NumberFormatException ignored) {
                    precioPista = null;
                }
            }
        }

        try {
            PartidoEntity actualizado = partidoService.reservarPista(id, solicitante.get(), precioPista);
            return ResponseEntity.ok(entityToDetalleDTO(actualizado, solicitante.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/reservas/estado-pago")
    public ResponseEntity<?> obtenerEstadoPagoReserva(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        if (solicitante.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(partidoService.obtenerEstadoPagoReserva(id, solicitante.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
