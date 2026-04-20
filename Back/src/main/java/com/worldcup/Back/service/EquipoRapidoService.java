package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.EquipoRapidoRequestDTO;
import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.TipoNotificacionEquipo;
import com.worldcup.Back.exception.BusinessException;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.EquipoRapidoRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EquipoRapidoService {

    @Autowired
    private EquipoRapidoRepository equipoRapidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AmistadService amistadService;

    @Autowired
    private EquipoRapidoNotificacionService equipoRapidoNotificacionService;

    @Transactional(readOnly = true)
    public List<EquipoRapidoEntity> listarMisEquipos(UsuarioEntity owner) {
        return equipoRapidoRepository.findVisibleByUsuarioOrderByActualizadoEnDesc(owner);
    }

    @Transactional(readOnly = true)
    public EquipoRapidoEntity obtenerEquipoRapidoPropio(UsuarioEntity owner, Long equipoId) {
        return equipoRapidoRepository.findByIdAndOwner(equipoId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));
    }

    @Transactional(readOnly = true)
    public EquipoRapidoEntity obtenerEquipoVisible(UsuarioEntity usuario, Long equipoId) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdWithMiembros(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));

        boolean esOwner = equipo.getOwner() != null && equipo.getOwner().getId() != null && equipo.getOwner().getId().equals(usuario.getId());
        boolean esMiembro = equipo.getMiembros().stream().anyMatch(m -> m.getId().equals(usuario.getId()));
        if (!esOwner && !esMiembro) {
            throw new ResourceNotFoundException("Equipo rapido", equipoId);
        }

        return equipo;
    }

    private void validarOrganizador(EquipoRapidoEntity equipo, UsuarioEntity usuario) {
        if (equipo.getOwner() == null || equipo.getOwner().getId() == null || !equipo.getOwner().getId().equals(usuario.getId())) {
            throw new BusinessException("Solo el organizador del equipo puede realizar esta acción");
        }
    }

    private boolean esMiembro(EquipoRapidoEntity equipo, Long usuarioId) {
        return equipo.getMiembros().stream().anyMatch(miembro -> miembro.getId().equals(usuarioId));
    }

    private void agregarMiembroSiNoExiste(EquipoRapidoEntity equipo, UsuarioEntity miembro) {
        if (!esMiembro(equipo, miembro.getId())) {
            equipo.getMiembros().add(miembro);
        }
    }

    @Transactional
    public EquipoRapidoEntity actualizarEquipoRapido(UsuarioEntity owner, Long equipoId, EquipoRapidoRequestDTO request) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdWithMiembros(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));
        validarOrganizador(equipo, owner);

        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        if (nombre.isBlank()) {
            throw new BusinessException("Debes indicar un nombre para el equipo");
        }

        int capacidad = request.getCapacidad() == null ? 7 : request.getCapacidad();
        if (capacidad < 2 || capacidad > 7) {
            throw new BusinessException("La capacidad del equipo debe estar entre 2 y 7 integrantes");
        }

        int totalIntegrantes = 1 + equipo.getMiembros().size();
        if (capacidad < totalIntegrantes) {
            throw new BusinessException("La nueva capacidad no puede ser menor que los integrantes actuales");
        }

        equipo.setNombre(nombre);
        equipo.setCapacidad(capacidad);
        return equipoRapidoRepository.save(equipo);
    }

    @Transactional
    public EquipoRapidoEntity agregarMiembro(UsuarioEntity owner, Long equipoId, Long miembroId) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdWithMiembros(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));
        validarOrganizador(equipo, owner);

        UsuarioEntity miembro = usuarioRepository.findById(miembroId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", miembroId));

        if (miembro.getId().equals(owner.getId())) {
            throw new BusinessException("No puedes agregarte a ti mismo");
        }

        if (!amistadService.sonAmigos(owner, miembro)) {
            throw new BusinessException("Solo puedes agregar amistades confirmadas al equipo");
        }

        if (esMiembro(equipo, miembro.getId())) {
            throw new BusinessException("Ese usuario ya forma parte del equipo");
        }

        int capacidad = equipo.getCapacidad() == null ? 7 : equipo.getCapacidad();
        int totalIntegrantes = 1 + equipo.getMiembros().size();
        if (totalIntegrantes >= capacidad) {
            throw new BusinessException("El equipo ya esta completo");
        }

        agregarMiembroSiNoExiste(equipo, miembro);
        EquipoRapidoEntity guardado = equipoRapidoRepository.save(equipo);

        equipoRapidoNotificacionService.crearNotificacion(
                guardado,
                miembro,
                owner,
                TipoNotificacionEquipo.AGREGADO,
                owner.getNombre() + " te ha agregado al equipo \"" + guardado.getNombre() + "\"."
        );
        return guardado;
    }

    @Transactional
    public EquipoRapidoEntity quitarMiembro(UsuarioEntity owner, Long equipoId, Long miembroId) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdWithMiembros(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));
        validarOrganizador(equipo, owner);

        UsuarioEntity miembro = usuarioRepository.findById(miembroId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", miembroId));

        if (miembro.getId().equals(owner.getId())) {
            throw new BusinessException("El organizador no puede expulsarse a si mismo");
        }

        boolean eliminado = equipo.getMiembros().removeIf(actual -> actual.getId().equals(miembro.getId()));
        if (!eliminado) {
            throw new BusinessException("Ese usuario no forma parte del equipo");
        }

        EquipoRapidoEntity guardado = equipoRapidoRepository.save(equipo);
        equipoRapidoNotificacionService.crearNotificacion(
                guardado,
                miembro,
                owner,
                TipoNotificacionEquipo.EXPULSADO,
                "Has sido expulsado del equipo \"" + guardado.getNombre() + "\" por " + owner.getNombre() + "."
        );
        return guardado;
    }

    @Transactional
    public EquipoRapidoEntity salirDeEquipo(UsuarioEntity usuario, Long equipoId) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdWithMiembros(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));

        if (equipo.getOwner() != null && equipo.getOwner().getId() != null && equipo.getOwner().getId().equals(usuario.getId())) {
            throw new BusinessException("El organizador no puede salir como miembro del equipo");
        }

        boolean eliminado = equipo.getMiembros().removeIf(actual -> actual.getId().equals(usuario.getId()));
        if (!eliminado) {
            throw new BusinessException("No formas parte de este equipo");
        }

        EquipoRapidoEntity guardado = equipoRapidoRepository.save(equipo);
        if (guardado.getOwner() != null) {
            equipoRapidoNotificacionService.crearNotificacion(
                    guardado,
                    guardado.getOwner(),
                    usuario,
                    TipoNotificacionEquipo.SALIDA,
                    usuario.getNombre() + " ha salido del equipo \"" + guardado.getNombre() + "\"."
            );
        }
        return guardado;
    }

    @Transactional
    public EquipoRapidoEntity crearEquipoRapido(UsuarioEntity owner, EquipoRapidoRequestDTO request) {
        if (request == null) {
            throw new BusinessException("Datos de equipo invalidos");
        }

        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        if (nombre.isBlank()) {
            throw new BusinessException("Debes indicar un nombre para el equipo");
        }

        int capacidad = request.getCapacidad() == null ? 7 : request.getCapacidad();
        if (capacidad < 2 || capacidad > 7) {
            throw new BusinessException("La capacidad del equipo debe estar entre 2 y 7 integrantes");
        }

        List<Long> requestedIds = request.getMiembroIds() == null ? List.of() : request.getMiembroIds();
        Set<Long> idsUnicos = requestedIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(owner.getId()))
                .collect(Collectors.toSet());

        int maxMiembrosPermitidos = capacidad - 1;
        if (idsUnicos.size() > maxMiembrosPermitidos) {
            throw new BusinessException("La capacidad elegida permite como maximo " + maxMiembrosPermitidos + " amistades");
        }

        List<UsuarioEntity> miembros = idsUnicos.isEmpty()
                ? new ArrayList<>()
                : usuarioRepository.findAllById(idsUnicos);

        if (miembros.size() != idsUnicos.size()) {
            throw new BusinessException("Uno o mas usuarios seleccionados no existen");
        }

        for (UsuarioEntity miembro : miembros) {
            if (!amistadService.sonAmigos(owner, miembro)) {
                throw new BusinessException("Solo puedes agregar amistades confirmadas a un equipo rapido");
            }
        }

        EquipoRapidoEntity entity = new EquipoRapidoEntity();
        entity.setOwner(owner);
        entity.setNombre(nombre);
        entity.setCapacidad(capacidad);
        entity.setMiembros(miembros);

        return equipoRapidoRepository.save(entity);
    }

    @Transactional
    public void eliminarEquipoRapido(UsuarioEntity owner, Long equipoId) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdWithMiembros(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));
        validarOrganizador(equipo, owner);

        equipo.getMiembros().forEach(miembro -> equipoRapidoNotificacionService.crearNotificacion(
            equipo,
            miembro,
            owner,
            TipoNotificacionEquipo.ELIMINADO,
            "El equipo \"" + equipo.getNombre() + "\" ha sido eliminado por " + owner.getNombre() + "."
        ));

        equipoRapidoRepository.delete(equipo);
    }
}
