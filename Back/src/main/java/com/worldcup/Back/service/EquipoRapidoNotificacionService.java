package com.worldcup.Back.service;

import com.worldcup.Back.dto.response.EquipoRapidoNotificacionResponseDTO;
import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.EquipoRapidoNotificacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.TipoNotificacionEquipo;
import com.worldcup.Back.repository.EquipoRapidoNotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoRapidoNotificacionService {

    @Autowired
    private EquipoRapidoNotificacionRepository equipoRapidoNotificacionRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public EquipoRapidoNotificacionEntity crearNotificacion(EquipoRapidoEntity equipo, UsuarioEntity destinatario, UsuarioEntity actor, TipoNotificacionEquipo tipo, String mensaje) {
        EquipoRapidoNotificacionEntity notificacion = new EquipoRapidoNotificacionEntity();
        notificacion.setEquipoRapidoId(equipo.getId());
        notificacion.setEquipoNombre(equipo.getNombre());
        notificacion.setDestinatario(destinatario);
        notificacion.setActor(actor);
        notificacion.setTipo(tipo);
        notificacion.setMensaje(mensaje);
        return equipoRapidoNotificacionRepository.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<EquipoRapidoNotificacionResponseDTO> obtenerNotificacionesDeUsuario(UsuarioEntity destinatario) {
        return equipoRapidoNotificacionRepository.findByDestinatarioOrderByCreadaEnDesc(destinatario)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarNotificacion(UsuarioEntity destinatario, Long notificacionId) {
        EquipoRapidoNotificacionEntity notificacion = equipoRapidoNotificacionRepository.findByIdAndDestinatario(notificacionId, destinatario)
                .orElseThrow(() -> new com.worldcup.Back.exception.ResourceNotFoundException("Notificacion de equipo", notificacionId));
        equipoRapidoNotificacionRepository.delete(notificacion);
    }

    private EquipoRapidoNotificacionResponseDTO toDTO(EquipoRapidoNotificacionEntity notificacion) {
        return new EquipoRapidoNotificacionResponseDTO(
                notificacion.getId(),
                notificacion.getEquipoRapidoId(),
                notificacion.getEquipoNombre(),
                userService.entityToResumenDTO(notificacion.getDestinatario()),
                notificacion.getActor() == null ? null : userService.entityToResumenDTO(notificacion.getActor()),
                notificacion.getTipo().name(),
                notificacion.getMensaje(),
                notificacion.getCreadaEn()
        );
    }
}