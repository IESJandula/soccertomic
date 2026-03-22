package com.worldcup.Back.service;

import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.EquipoRapidoInvitacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoInvitacion;
import com.worldcup.Back.exception.BusinessException;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.EquipoRapidoInvitacionRepository;
import com.worldcup.Back.repository.EquipoRapidoRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipoRapidoInvitacionService {

    @Autowired
    private EquipoRapidoInvitacionRepository equipoRapidoInvitacionRepository;

    @Autowired
    private EquipoRapidoRepository equipoRapidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AmistadService amistadService;

    @Transactional
    public EquipoRapidoInvitacionEntity crearInvitacion(Long equipoRapidoId, Long destinatarioId, UsuarioEntity emisor) {
        EquipoRapidoEntity equipo = equipoRapidoRepository.findByIdAndOwner(equipoRapidoId, emisor)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo rapido", equipoRapidoId));

        UsuarioEntity destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", destinatarioId));

        if (destinatario.getId().equals(emisor.getId())) {
            throw new BusinessException("No puedes invitarte a ti mismo");
        }

        if (!amistadService.sonAmigos(emisor, destinatario)) {
            throw new BusinessException("Solo puedes invitar amistades confirmadas");
        }

        boolean yaEsMiembro = equipo.getMiembros().stream().anyMatch(m -> m.getId().equals(destinatario.getId()));
        if (yaEsMiembro) {
            throw new BusinessException("Esta persona ya forma parte del equipo");
        }

        if (equipoRapidoInvitacionRepository.existsByEquipoRapidoAndDestinatarioAndEstado(equipo, destinatario, EstadoInvitacion.PENDIENTE)) {
            throw new BusinessException("Ya existe una invitacion pendiente para esta persona");
        }

        EquipoRapidoInvitacionEntity invitacion = new EquipoRapidoInvitacionEntity();
        invitacion.setEquipoRapido(equipo);
        invitacion.setEmisor(emisor);
        invitacion.setDestinatario(destinatario);
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);
        invitacion.setMensaje("Has sido invitado a formar parte del equipo \"" + equipo.getNombre() + "\". Si aceptas, podras ser convocado directamente a partidos.");

        return equipoRapidoInvitacionRepository.save(invitacion);
    }

    @Transactional(readOnly = true)
    public List<EquipoRapidoInvitacionEntity> obtenerInvitacionesDeUsuario(UsuarioEntity usuario) {
        return equipoRapidoInvitacionRepository.findByDestinatarioOrderByCreadaEnDesc(usuario);
    }

    @Transactional(readOnly = true)
    public List<EquipoRapidoInvitacionEntity> obtenerInvitacionesPendientesDeUsuario(UsuarioEntity usuario) {
        return equipoRapidoInvitacionRepository.findByDestinatarioAndEstadoOrderByCreadaEnDesc(usuario, EstadoInvitacion.PENDIENTE);
    }

    @Transactional
    public EquipoRapidoInvitacionEntity aceptarInvitacion(Long invitacionId, UsuarioEntity destinatario) {
        EquipoRapidoInvitacionEntity invitacion = equipoRapidoInvitacionRepository.findByIdAndDestinatario(invitacionId, destinatario)
                .orElseThrow(() -> new ResourceNotFoundException("Invitacion de equipo", invitacionId));

        if (invitacion.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("Solo puedes aceptar invitaciones pendientes");
        }

        EquipoRapidoEntity equipo = invitacion.getEquipoRapido();
        int capacidad = equipo.getCapacidad() == null ? 7 : equipo.getCapacidad();
        int totalIntegrantes = 1 + equipo.getMiembros().size();
        if (totalIntegrantes >= capacidad) {
            throw new BusinessException("El equipo ya esta completo");
        }

        boolean yaEsMiembro = equipo.getMiembros().stream().anyMatch(m -> m.getId().equals(destinatario.getId()));
        if (!yaEsMiembro) {
            equipo.getMiembros().add(destinatario);
            equipoRapidoRepository.save(equipo);
        }

        invitacion.setEstado(EstadoInvitacion.ACEPTADA);
        invitacion.setRespondidaEn(LocalDateTime.now());
        return equipoRapidoInvitacionRepository.save(invitacion);
    }

    @Transactional
    public EquipoRapidoInvitacionEntity rechazarInvitacion(Long invitacionId, UsuarioEntity destinatario) {
        EquipoRapidoInvitacionEntity invitacion = equipoRapidoInvitacionRepository.findByIdAndDestinatario(invitacionId, destinatario)
                .orElseThrow(() -> new ResourceNotFoundException("Invitacion de equipo", invitacionId));

        if (invitacion.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new BusinessException("Solo puedes rechazar invitaciones pendientes");
        }

        invitacion.setEstado(EstadoInvitacion.RECHAZADA);
        invitacion.setRespondidaEn(LocalDateTime.now());
        return equipoRapidoInvitacionRepository.save(invitacion);
    }
}
