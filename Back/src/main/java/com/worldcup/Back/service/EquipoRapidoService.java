package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.EquipoRapidoRequestDTO;
import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
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

    @Transactional(readOnly = true)
    public List<EquipoRapidoEntity> listarMisEquipos(UsuarioEntity owner) {
        return equipoRapidoRepository.findByOwnerOrderByActualizadoEnDesc(owner);
    }

    @Transactional(readOnly = true)
    public EquipoRapidoEntity obtenerEquipoRapidoPropio(UsuarioEntity owner, Long equipoId) {
        return equipoRapidoRepository.findByIdAndOwner(equipoId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));
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
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdAndOwner(equipoId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoId));

        equipoRapidoRepository.delete(equipo);
    }
}
