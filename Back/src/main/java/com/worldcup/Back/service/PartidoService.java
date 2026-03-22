package com.worldcup.Back.service;

import com.worldcup.Back.dto.response.EquiposBalanceadosResponseDTO;
import com.worldcup.Back.dto.response.JugadorBalanceadoDTO;
import com.worldcup.Back.dto.response.PartidoBalanceAnalisisResponseDTO;
import com.worldcup.Back.dto.response.PartidoHistorialDTO;
import com.worldcup.Back.entity.PlayerProfileEntity;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.InvitacionEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.PartidoVotacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoPartido;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.PartidoOrganizadorRepository;
import com.worldcup.Back.repository.PartidoRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartidoService {

    private static final Logger logger = LoggerFactory.getLogger(PartidoService.class);
    private static final DateTimeFormatter FORMATO_FECHA_NOTIFICACION = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", new Locale("es", "ES"));

    private static final List<String> POSICIONES_CLAVE = List.of("ARQUERO", "DEFENSA", "MEDIOCAMPISTA", "DELANTERO");

    @Autowired
    private PartidoRepository partidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PartidoOrganizadorRepository partidoOrganizadorRepository;

    @Autowired
    private PartidoVotacionRepository partidoVotacionRepository;

    @Autowired
    private PlayerProfileService playerProfileService;

    @Autowired
    private TeamBalancingService teamBalancingService;

    @Autowired
    private InvitacionService invitacionService;

    private Optional<PartidoOrganizadorEntity> buscarRelacionOrganizador(PartidoEntity partido, UsuarioEntity usuario) {
        return partidoOrganizadorRepository.findByPartidoAndUsuario(partido, usuario);
    }

    private boolean esOrganizador(PartidoEntity partido, UsuarioEntity usuario) {
        return buscarRelacionOrganizador(partido, usuario).isPresent();
    }

    private boolean esOwner(PartidoEntity partido, UsuarioEntity usuario) {
        return buscarRelacionOrganizador(partido, usuario)
                .map(rel -> rel.getRol() == PartidoOrganizadorRol.OWNER)
                .orElse(false);
    }

    private void validarPermisoOwner(PartidoEntity partido, UsuarioEntity usuario, String mensaje) {
        if (!esOwner(partido, usuario)) {
            throw new RuntimeException(mensaje);
        }
    }

    private void validarPermisoOrganizador(PartidoEntity partido, UsuarioEntity usuario, String mensaje) {
        if (!esOrganizador(partido, usuario)) {
            throw new RuntimeException(mensaje);
        }
    }

    private void establecerOwnerUnico(PartidoEntity partido, Long ownerUsuarioId) {
        List<PartidoOrganizadorEntity> relaciones = partidoOrganizadorRepository.findByPartidoOrderByAsignadoEnAsc(partido);
        boolean existe = false;
        for (PartidoOrganizadorEntity rel : relaciones) {
            if (rel.getUsuario().getId().equals(ownerUsuarioId)) {
                rel.setRol(PartidoOrganizadorRol.OWNER);
                existe = true;
            } else {
                rel.setRol(PartidoOrganizadorRol.CO_ORGANIZER);
            }
        }
        if (!existe) {
            throw new RuntimeException("El nuevo OWNER debe ser organizador del partido");
        }
        partidoOrganizadorRepository.saveAll(relaciones);
    }

    private UsuarioEntity obtenerOwnerPrincipal(PartidoEntity partido) {
        List<PartidoOrganizadorEntity> owners = partidoOrganizadorRepository.findByPartidoAndRolOrderByAsignadoEnAsc(
                partido,
                PartidoOrganizadorRol.OWNER
        );
        if (!owners.isEmpty()) {
            return owners.get(0).getUsuario();
        }

        List<PartidoOrganizadorEntity> organizadores = partidoOrganizadorRepository.findByPartidoOrderByAsignadoEnAsc(partido);
        if (!organizadores.isEmpty()) {
            return organizadores.get(0).getUsuario();
        }

        if (partido.getJugadoresInscritos() != null && !partido.getJugadoresInscritos().isEmpty()) {
            return partido.getJugadoresInscritos().get(0);
        }
        if (partido.getEquipoA() != null && !partido.getEquipoA().isEmpty()) {
            return partido.getEquipoA().get(0);
        }
        if (partido.getEquipoB() != null && !partido.getEquipoB().isEmpty()) {
            return partido.getEquipoB().get(0);
        }

        throw new RuntimeException("El partido no tiene OWNER asignado");
    }

    private void cancelarPartido(PartidoEntity partido, String motivo) {
        partido.setEstado(EstadoPartido.CANCELADO);
        partido.setMotivoCancelacion(motivo);
        partido.setCanceladoEn(LocalDateTime.now());
        partido.setActualizadoEn(LocalDateTime.now());
    }

    private void validarPartidoEditable(PartidoEntity partido) {
        if (partido.getEstado() == EstadoPartido.CANCELADO) {
            throw new RuntimeException("El partido está cancelado y no admite más cambios");
        }
        if (partido.getEstado() == EstadoPartido.FINALIZADO) {
            throw new RuntimeException("El partido está finalizado y no admite más cambios");
        }
        if (Boolean.TRUE.equals(partido.getArchivado())) {
            throw new RuntimeException("El partido está archivado y no admite más cambios");
        }
    }

    private void notificarCancelacion(PartidoEntity partido, String motivo, UsuarioEntity cancelador) {
        int totalJugadores = partido.getJugadoresInscritos().size() + partido.getEquipoA().size() + partido.getEquipoB().size();
        logger.info("[NOTIFICACION_PARTIDO] partido={} jugadores={} motivo={}", partido.getId(), totalJugadores, motivo);

        Set<UsuarioEntity> asistentes = new HashSet<>();
        if (partido.getJugadoresInscritos() != null) asistentes.addAll(partido.getJugadoresInscritos());
        if (partido.getEquipoA() != null) asistentes.addAll(partido.getEquipoA());
        if (partido.getEquipoB() != null) asistentes.addAll(partido.getEquipoB());

        asistentes.stream()
                .forEach(usuario -> invitacionService.crearNotificacionCancelacion(partido, usuario));
    }

    private void notificarReservaPista(PartidoEntity partido, UsuarioEntity ejecutor, BigDecimal precioPista) {
        Set<UsuarioEntity> destinatarios = new HashSet<>();
        if (partido.getJugadoresInscritos() != null) destinatarios.addAll(partido.getJugadoresInscritos());
        if (partido.getEquipoA() != null) destinatarios.addAll(partido.getEquipoA());
        if (partido.getEquipoB() != null) destinatarios.addAll(partido.getEquipoB());
        if (partido.getOrganizadores() != null) {
            partido.getOrganizadores().forEach(rel -> {
                if (rel.getUsuario() != null) {
                    destinatarios.add(rel.getUsuario());
                }
            });
        }

        Set<Long> personasParaDividir = new HashSet<>();
        destinatarios.stream().map(UsuarioEntity::getId).forEach(personasParaDividir::add);
        if (ejecutor != null && ejecutor.getId() != null) {
            personasParaDividir.add(ejecutor.getId());
        }

        int divisor = Math.max(1, personasParaDividir.size());
        BigDecimal precioNormalizado = precioPista.setScale(2, RoundingMode.HALF_UP);
        BigDecimal parteIndividual = precioNormalizado.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
        String lugar = partido.getLugar() == null ? "Lugar no definido" : partido.getLugar();
        String fecha = partido.getFecha() == null ? "Fecha no definida" : partido.getFecha().format(FORMATO_FECHA_NOTIFICACION);
        String mensaje = "Partido reservado en " + lugar + " el " + fecha
            + ". Tu parte: $" + parteIndividual.toPlainString()
            + ". Monto total: $" + precioNormalizado.toPlainString() + ".";

        Long ejecutorId = ejecutor != null ? ejecutor.getId() : null;
        destinatarios.forEach(usuario -> {
            boolean pagada = ejecutorId != null && usuario.getId().equals(ejecutorId);
            invitacionService.crearNotificacionReserva(
                    partido,
                    usuario,
                    mensaje,
                    precioNormalizado,
                    parteIndividual,
                    ejecutorId,
                    ejecutor != null ? ejecutor.getNombre() : null,
                    pagada
            );
        });
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> obtenerEstadoPagoReserva(Long partidoId, UsuarioEntity solicitante) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        validarPermisoOrganizador(partido, solicitante, "Solo una persona organizadora puede ver el estado de pagos");

        List<InvitacionEntity> invitaciones = invitacionService.obtenerInvitacionesDePartido(partido);
        Map<Long, Boolean> estadoPago = new HashMap<>();

        invitaciones.stream()
                .filter(inv -> inv.getPrecioTotalPista() != null && inv.getParteIndividual() != null)
                .forEach(inv -> {
                    if (inv.getUsuario() != null && inv.getUsuario().getId() != null) {
                        estadoPago.put(inv.getUsuario().getId(), Boolean.TRUE.equals(inv.getPagada()));
                    }
                });

        return estadoPago;
    }

    @Transactional
    public PartidoEntity crearPartido(PartidoEntity partido, UsuarioEntity ownerInicial) {
        // Inicializar valores por defecto si no vinieron en el JSON
        if (partido.getEstado() == null) {
            partido.setEstado(EstadoPartido.BORRADOR);
        }
        if (partido.getTipo() == null) {
            partido.setTipo(com.worldcup.Back.entity.enums.TipoPartido.PRIVADO);
        }
        if (partido.getJugadoresPorEquipo() == null) {
            partido.setJugadoresPorEquipo(6);
        }
        if (partido.getDuracionMinutos() == null || partido.getDuracionMinutos() <= 0) {
            partido.setDuracionMinutos(60);
        }
        PartidoEntity guardado = partidoRepository.save(partido);

        PartidoOrganizadorEntity owner = new PartidoOrganizadorEntity();
        owner.setPartido(guardado);
        owner.setUsuario(ownerInicial);
        owner.setRol(PartidoOrganizadorRol.OWNER);
        partidoOrganizadorRepository.save(owner);

        return partidoRepository.findById(guardado.getId()).orElse(guardado);
    }

    public Optional<PartidoEntity> obtenerPorId(Long id) {
        Optional<PartidoEntity> partido = partidoRepository.findById(id);
        partido.ifPresent(this::autoFinalizarSiCorresponde);
        return partido;
    }

    @Transactional
    public PartidoEntity inscribirOwnerInicial(Long partidoId, UsuarioEntity owner) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        if (!partido.getJugadoresInscritos().contains(owner)
                && !partido.getEquipoA().contains(owner)
                && !partido.getEquipoB().contains(owner)) {
            partido.getJugadoresInscritos().add(owner);
            partido.setActualizadoEn(LocalDateTime.now());
        }

        return partidoRepository.save(partido);
    }

    public List<PartidoEntity> obtenerPartidosInscritos(UsuarioEntity usuario) {
        List<PartidoEntity> partidos = partidoRepository.findByParticipante(usuario);
        partidos.forEach(this::autoFinalizarSiCorresponde);
        return partidos;
    }

    @Transactional
    public List<PartidoHistorialDTO> obtenerHistorialPartidosFinalizados(UsuarioEntity usuario) {
        List<PartidoEntity> partidosCreados = partidoRepository.findByOrganizador(usuario);
        partidosCreados.forEach(this::autoFinalizarSiCorresponde);

        return partidoRepository.findHistorialFinalizadoDeUsuario(usuario, EstadoPartido.estadosFinalizadosCompatibles())
                .stream()
                .map(partido -> toHistorialDTO(partido, usuario))
                .toList();
    }

    public List<PartidoEntity> obtenerPartidosFuturos() {
        LocalDateTime ahora = LocalDateTime.now();
        List<PartidoEntity> partidos = partidoRepository.findByFechaBetween(ahora, ahora.plusDays(365));
        partidos.forEach(this::autoFinalizarSiCorresponde);
        return partidos;
    }

    @Transactional
    public PartidoEntity actualizarPartido(Long id, PartidoEntity datosActualizados) {
        Optional<PartidoEntity> partido = partidoRepository.findById(id);
        if (partido.isPresent()) {
            PartidoEntity p = partido.get();
            validarPartidoEditable(p);
            if (datosActualizados.getFecha() != null) p.setFecha(datosActualizados.getFecha());
            if (datosActualizados.getLugar() != null) p.setLugar(datosActualizados.getLugar());
            if (datosActualizados.getJugadoresPorEquipo() != null) p.setJugadoresPorEquipo(datosActualizados.getJugadoresPorEquipo());
            if (datosActualizados.getDuracionMinutos() != null && datosActualizados.getDuracionMinutos() > 0) {
                p.setDuracionMinutos(datosActualizados.getDuracionMinutos());
            }
            p.setActualizadoEn(LocalDateTime.now());
            return partidoRepository.save(p);
        }
        throw new ResourceNotFoundException("Partido", id);
    }

    @Transactional
    public PartidoEntity cambiarATipo(Long id, String tipo, UsuarioEntity solicitante) {
        Optional<PartidoEntity> partido = partidoRepository.findById(id);
        if (partido.isPresent()) {
            PartidoEntity p = partido.get();
            validarPartidoEditable(p);
            validarPermisoOrganizador(p, solicitante, "Solo un organizador puede abrir el partido al público");
            p.setTipo(tipo.equalsIgnoreCase("PUBLICO") ? com.worldcup.Back.entity.enums.TipoPartido.PUBLICO : com.worldcup.Back.entity.enums.TipoPartido.PRIVADO);
            p.setActualizadoEn(LocalDateTime.now());
            return partidoRepository.save(p);
        }
        throw new ResourceNotFoundException("Partido", id);
    }

    @Transactional
    public PartidoEntity actualizarEstado(Long id, EstadoPartido estado) {
        Optional<PartidoEntity> partido = partidoRepository.findById(id);
        if (partido.isPresent()) {
            PartidoEntity p = partido.get();
            p.setEstado(estado);
            return partidoRepository.save(p);
        }
        throw new RuntimeException("Partido no encontrado");
    }

    public boolean estaBalanceado(Long id) {
        Optional<PartidoEntity> partido = partidoRepository.findById(id);
        if (partido.isPresent()) {
            PartidoEntity p = partido.get();
            return p.getEquipoA().size() == p.getEquipoB().size() && 
                   p.getEquipoA().size() == p.getJugadoresPorEquipo();
        }
        return false;
    }

    @Transactional
    public PartidoEntity inscribirseAPartido(Long partidoId, UsuarioEntity usuario) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();
        validarPartidoEditable(p);
        
        // Verificar si ya está inscrito
        if (p.getJugadoresInscritos().contains(usuario) || 
            p.getEquipoA().contains(usuario) || 
            p.getEquipoB().contains(usuario)) {
            throw new RuntimeException("Ya estás inscrito en este partido");
        }

        // Verificar si hay espacio
        int totalJugadores = p.getJugadoresInscritos().size() + p.getEquipoA().size() + p.getEquipoB().size();
        int maxJugadores = p.getJugadoresPorEquipo() * 2;
        if (totalJugadores >= maxJugadores) {
            throw new RuntimeException("El partido está completo");
        }

        // Agregar a la lista de inscritos (sin equipo asignado)
        p.getJugadoresInscritos().add(usuario);

        p.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(p);
    }

    @Transactional
    public void desinscribirseDePartido(Long partidoId, UsuarioEntity usuario) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();
        validarPartidoEditable(p);
        p.getJugadoresInscritos().remove(usuario);
        p.getEquipoA().remove(usuario);
        p.getEquipoB().remove(usuario);
        p.setActualizadoEn(LocalDateTime.now());
        partidoRepository.save(p);
    }

    @Transactional
    public PartidoEntity asignarJugadorAEquipo(Long partidoId, Long jugadorId, String equipo, UsuarioEntity creador) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();
        validarPartidoEditable(p);
        
        validarPermisoOrganizador(p, creador, "Solo un organizador puede asignar jugadores a equipos");
        
        // Buscar el jugador
        Optional<UsuarioEntity> jugadorOpt = usuarioRepository.findById(jugadorId);
        if (jugadorOpt.isEmpty()) {
            throw new ResourceNotFoundException("Usuario", jugadorId);
        }
        UsuarioEntity jugador = jugadorOpt.get();
        
        // Verificar que el jugador esté inscrito
        if (!p.getJugadoresInscritos().contains(jugador) && 
            !p.getEquipoA().contains(jugador) && 
            !p.getEquipoB().contains(jugador)) {
            throw new RuntimeException("El jugador no está inscrito en este partido");
        }
        
        // Remover de donde esté
        p.getJugadoresInscritos().remove(jugador);
        p.getEquipoA().remove(jugador);
        p.getEquipoB().remove(jugador);
        
        // Asignar al equipo correspondiente
        if ("A".equalsIgnoreCase(equipo)) {
            if (p.getEquipoA().size() >= p.getJugadoresPorEquipo()) {
                throw new RuntimeException("Equipo A está completo");
            }
            p.getEquipoA().add(jugador);
        } else if ("B".equalsIgnoreCase(equipo)) {
            if (p.getEquipoB().size() >= p.getJugadoresPorEquipo()) {
                throw new RuntimeException("Equipo B está completo");
            }
            p.getEquipoB().add(jugador);
        } else if ("INSCRITOS".equalsIgnoreCase(equipo)) {
            // Mover a la lista de inscritos (sin equipo)
            p.getJugadoresInscritos().add(jugador);
        } else {
            throw new RuntimeException("Equipo inválido. Debe ser 'A', 'B' o 'INSCRITOS'");
        }
        
        p.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(p);
    }

    @Transactional
    public PartidoEntity moverJugador(Long partidoId, Long jugadorId, String equipoDestino, UsuarioEntity creador) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();
        
        validarPermisoOrganizador(p, creador, "Solo un organizador puede mover jugadores");
        
        // Buscar el jugador
        Optional<UsuarioEntity> jugadorOpt = usuarioRepository.findById(jugadorId);
        if (jugadorOpt.isEmpty()) {
            throw new ResourceNotFoundException("Usuario", jugadorId);
        }
        UsuarioEntity jugador = jugadorOpt.get();
        
        // Verificar que el jugador esté en algún equipo
        boolean estaEnEquipoA = p.getEquipoA().contains(jugador);
        boolean estaEnEquipoB = p.getEquipoB().contains(jugador);
        
        if (!estaEnEquipoA && !estaEnEquipoB) {
            throw new RuntimeException("El jugador no está en ningún equipo");
        }
        
        // Remover del equipo actual
        p.getEquipoA().remove(jugador);
        p.getEquipoB().remove(jugador);

        // Agregar al equipo destino
        if ("A".equalsIgnoreCase(equipoDestino)) {
            if (p.getEquipoA().size() >= p.getJugadoresPorEquipo()) {
                throw new RuntimeException("Equipo A está completo");
            }
            p.getEquipoA().add(jugador);
        } else if ("B".equalsIgnoreCase(equipoDestino)) {
            if (p.getEquipoB().size() >= p.getJugadoresPorEquipo()) {
                throw new RuntimeException("Equipo B está completo");
            }
            p.getEquipoB().add(jugador);
        } else {
            throw new RuntimeException("Equipo inválido. Debe ser 'A' o 'B'");
        }

        p.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(p);
    }

    @Transactional
    public PartidoEntity moverJugadorASinEquipo(Long partidoId, Long jugadorId, UsuarioEntity creador) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();
        
        validarPermisoOrganizador(p, creador, "Solo un organizador puede mover jugadores");
        
        // Buscar el jugador
        Optional<UsuarioEntity> jugadorOpt = usuarioRepository.findById(jugadorId);
        if (jugadorOpt.isEmpty()) {
            throw new ResourceNotFoundException("Usuario", jugadorId);
        }
        UsuarioEntity jugador = jugadorOpt.get();
        
        // Verificar que el jugador esté en algún equipo
        boolean estaEnEquipoA = p.getEquipoA().contains(jugador);
        boolean estaEnEquipoB = p.getEquipoB().contains(jugador);
        
        if (!estaEnEquipoA && !estaEnEquipoB) {
            throw new RuntimeException("El jugador no está en ningún equipo");
        }
        
        // Remover del equipo donde esté
        p.getEquipoA().remove(jugador);
        p.getEquipoB().remove(jugador);
        
        // Agregar a jugadores inscritos (sin equipo)
        if (!p.getJugadoresInscritos().contains(jugador)) {
            p.getJugadoresInscritos().add(jugador);
        }
        
        p.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(p);
    }

    @Transactional
    public void eliminarPartido(Long partidoId, UsuarioEntity creador) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();

        validarPermisoOwner(p, creador, "Solo un OWNER puede eliminar el partido");

        if (p.getEstado() == EstadoPartido.FINALIZADO) {
            p.setArchivado(true);
            p.setActualizadoEn(LocalDateTime.now());
            partidoRepository.save(p);
            return;
        }

        cancelarPartido(p, "Cancelado por organizador");
        notificarCancelacion(p, "Cancelado por organizador", creador);
        partidoRepository.save(p);
    }

    @Transactional
    public PartidoEntity transferirCreador(Long partidoId, UsuarioEntity creadorActual, Long nuevoCreadorId) {
        Optional<PartidoEntity> partido = partidoRepository.findById(partidoId);
        if (partido.isEmpty()) {
            throw new ResourceNotFoundException("Partido", partidoId);
        }

        PartidoEntity p = partido.get();
        validarPermisoOwner(p, creadorActual, "Solo un OWNER puede transferir la propiedad");

        // Verificar que el nuevo creador exista
        Optional<UsuarioEntity> nuevoCreador = usuarioRepository.findById(nuevoCreadorId);
        if (nuevoCreador.isEmpty()) {
            throw new RuntimeException("El nuevo creador no existe");
        }

        if (!esOrganizador(p, nuevoCreador.get())) {
            throw new RuntimeException("El nuevo OWNER debe ser organizador del partido");
        }

        partidoOrganizadorRepository.findByPartidoAndUsuario(p, creadorActual)
            .orElseThrow(() -> new RuntimeException("Organizador actual no encontrado"));
        partidoOrganizadorRepository.findByPartidoAndUsuario(p, nuevoCreador.get())
            .orElseThrow(() -> new RuntimeException("Nuevo organizador no encontrado"));

        establecerOwnerUnico(p, nuevoCreador.get().getId());
        p.setActualizadoEn(LocalDateTime.now());

        return partidoRepository.save(p);
    }

    @Transactional
    public PartidoEntity reservarPista(Long partidoId, UsuarioEntity solicitante, BigDecimal precioPista) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        validarPermisoOrganizador(partido, solicitante, "Solo una persona organizadora puede marcar la pista como reservada");
        validarPartidoEditable(partido);

        if (partido.getEstado() == EstadoPartido.FINALIZADO) {
            throw new RuntimeException("No se puede reservar pista en un partido finalizado");
        }

        if (partido.getEstado() == EstadoPartido.CONFIRMADO) {
            throw new RuntimeException("La pista ya figura como reservada");
        }

        if (precioPista == null || precioPista.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Debes indicar un precio válido para la pista");
        }

        partido.setEstado(EstadoPartido.CONFIRMADO);
        partido.setActualizadoEn(LocalDateTime.now());
        PartidoEntity actualizado = partidoRepository.save(partido);

        notificarReservaPista(actualizado, solicitante, precioPista);
        return actualizado;
    }

    @Transactional
    public EquiposBalanceadosResponseDTO balancearEquipos(Long partidoId, List<Long> playerIds, UsuarioEntity solicitante) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        if (!esOrganizador(partido, solicitante)) {
            throw new RuntimeException("Solo un organizador puede balancear equipos");
        }

        if (playerIds == null || playerIds.isEmpty()) {
            throw new IllegalArgumentException("Debes enviar al menos un jugador");
        }

        List<Long> idsUnicos = playerIds.stream().distinct().toList();
        List<UsuarioEntity> jugadores = usuarioRepository.findAllById(idsUnicos);

        if (jugadores.size() != idsUnicos.size()) {
            throw new IllegalArgumentException("Uno o más jugadores no existen");
        }

        int maxJugadores = partido.getJugadoresPorEquipo() * 2;
        if (jugadores.size() > maxJugadores) {
            throw new IllegalArgumentException("La cantidad de jugadores excede el cupo del partido");
        }

        if (jugadores.size() < 2) {
            throw new IllegalArgumentException("Se necesitan al menos dos jugadores para balancear");
        }

        Map<Long, String> asignacionesAntes = buildAssignmentMap(partido.getEquipoA(), partido.getEquipoB());
        BalanceSnapshot snapshotAntes = buildSnapshotFromUsers(partido.getEquipoA(), partido.getEquipoB(), partido.getJugadoresPorEquipo());
        List<String> razonesAntes = buildRazonesDesbalance(snapshotAntes);

        List<BalanceCandidate> candidatos = jugadores.stream()
            .map(this::toCandidate)
            .collect(Collectors.toCollection(ArrayList::new));

        PartitionResult bestPartition = calcularMejorParticion(candidatos, partido.getJugadoresPorEquipo());

        TeamState equipoA = new TeamState(partido.getJugadoresPorEquipo());
        TeamState equipoB = new TeamState(partido.getJugadoresPorEquipo());

        bestPartition.equipoA().forEach(equipoA::add);
        bestPartition.equipoB().forEach(equipoB::add);

        partido.setEquipoA(equipoA.members().stream().map(BalanceCandidate::user).collect(Collectors.toCollection(ArrayList::new)));
        partido.setEquipoB(equipoB.members().stream().map(BalanceCandidate::user).collect(Collectors.toCollection(ArrayList::new)));
        if (partido.getJugadoresInscritos() != null) {
            partido.getJugadoresInscritos().removeIf(j -> idsUnicos.contains(j.getId()));
        }
        partido.setActualizadoEn(LocalDateTime.now());
        partidoRepository.save(partido);

        Map<Long, String> asignacionesDespues = buildAssignmentMap(partido.getEquipoA(), partido.getEquipoB());
        List<String> cambiosAplicados = buildCambiosAplicados(jugadores, asignacionesAntes, asignacionesDespues);
        BalanceSnapshot snapshotDespues = new BalanceSnapshot(
            equipoA.size(),
            equipoB.size(),
            equipoA.totalLevel(),
            equipoB.totalLevel(),
            new HashSet<>(equipoA.positions()),
            new HashSet<>(equipoB.positions()),
            partido.getJugadoresPorEquipo()
        );

        return new EquiposBalanceadosResponseDTO(
                partido.getId(),
                equipoA.totalLevel(),
                equipoB.totalLevel(),
            snapshotAntes.diferenciaNivelAbsoluta(),
            snapshotDespues.diferenciaNivelAbsoluta(),
            snapshotAntes.balanceado(),
            snapshotDespues.balanceado(),
            razonesAntes,
            cambiosAplicados,
                equipoA.members().stream().map(this::toJugadorBalanceadoDTO).toList(),
                equipoB.members().stream().map(this::toJugadorBalanceadoDTO).toList()
        );
    }

    @Transactional(readOnly = true)
    public com.worldcup.Back.dto.response.BalanceResultDTO asignarPosicionesSinRedistribuir(Long partidoId, String formacion) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        List<UsuarioEntity> equipoA = partido.getEquipoA() != null ? partido.getEquipoA() : new ArrayList<>();
        List<UsuarioEntity> equipoB = partido.getEquipoB() != null ? partido.getEquipoB() : new ArrayList<>();

        int totalPlayers = equipoA.size() + equipoB.size();
        if (totalPlayers < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 jugadores para asignar posiciones. Jugadores actuales: " + totalPlayers);
        }

        return teamBalancingService.asignarPosicionesSinRedistribuir(equipoA, equipoB, formacion);
    }

        @Transactional(readOnly = true)
        public PartidoBalanceAnalisisResponseDTO analizarBalancePartido(Long partidoId) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
            .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        BalanceSnapshot snapshot = buildSnapshotFromUsers(partido.getEquipoA(), partido.getEquipoB(), partido.getJugadoresPorEquipo());

        return new PartidoBalanceAnalisisResponseDTO(
            partido.getId(),
            snapshot.balanceado(),
            snapshot.diferenciaNivelAbsoluta(),
            snapshot.jugadoresA(),
            snapshot.jugadoresB(),
            buildRazonesDesbalance(snapshot)
        );
        }

    private PartitionResult calcularMejorParticion(List<BalanceCandidate> candidatos, int capacidadEquipo) {
        int total = candidatos.size();
        int floor = total / 2;
        int ceil = (total + 1) / 2;

        List<Integer> objetivosA = new ArrayList<>();
        if (floor <= capacidadEquipo && total - floor <= capacidadEquipo) {
            objetivosA.add(floor);
        }
        if (ceil <= capacidadEquipo && total - ceil <= capacidadEquipo && ceil != floor) {
            objetivosA.add(ceil);
        }

        if (objetivosA.isEmpty()) {
            throw new IllegalArgumentException("No es posible repartir jugadores respetando capacidad de equipos");
        }

        PartitionResult mejorGlobal = null;
        for (Integer objetivoA : objetivosA) {
            PartitionResult candidato = buscarMejorConObjetivo(candidatos, objetivoA);
            if (mejorGlobal == null || candidato.compareTo(mejorGlobal) < 0) {
                mejorGlobal = candidato;
            }
        }

        if (mejorGlobal == null) {
            throw new IllegalStateException("No se pudo calcular una partición válida");
        }

        return mejorGlobal;
    }

    private PartitionResult buscarMejorConObjetivo(List<BalanceCandidate> candidatos, int objetivoA) {
        boolean[] elegido = new boolean[candidatos.size()];
        SearchState state = new SearchState();
        backtrackParticiones(candidatos, 0, objetivoA, elegido, state);
        return state.best;
    }

    private void backtrackParticiones(List<BalanceCandidate> candidatos,
                                      int index,
                                      int objetivoA,
                                      boolean[] elegido,
                                      SearchState state) {
        int elegidosCount = 0;
        for (boolean value : elegido) {
            if (value) elegidosCount++;
        }

        if (elegidosCount > objetivoA) {
            return;
        }

        int restantes = candidatos.size() - index;
        if (elegidosCount + restantes < objetivoA) {
            return;
        }

        if (index == candidatos.size()) {
            if (elegidosCount != objetivoA) {
                return;
            }

            List<BalanceCandidate> equipoA = new ArrayList<>();
            List<BalanceCandidate> equipoB = new ArrayList<>();

            for (int i = 0; i < candidatos.size(); i++) {
                if (elegido[i]) {
                    equipoA.add(candidatos.get(i));
                } else {
                    equipoB.add(candidatos.get(i));
                }
            }

            PartitionResult candidato = evaluarParticion(equipoA, equipoB);
            if (state.best == null || candidato.compareTo(state.best) < 0) {
                state.best = candidato;
            }
            return;
        }

        elegido[index] = true;
        backtrackParticiones(candidatos, index + 1, objetivoA, elegido, state);

        elegido[index] = false;
        backtrackParticiones(candidatos, index + 1, objetivoA, elegido, state);
    }

    private PartitionResult evaluarParticion(List<BalanceCandidate> equipoA, List<BalanceCandidate> equipoB) {
        int nivelA = equipoA.stream().mapToInt(BalanceCandidate::level).sum();
        int nivelB = equipoB.stream().mapToInt(BalanceCandidate::level).sum();
        int diferenciaNivel = Math.abs(nivelA - nivelB);

        Set<String> posicionesA = equipoA.stream()
                .flatMap(j -> j.positions().stream())
                .collect(Collectors.toSet());
        Set<String> posicionesB = equipoB.stream()
                .flatMap(j -> j.positions().stream())
                .collect(Collectors.toSet());

        int faltantesA = (int) POSICIONES_CLAVE.stream().filter(pos -> !posicionesA.contains(pos)).count();
        int faltantesB = (int) POSICIONES_CLAVE.stream().filter(pos -> !posicionesB.contains(pos)).count();
        int penalidadPosiciones = faltantesA + faltantesB;

        return new PartitionResult(equipoA, equipoB, diferenciaNivel, penalidadPosiciones, nivelA, nivelB);
    }

    private BalanceCandidate toCandidate(UsuarioEntity usuario) {
        // Obtener posiciones del usuario base
        List<String> posiciones = usuario.getPosiciones() == null 
                ? List.of() 
                : usuario.getPosiciones()
                    .stream()
                    .map(this::normalizePosition)
                    .filter(POSICIONES_CLAVE::contains)
                    .distinct()
                    .toList();

        String primary = posiciones.isEmpty() ? "MEDIOCAMPISTA" : posiciones.get(0);
        int level = playerProfileService.getPlayerLevel(usuario);

        return new BalanceCandidate(usuario, level, primary, new HashSet<>(posiciones));
    }

    private String normalizePosition(String raw) {
        if (raw == null || raw.isBlank()) {
            return "MEDIOCAMPISTA";
        }
        return raw.trim().toUpperCase();
    }

    private JugadorBalanceadoDTO toJugadorBalanceadoDTO(BalanceCandidate candidate) {
        return new JugadorBalanceadoDTO(
                candidate.user().getId(),
                candidate.user().getNombre(),
                candidate.primaryPosition(),
                candidate.level()
        );
    }

    private BalanceSnapshot buildSnapshotFromUsers(List<UsuarioEntity> equipoAUsuarios, List<UsuarioEntity> equipoBUsuarios, int jugadoresPorEquipo) {
        TeamState equipoA = new TeamState(jugadoresPorEquipo);
        TeamState equipoB = new TeamState(jugadoresPorEquipo);

        equipoAUsuarios.stream().map(this::toCandidate).forEach(equipoA::add);
        equipoBUsuarios.stream().map(this::toCandidate).forEach(equipoB::add);

        return new BalanceSnapshot(
                equipoA.size(),
                equipoB.size(),
                equipoA.totalLevel(),
                equipoB.totalLevel(),
                new HashSet<>(equipoA.positions()),
                new HashSet<>(equipoB.positions()),
                jugadoresPorEquipo
        );
    }

    private List<String> buildRazonesDesbalance(BalanceSnapshot snapshot) {
        List<String> razones = new ArrayList<>();

        if (snapshot.jugadoresA() != snapshot.jugadoresB()) {
            razones.add("Cantidad desigual de jugadores: Equipo A " + snapshot.jugadoresA() + " vs Equipo B " + snapshot.jugadoresB());
        }

        if (snapshot.diferenciaNivelAbsoluta() >= 2) {
            razones.add("Diferencia de nivel acumulado: " + snapshot.nivelA() + " vs " + snapshot.nivelB());
        }

        for (String posicion : POSICIONES_CLAVE) {
            if (!snapshot.posicionesA().contains(posicion)) {
                razones.add("Equipo A sin cobertura de posición clave: " + posicion);
            }
            if (!snapshot.posicionesB().contains(posicion)) {
                razones.add("Equipo B sin cobertura de posición clave: " + posicion);
            }
        }

        if (snapshot.jugadoresA() < snapshot.capacidadEquipo() || snapshot.jugadoresB() < snapshot.capacidadEquipo()) {
            razones.add("Aún faltan jugadores por completar cupos del partido");
        }

        if (razones.isEmpty()) {
            razones.add("No se detectaron causas de desbalance relevantes");
        }

        return razones;
    }

    private Map<Long, String> buildAssignmentMap(List<UsuarioEntity> equipoA, List<UsuarioEntity> equipoB) {
        Map<Long, String> map = new HashMap<>();
        for (UsuarioEntity usuario : equipoA) {
            map.put(usuario.getId(), "A");
        }
        for (UsuarioEntity usuario : equipoB) {
            map.put(usuario.getId(), "B");
        }
        return map;
    }

    private List<String> buildCambiosAplicados(List<UsuarioEntity> jugadores, Map<Long, String> antes, Map<Long, String> despues) {
        List<String> cambios = new ArrayList<>();

        for (UsuarioEntity jugador : jugadores) {
            String equipoAntes = antes.getOrDefault(jugador.getId(), "SIN_EQUIPO");
            String equipoDespues = despues.getOrDefault(jugador.getId(), "SIN_EQUIPO");

            if (!equipoAntes.equals(equipoDespues)) {
                cambios.add(jugador.getNombre() + ": " + traducirEquipo(equipoAntes) + " → " + traducirEquipo(equipoDespues));
            }
        }

        if (cambios.isEmpty()) {
            cambios.add("No fue necesario mover jugadores; los equipos ya estaban óptimos.");
        }

        return cambios;
    }

    private String traducirEquipo(String codigo) {
        return switch (codigo) {
            case "A" -> "Equipo A";
            case "B" -> "Equipo B";
            default -> "Sin equipo";
        };
    }

    private record BalanceCandidate(UsuarioEntity user, int level, String primaryPosition, Set<String> positions) {
    }

    private static class SearchState {
        private PartitionResult best;
    }

    private record PartitionResult(
            List<BalanceCandidate> equipoA,
            List<BalanceCandidate> equipoB,
            int diferenciaNivel,
            int penalidadPosiciones,
            int nivelA,
            int nivelB
    ) {
        private int compareTo(PartitionResult other) {
            if (diferenciaNivel != other.diferenciaNivel) {
                return Integer.compare(diferenciaNivel, other.diferenciaNivel);
            }

            if (penalidadPosiciones != other.penalidadPosiciones) {
                return Integer.compare(penalidadPosiciones, other.penalidadPosiciones);
            }

            return Integer.compare(Math.abs(equipoA.size() - equipoB.size()), Math.abs(other.equipoA.size() - other.equipoB.size()));
        }
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

    private PartidoHistorialDTO toHistorialDTO(PartidoEntity partido, UsuarioEntity usuario) {
        DatosHistorialVotacion datosVotacion = resolverDatosHistorialVotacion(partido);
        Integer golesEquipoA = datosVotacion.golesEquipoA();
        Integer golesEquipoB = datosVotacion.golesEquipoB();
        String ganador = datosVotacion.ganador();
        int vecesDiferencial = (int) partidoVotacionRepository.findByPartido(partido)
            .stream()
            .filter(voto -> voto.getJugadoresDiferenciales() != null
                && voto.getJugadoresDiferenciales().stream().anyMatch(id -> id.equals(usuario.getId())))
            .count();

        boolean usuarioEnA = partido.getEquipoA().stream().anyMatch(u -> u.getId().equals(usuario.getId()));
        boolean usuarioEnB = partido.getEquipoB().stream().anyMatch(u -> u.getId().equals(usuario.getId()));
        String resultado = "SIN_PARTICIPACION";

        if (usuarioEnA || usuarioEnB) {
            if (golesEquipoA != null && golesEquipoB != null
                    && golesEquipoA.equals(golesEquipoB)) {
                resultado = "EMPATE";
            } else if ("EQUIPO_A".equalsIgnoreCase(ganador)) {
                resultado = usuarioEnA ? "VICTORIA" : "DERROTA";
            } else if ("EQUIPO_B".equalsIgnoreCase(ganador)) {
                resultado = usuarioEnB ? "VICTORIA" : "DERROTA";
            }
        } else if (esOrganizador(partido, usuario)) {
            resultado = "ORGANIZADO";
        }

        return new PartidoHistorialDTO(
                partido.getId(),
                partido.getFecha(),
                partido.getLugar(),
                golesEquipoA,
                golesEquipoB,
                ganador,
                resultado,
                usuarioEnA,
                partido.getTipo().toString(),
                datosVotacion.intensidadPartido(),
                datosVotacion.porcentajeBalanceo(),
                vecesDiferencial
        );
    }

    private DatosHistorialVotacion resolverDatosHistorialVotacion(PartidoEntity partido) {
        Integer golesA = partido.getGolesEquipoA();
        Integer golesB = partido.getGolesEquipoB();
        String ganador = partido.getGanador();

        List<PartidoVotacionEntity> votos = partidoVotacionRepository.findByPartido(partido);
        if (votos.isEmpty()) {
            return new DatosHistorialVotacion(golesA, golesB, ganador, null, null);
        }

        if (golesA == null && golesB == null && ganador == null) {
            golesA = (int) Math.round(votos.stream().mapToInt(PartidoVotacionEntity::getGolesEquipoAPropuesto).average().orElse(0));
            golesB = (int) Math.round(votos.stream().mapToInt(PartidoVotacionEntity::getGolesEquipoBPropuesto).average().orElse(0));

            if (golesA > golesB) {
                ganador = "EQUIPO_A";
            } else if (golesB > golesA) {
                ganador = "EQUIPO_B";
            } else {
                ganador = "EMPATE";
            }
        }

        long votosParejo = votos.stream().filter(v -> Boolean.TRUE.equals(v.getPartidoFueParejo())).count();
        double porcentajeParejo = (votosParejo * 100.0) / votos.size();

        Map<String, Long> intensidadConteo = votos.stream()
                .map(PartidoVotacionEntity::getIntensidadPartido)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(i -> i, Collectors.counting()));

        String intensidadMasVotada = intensidadConteo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String intensidadLabel = intensidadMasVotada == null
                ? null
                : intensidadMasVotada.replace('_', ' ');

        return new DatosHistorialVotacion(
                golesA,
                golesB,
                ganador,
                intensidadLabel,
                porcentajeParejo
        );
    }

    private record DatosHistorialVotacion(
            Integer golesEquipoA,
            Integer golesEquipoB,
            String ganador,
            String intensidadPartido,
            Double porcentajeBalanceo
    ) {
    }

    private record BalanceSnapshot(
            int jugadoresA,
            int jugadoresB,
            int nivelA,
            int nivelB,
            Set<String> posicionesA,
            Set<String> posicionesB,
            int capacidadEquipo
    ) {
        private int diferenciaNivelAbsoluta() {
            return Math.abs(nivelA - nivelB);
        }

        private boolean balanceado() {
            return jugadoresA == jugadoresB
                    && jugadoresA == capacidadEquipo
                    && diferenciaNivelAbsoluta() <= 1
                    && posicionesA.containsAll(POSICIONES_CLAVE)
                    && posicionesB.containsAll(POSICIONES_CLAVE);
        }
    }

    @Transactional
    public List<PartidoOrganizadorEntity> obtenerOrganizadores(Long partidoId) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));
        return partidoOrganizadorRepository.findByPartidoOrderByAsignadoEnAsc(partido);
    }

    @Transactional
    public PartidoEntity asignarCoOrganizador(Long partidoId, UsuarioEntity ownerSolicitante, Long usuarioId) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        validarPermisoOwner(partido, ownerSolicitante, "Solo un OWNER puede asignar organizadores");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        if (partidoOrganizadorRepository.existsByPartidoAndUsuario(partido, usuario)) {
            throw new RuntimeException("El usuario ya es organizador del partido");
        }

        PartidoOrganizadorEntity rel = new PartidoOrganizadorEntity();
        rel.setPartido(partido);
        rel.setUsuario(usuario);
        rel.setRol(PartidoOrganizadorRol.CO_ORGANIZER);
        partidoOrganizadorRepository.save(rel);

        partido.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(partido);
    }

    @Transactional
    public PartidoEntity revocarOrganizador(Long partidoId, UsuarioEntity ownerSolicitante, Long usuarioId) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        validarPermisoOwner(partido, ownerSolicitante, "Solo un OWNER puede revocar organizadores");

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        PartidoOrganizadorEntity rel = partidoOrganizadorRepository.findByPartidoAndUsuario(partido, usuario)
                .orElseThrow(() -> new RuntimeException("El usuario no es organizador del partido"));

        if (rel.getRol() == PartidoOrganizadorRol.OWNER) {
            long owners = partidoOrganizadorRepository.findByPartidoAndRolOrderByAsignadoEnAsc(partido, PartidoOrganizadorRol.OWNER).size();
            if (owners <= 1) {
                throw new RuntimeException("No se puede revocar al último OWNER");
            }
        }

        partido.getOrganizadores().removeIf(o -> o.getId().equals(rel.getId()));
        partido.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(partido);
    }

    @Transactional
    public PartidoEntity salirComoOrganizador(Long partidoId, UsuarioEntity usuario, boolean confirmarEliminacion, Long nuevoOwnerId) {
        PartidoEntity partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido", partidoId));

        PartidoOrganizadorEntity rel = partidoOrganizadorRepository.findByPartidoAndUsuario(partido, usuario)
                .orElseThrow(() -> new RuntimeException("No eres organizador de este partido"));

        List<PartidoOrganizadorEntity> owners = partidoOrganizadorRepository.findByPartidoAndRolOrderByAsignadoEnAsc(
                partido,
                PartidoOrganizadorRol.OWNER
        );

        List<PartidoOrganizadorEntity> otrosOrganizadores = partidoOrganizadorRepository.findByPartidoOrderByAsignadoEnAsc(partido)
                .stream()
                .filter(o -> !o.getUsuario().getId().equals(usuario.getId()))
                .toList();

        if (rel.getRol() == PartidoOrganizadorRol.OWNER && owners.size() == 1) {
            if (otrosOrganizadores.isEmpty()) {
                if (!confirmarEliminacion) {
                    throw new RuntimeException("Eres el único OWNER. Debes confirmar eliminación del partido para salir.");
                }
                cancelarPartido(partido, "Cancelado por organizador");
                notificarCancelacion(partido, "Cancelado por organizador", usuario);
                return partidoRepository.save(partido);
            }

            // Si hay otros organizadores, transferir propiedad y permitir salida sin eliminar partido
            if (nuevoOwnerId != null) {
                UsuarioEntity nuevoOwner = usuarioRepository.findById(nuevoOwnerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario", nuevoOwnerId));
            partidoOrganizadorRepository.findByPartidoAndUsuario(partido, nuevoOwner)
                .orElseThrow(() -> new RuntimeException("El nuevo OWNER debe ser organizador"));
            establecerOwnerUnico(partido, nuevoOwnerId);
            } else {
                PartidoOrganizadorEntity masAntiguo = otrosOrganizadores.get(0);
            establecerOwnerUnico(partido, masAntiguo.getUsuario().getId());
            }
        } else if (rel.getRol() == PartidoOrganizadorRol.OWNER) {
            if (nuevoOwnerId != null) {
                UsuarioEntity nuevoOwner = usuarioRepository.findById(nuevoOwnerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario", nuevoOwnerId));
            partidoOrganizadorRepository.findByPartidoAndUsuario(partido, nuevoOwner)
                .orElseThrow(() -> new RuntimeException("El nuevo OWNER debe ser organizador"));
            establecerOwnerUnico(partido, nuevoOwnerId);
            } else {
                PartidoOrganizadorEntity masAntiguo = partidoOrganizadorRepository.findByPartidoOrderByAsignadoEnAsc(partido)
                        .stream()
                        .filter(o -> !o.getUsuario().getId().equals(usuario.getId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No existe otro organizador para promover"));
            establecerOwnerUnico(partido, masAntiguo.getUsuario().getId());
            }
        }

        partido.getOrganizadores().removeIf(o -> o.getId().equals(rel.getId()));

        List<PartidoOrganizadorEntity> ownersRestantes = partidoOrganizadorRepository.findByPartidoAndRolOrderByAsignadoEnAsc(
                partido,
                PartidoOrganizadorRol.OWNER
        );
        if (ownersRestantes.isEmpty()) {
            throw new RuntimeException("No puede quedar un partido sin OWNER");
        }

        partido.setActualizadoEn(LocalDateTime.now());
        return partidoRepository.save(partido);
    }

    public UsuarioEntity obtenerOwnerDelPartido(PartidoEntity partido) {
        return obtenerOwnerPrincipal(partido);
    }

    private static class TeamState {
        private final int capacity;
        private final List<BalanceCandidate> members = new ArrayList<>();
        private int totalLevel = 0;
        private final Set<String> positions = new HashSet<>();

        private TeamState(int capacity) {
            this.capacity = capacity;
        }

        private boolean isFull() {
            return members.size() >= capacity;
        }

        private void add(BalanceCandidate candidate) {
            if (isFull()) {
                throw new IllegalStateException("Equipo sin cupo para más jugadores");
            }

            members.add(candidate);
            totalLevel += candidate.level();
            positions.addAll(candidate.positions());
        }

        private boolean hasPosition(String position) {
            return positions.contains(position);
        }

        private int size() {
            return members.size();
        }

        private int totalLevel() {
            return totalLevel;
        }

        private List<BalanceCandidate> members() {
            return members;
        }

        private Set<String> positions() {
            return positions;
        }
    }
}
