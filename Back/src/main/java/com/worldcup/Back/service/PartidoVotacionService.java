package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.PartidoCompaneroValoracionRequestDTO;
import com.worldcup.Back.dto.response.PartidoCompaneroAsignadoDTO;
import com.worldcup.Back.dto.request.PartidoVotacionRequestDTO;
import com.worldcup.Back.dto.response.PartidoCompaneroValoracionDTO;
import com.worldcup.Back.dto.response.PartidoVotacionPanelCompartidoDTO;
import com.worldcup.Back.dto.response.PartidoVotacionResponseDTO;
import com.worldcup.Back.dto.response.UsuarioVotacionResumenDTO;
import com.worldcup.Back.entity.PartidoCompaneroValoradoEmbeddable;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.PartidoVotacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoPartido;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.PartidoRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartidoVotacionService {

    @Autowired
    private PartidoRepository partidoRepository;

    @Autowired
    private PartidoVotacionRepository partidoVotacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserService userService;

    @Transactional(readOnly = true)
    public UsuarioVotacionResumenDTO obtenerResumenVotacionDeUsuario(UsuarioEntity usuario) {
        List<PartidoEntity> historial = partidoRepository.findHistorialFinalizadoDeUsuario(usuario, EstadoPartido.estadosFinalizadosCompatibles());
        if (historial.isEmpty()) {
            return new UsuarioVotacionResumenDTO(0, 0, 0);
        }

        int vecesDiferencial = 0;
        int valoracionesPositivas = 0;
        int valoracionesNegativas = 0;

        for (PartidoEntity partido : historial) {
            List<PartidoVotacionEntity> votos = partidoVotacionRepository.findByPartido(partido);
            for (PartidoVotacionEntity voto : votos) {
                if (voto.getJugadoresDiferenciales() != null
                        && voto.getJugadoresDiferenciales().stream().anyMatch(id -> id.equals(usuario.getId()))) {
                    vecesDiferencial++;
                }

                if (voto.getValoracionesCompaneros() == null) {
                    continue;
                }

                for (PartidoCompaneroValoradoEmbeddable valoracion : voto.getValoracionesCompaneros()) {
                    if (!valoracion.getJugadorId().equals(usuario.getId())) {
                        continue;
                    }

                    if (valoracion.getPuntuacion() != null && valoracion.getPuntuacion() > 0) {
                        valoracionesPositivas++;
                    } else if (valoracion.getPuntuacion() != null && valoracion.getPuntuacion() < 0) {
                        valoracionesNegativas++;
                    }
                }
            }
        }

        return new UsuarioVotacionResumenDTO(vecesDiferencial, valoracionesPositivas, valoracionesNegativas);
    }

    @Transactional
    public PartidoVotacionResponseDTO guardarVoto(Long partidoId, UsuarioEntity votante, PartidoVotacionRequestDTO dto) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        autoFinalizarSiCorresponde(partido);
        validarPermisosYEstado(partido, votante);

        PartidoVotacionEntity entity = partidoVotacionRepository.findByPartidoAndVotante(partido, votante)
                .orElseGet(PartidoVotacionEntity::new);

        List<UsuarioEntity> participantes = obtenerParticipantesOrdenados(partido);
        List<UsuarioEntity> asignados = obtenerCompanerosAsignados(participantes, votante.getId());
        validarVotacion(partido, votante, dto, participantes, asignados);

        Map<Long, Integer> valoracionesPrevias = entity.getValoracionesCompaneros() == null
            ? Map.of()
            : entity.getValoracionesCompaneros().stream()
                .collect(Collectors.toMap(PartidoCompaneroValoradoEmbeddable::getJugadorId, PartidoCompaneroValoradoEmbeddable::getPuntuacion, (a, b) -> b));

        entity.setPartido(partido);
        entity.setVotante(votante);
        entity.setGolesEquipoAPropuesto(dto.getGolesEquipoAPropuesto());
        entity.setGolesEquipoBPropuesto(dto.getGolesEquipoBPropuesto());
        entity.setIntensidadPartido(normalizarIntensidad(dto.getIntensidadPartido()));
        entity.setPartidoFueParejo(Boolean.TRUE.equals(dto.getPartidoFueParejo()));
        entity.setJugadoresDiferenciales(new ArrayList<>(normalizarIds(dto.getJugadoresDiferenciales())));
        entity.setValoracionesCompaneros(mapearValoraciones(dto.getValoracionesCompaneros()));
        entity.setActualizadaEn(LocalDateTime.now());

        PartidoVotacionEntity saved = partidoVotacionRepository.save(entity);
        aplicarImpactoReputacion(valoracionesPrevias, saved.getValoracionesCompaneros());
        return toResponse(saved, asignados);
    }

    @Transactional
    public PartidoVotacionResponseDTO obtenerMiVoto(Long partidoId, UsuarioEntity votante) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        autoFinalizarSiCorresponde(partido);
        validarPermisosYEstado(partido, votante);

        List<UsuarioEntity> participantes = obtenerParticipantesOrdenados(partido);
        List<UsuarioEntity> asignados = obtenerCompanerosAsignados(participantes, votante.getId());

        PartidoVotacionEntity entity = partidoVotacionRepository.findByPartidoAndVotante(partido, votante)
                .orElseThrow(() -> new ResourceNotFoundException("Votación no encontrada"));

        return toResponse(entity, asignados);
        }

        @Transactional
        public List<PartidoCompaneroAsignadoDTO> obtenerAsignacion(Long partidoId, UsuarioEntity solicitante) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
            .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        autoFinalizarSiCorresponde(partido);
        validarPermisosYEstado(partido, solicitante);

        List<UsuarioEntity> participantes = obtenerParticipantesOrdenados(partido);
        List<UsuarioEntity> asignados = obtenerCompanerosAsignados(participantes, solicitante.getId());
        return toAsignadosDTO(asignados);
    }

    @Transactional
    public PartidoVotacionPanelCompartidoDTO obtenerPanelCompartido(Long partidoId, UsuarioEntity solicitante) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        autoFinalizarSiCorresponde(partido);
        validarPermisosYEstado(partido, solicitante);

        List<PartidoVotacionEntity> votos = partidoVotacionRepository.findByPartido(partido);
        if (votos.isEmpty()) {
            return new PartidoVotacionPanelCompartidoDTO(partidoId, 0, 0.0, 0.0, 0.0, null, List.of(), List.of());
        }

        double promedioA = votos.stream().mapToInt(PartidoVotacionEntity::getGolesEquipoAPropuesto).average().orElse(0);
        double promedioB = votos.stream().mapToInt(PartidoVotacionEntity::getGolesEquipoBPropuesto).average().orElse(0);
        double porcentajeParejo = votos.stream().filter(v -> Boolean.TRUE.equals(v.getPartidoFueParejo())).count() * 100.0 / votos.size();

        Map<String, Long> intensidadConteo = votos.stream()
            .map(PartidoVotacionEntity::getIntensidadPartido)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(i -> i, Collectors.counting()));
        String intensidadMasVotada = intensidadConteo.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        Map<Long, Integer> conteoDiferenciales = new HashMap<>();
        for (PartidoVotacionEntity voto : votos) {
            for (Long jugadorId : voto.getJugadoresDiferenciales()) {
                conteoDiferenciales.merge(jugadorId, 1, Integer::sum);
            }
        }

        Map<Long, List<Integer>> valoracionCompaneros = new HashMap<>();
        for (PartidoVotacionEntity voto : votos) {
            for (PartidoCompaneroValoradoEmbeddable valoracion : voto.getValoracionesCompaneros()) {
                valoracionCompaneros
                        .computeIfAbsent(valoracion.getJugadorId(), ignored -> new ArrayList<>())
                        .add(valoracion.getPuntuacion());
            }
        }

        Set<Long> jugadorIds = new HashSet<>();
        jugadorIds.addAll(conteoDiferenciales.keySet());
        jugadorIds.addAll(valoracionCompaneros.keySet());

        Map<Long, String> nombresJugadores = usuarioRepository.findAllById(jugadorIds)
                .stream()
                .collect(Collectors.toMap(UsuarioEntity::getId, UsuarioEntity::getNombre));

        List<PartidoVotacionPanelCompartidoDTO.JugadorConteoDTO> diferenciales = conteoDiferenciales.entrySet().stream()
            .map(entry -> new PartidoVotacionPanelCompartidoDTO.JugadorConteoDTO(
                        entry.getKey(),
                        nombresJugadores.getOrDefault(entry.getKey(), "Jugador " + entry.getKey()),
                        entry.getValue()
                ))
            .sorted(Comparator.comparingInt(PartidoVotacionPanelCompartidoDTO.JugadorConteoDTO::getVotos).reversed())
                .toList();

        List<PartidoVotacionPanelCompartidoDTO.JugadorPromedioDTO> promedioCompaneros = valoracionCompaneros.entrySet().stream()
                .map(entry -> {
                        double promedio = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                        int positivos = (int) entry.getValue().stream().filter(v -> v > 0).count();
                        int negativos = (int) entry.getValue().stream().filter(v -> v < 0).count();
                return new PartidoVotacionPanelCompartidoDTO.JugadorPromedioDTO(
                            entry.getKey(),
                            nombresJugadores.getOrDefault(entry.getKey(), "Jugador " + entry.getKey()),
                            promedio,
                            entry.getValue().size(),
                            positivos,
                            negativos
                    );
                })
            .sorted(Comparator.comparingDouble(PartidoVotacionPanelCompartidoDTO.JugadorPromedioDTO::getPromedio).reversed())
                .toList();

        return new PartidoVotacionPanelCompartidoDTO(
                partidoId,
                votos.size(),
                promedioA,
                promedioB,
                porcentajeParejo,
                intensidadMasVotada,
                diferenciales,
                promedioCompaneros
        );
    }

    private void validarPermisosYEstado(PartidoEntity partido, UsuarioEntity usuario) {
        boolean esCreador = partido.getOrganizadores().stream()
            .anyMatch(org -> org.getUsuario().getId().equals(usuario.getId()) && org.getRol() == PartidoOrganizadorRol.OWNER);
        boolean participa = partido.getEquipoA().stream().anyMatch(u -> u.getId().equals(usuario.getId()))
                || partido.getEquipoB().stream().anyMatch(u -> u.getId().equals(usuario.getId()));

        if (!(esCreador || participa)) {
            throw new RuntimeException("Solo participantes del partido pueden votar");
        }

        if (partido.getEstado() != EstadoPartido.FINALIZADO) {
            throw new RuntimeException("Las votaciones se habilitan cuando el partido finaliza");
        }
    }

    private void validarVotacion(PartidoEntity partido,
                                 UsuarioEntity votante,
                                 PartidoVotacionRequestDTO dto,
                                 List<UsuarioEntity> participantes,
                                 List<UsuarioEntity> asignados) {
        if (dto.getGolesEquipoAPropuesto() < 0 || dto.getGolesEquipoBPropuesto() < 0) {
            throw new IllegalArgumentException("Los goles propuestos no pueden ser negativos");
        }

        if (dto.getIntensidadPartido() == null || normalizarIntensidad(dto.getIntensidadPartido()) == null) {
            throw new IllegalArgumentException("La intensidad del partido es obligatoria");
        }

        Set<Long> participantesIds = participantes.stream().map(UsuarioEntity::getId).collect(Collectors.toSet());
        for (Long diferencialId : normalizarIds(dto.getJugadoresDiferenciales())) {
            if (!participantesIds.contains(diferencialId)) {
                throw new IllegalArgumentException("Jugador diferencial inválido para este partido");
            }
        }

        if (dto.getValoracionesCompaneros() == null || dto.getValoracionesCompaneros().isEmpty()) {
            return;
        }

        Map<Long, Integer> enviados = new LinkedHashMap<>();
        for (PartidoCompaneroValoracionRequestDTO valoracion : dto.getValoracionesCompaneros()) {
            if (valoracion.getJugadorId().equals(votante.getId())) {
                throw new IllegalArgumentException("No puedes valorarte a ti mismo");
            }
            if (!participantesIds.contains(valoracion.getJugadorId())) {
                throw new IllegalArgumentException("Solo puedes valorar compañeros del partido");
            }
            if (valoracion.getPuntuacion() == null || (valoracion.getPuntuacion() != 1 && valoracion.getPuntuacion() != -1)) {
                throw new IllegalArgumentException("Cada valoración de compañero debe ser pulgar arriba o abajo");
            }
            enviados.put(valoracion.getJugadorId(), valoracion.getPuntuacion());
        }
    }

    private String normalizarIntensidad(String intensidad) {
        if (intensidad == null) {
            return null;
        }
        String valor = intensidad.trim().toUpperCase();
        return switch (valor) {
            case "BAJO", "MEDIO", "ALTO", "MUY_ALTO" -> valor;
            default -> null;
        };
    }

    private List<UsuarioEntity> obtenerParticipantesOrdenados(PartidoEntity partido) {
        List<UsuarioEntity> participantes = new ArrayList<>();
        if (partido.getEquipoA() != null) {
            participantes.addAll(partido.getEquipoA());
        }
        if (partido.getEquipoB() != null) {
            participantes.addAll(partido.getEquipoB());
        }

        return participantes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UsuarioEntity::getId, u -> u, (a, b) -> a))
                .values()
                .stream()
                .sorted(Comparator.comparingLong(UsuarioEntity::getId))
                .toList();
    }

    private List<UsuarioEntity> obtenerCompanerosAsignados(List<UsuarioEntity> participantes, Long votanteId) {
        if (participantes.size() <= 1 || votanteId == null) {
            return List.of();
        }

        return participantes.stream()
                .filter(jugador -> !jugador.getId().equals(votanteId))
                .toList();
    }

    private List<PartidoCompaneroAsignadoDTO> toAsignadosDTO(List<UsuarioEntity> asignados) {
        return asignados.stream()
                .map(u -> new PartidoCompaneroAsignadoDTO(u.getId(), u.getNombre()))
                .toList();
    }

    private void aplicarImpactoReputacion(Map<Long, Integer> valoracionesPrevias,
                                          List<PartidoCompaneroValoradoEmbeddable> valoracionesNuevas) {
        Map<Long, Integer> nuevas = valoracionesNuevas == null
                ? Map.of()
                : valoracionesNuevas.stream()
                    .collect(Collectors.toMap(PartidoCompaneroValoradoEmbeddable::getJugadorId, PartidoCompaneroValoradoEmbeddable::getPuntuacion, (a, b) -> b));

        Set<Long> jugadoresAjustar = new HashSet<>();
        jugadoresAjustar.addAll(valoracionesPrevias.keySet());
        jugadoresAjustar.addAll(nuevas.keySet());

        if (jugadoresAjustar.isEmpty()) {
            return;
        }

        List<UsuarioEntity> usuarios = usuarioRepository.findAllById(jugadoresAjustar);
        for (UsuarioEntity usuario : usuarios) {
            int previo = valoracionesPrevias.getOrDefault(usuario.getId(), 0);
            int nuevo = nuevas.getOrDefault(usuario.getId(), 0);

            int deltaReputacion = nuevo - previo;
            if (deltaReputacion != 0) {
                int reputacionActual = usuario.getReputacionPositiva() == null ? 0 : usuario.getReputacionPositiva();
                usuario.setReputacionPositiva(Math.max(0, reputacionActual + deltaReputacion));

                int puntosActuales = usuario.getPuntos() == null ? 0 : usuario.getPuntos();
                usuario.setPuntos(Math.max(0, puntosActuales + (deltaReputacion * 2)));
                usuario.setNivel(calcularNivelPorReputacion(usuario.getReputacionPositiva()));
            }
        }

        usuarioRepository.saveAll(usuarios);
    }

    private String calcularNivelPorReputacion(Integer reputacionPositiva) {
        int reputacion = reputacionPositiva == null ? 0 : reputacionPositiva;
        if (reputacion >= 50) {
            return "avanzado";
        }
        if (reputacion >= 20) {
            return "intermedio";
        }
        return "beginner";
    }

    private List<Long> normalizarIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(id -> id != null && id > 0).distinct().toList();
    }

    private List<PartidoCompaneroValoradoEmbeddable> mapearValoraciones(List<PartidoCompaneroValoracionRequestDTO> valoraciones) {
        if (valoraciones == null) {
            return new ArrayList<>();
        }

        return valoraciones.stream()
                .filter(v -> v.getJugadorId() != null && v.getPuntuacion() != null)
                .map(v -> new PartidoCompaneroValoradoEmbeddable(v.getJugadorId(), v.getPuntuacion()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private PartidoVotacionResponseDTO toResponse(PartidoVotacionEntity entity, List<UsuarioEntity> asignados) {
        Map<Long, String> nombres = usuarioRepository
                .findAllById(entity.getValoracionesCompaneros().stream().map(PartidoCompaneroValoradoEmbeddable::getJugadorId).toList())
                .stream()
                .collect(Collectors.toMap(UsuarioEntity::getId, UsuarioEntity::getNombre));

        List<PartidoCompaneroValoracionDTO> valoracionesCompaneros = entity.getValoracionesCompaneros().stream()
                .map(v -> new PartidoCompaneroValoracionDTO(
                        v.getJugadorId(),
                        nombres.getOrDefault(v.getJugadorId(), "Jugador " + v.getJugadorId()),
                        v.getPuntuacion()
                ))
                .toList();

        return new PartidoVotacionResponseDTO(
                entity.getId(),
                entity.getPartido().getId(),
                userService.entityToResumenDTO(entity.getVotante()),
                entity.getGolesEquipoAPropuesto(),
                entity.getGolesEquipoBPropuesto(),
                entity.getIntensidadPartido(),
                entity.getPartidoFueParejo(),
                entity.getJugadoresDiferenciales(),
                toAsignadosDTO(asignados),
                valoracionesCompaneros,
                entity.getActualizadaEn()
        );
    }

    @Transactional
    public PartidoEntity autoFinalizarSiCorresponde(PartidoEntity partido) {
        if (partido == null || partido.getFecha() == null) {
            return partido;
        }

        EstadoPartido estadoActual = partido.getEstado() == null ? null : partido.getEstado().canonical();

        if (estadoActual == null || estadoActual.isFinalizado() || estadoActual == EstadoPartido.CANCELADO) {
            return partido;
        }

        int duracion = (partido.getDuracionMinutos() == null || partido.getDuracionMinutos() <= 0)
                ? 60
                : partido.getDuracionMinutos();

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime finEstimado = partido.getFecha().plusMinutes(duracion);
        if (!ahora.isBefore(finEstimado)) {
            partido.setEstado(EstadoPartido.FINALIZADO);
            partido.setActualizadoEn(LocalDateTime.now());
            return partidoRepository.save(partido);
        }

        if (!ahora.isBefore(partido.getFecha()) && estadoActual != EstadoPartido.EN_CURSO) {
            partido.setEstado(EstadoPartido.EN_CURSO);
            partido.setActualizadoEn(LocalDateTime.now());
            return partidoRepository.save(partido);
        }

        return partido;
    }
}
