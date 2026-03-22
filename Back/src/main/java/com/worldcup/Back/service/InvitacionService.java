package com.worldcup.Back.service;

import com.worldcup.Back.entity.InvitacionEntity;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoInvitacion;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.InvitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvitacionService {

    @Autowired
    private InvitacionRepository invitacionRepository;

    @Transactional
    public InvitacionEntity crearInvitacion(PartidoEntity partido, UsuarioEntity usuario) {
        InvitacionEntity invitacion = new InvitacionEntity();
        invitacion.setPartido(partido);
        invitacion.setUsuario(usuario);
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);
        invitacion.setPagada(false);
        invitacion.setCreadaEn(LocalDateTime.now());
        return invitacionRepository.save(invitacion);
    }

    public List<InvitacionEntity> obtenerInvitacionesDeUsuario(UsuarioEntity usuario) {
        return invitacionRepository.findByUsuario(usuario);
    }

    public List<InvitacionEntity> obtenerInvitacionesPendientes(UsuarioEntity usuario) {
        return invitacionRepository.findByUsuarioAndEstado(usuario, EstadoInvitacion.PENDIENTE);
    }

    public List<InvitacionEntity> obtenerInvitacionesDePartido(PartidoEntity partido) {
        return invitacionRepository.findByPartido(partido);
    }

    @Transactional
    public InvitacionEntity aceptarInvitacion(Long invitacionId) {
        Optional<InvitacionEntity> invitacion = invitacionRepository.findById(invitacionId);
        if (invitacion.isPresent()) {
            InvitacionEntity inv = invitacion.get();
            inv.setEstado(EstadoInvitacion.ACEPTADA);
            inv.setPagada(false);
            inv.setRespondidaEn(LocalDateTime.now());
            return invitacionRepository.save(inv);
        }
        throw new ResourceNotFoundException("Invitación", invitacionId);
    }

    @Transactional
    public InvitacionEntity rechazarInvitacion(Long invitacionId) {
        Optional<InvitacionEntity> invitacion = invitacionRepository.findById(invitacionId);
        if (invitacion.isPresent()) {
            InvitacionEntity inv = invitacion.get();
            inv.setEstado(EstadoInvitacion.RECHAZADA);
            inv.setPagada(false);
            inv.setRespondidaEn(LocalDateTime.now());
            return invitacionRepository.save(inv);
        }
        throw new ResourceNotFoundException("Invitación", invitacionId);
    }

    public InvitacionEntity crearNotificacionCancelacion(PartidoEntity partido, UsuarioEntity usuario) {
        Optional<InvitacionEntity> existente = invitacionRepository.findByPartidoAndUsuario(partido, usuario);
        if (existente.isPresent()) {
            InvitacionEntity invitacion = existente.get();
            invitacion.setEstado(EstadoInvitacion.CANCELADA);
            invitacion.setMensaje(null);
            invitacion.setPrecioTotalPista(null);
            invitacion.setParteIndividual(null);
            invitacion.setPagada(false);
            invitacion.setRespondidaEn(LocalDateTime.now());
            return invitacionRepository.save(invitacion);
        }

        InvitacionEntity notificacion = new InvitacionEntity();
        notificacion.setPartido(partido);
        notificacion.setUsuario(usuario);
        notificacion.setEstado(EstadoInvitacion.CANCELADA);
        notificacion.setMensaje(null);
        notificacion.setPrecioTotalPista(null);
        notificacion.setParteIndividual(null);
        notificacion.setPagada(false);
        notificacion.setCreadaEn(LocalDateTime.now());
        notificacion.setRespondidaEn(LocalDateTime.now());
        return invitacionRepository.save(notificacion);
    }

    public InvitacionEntity crearNotificacionReserva(PartidoEntity partido,
                                                     UsuarioEntity usuario,
                                                     String mensaje,
                                                     BigDecimal precioTotalPista,
                                                     BigDecimal parteIndividual,
                                                     Long reservadoPorUsuarioId,
                                                     String reservadoPorNombre,
                                                     boolean pagada) {
        InvitacionEntity notificacion = new InvitacionEntity();
        notificacion.setPartido(partido);
        notificacion.setUsuario(usuario);
        notificacion.setEstado(EstadoInvitacion.ACEPTADA);
        notificacion.setMensaje(mensaje);
        notificacion.setPrecioTotalPista(precioTotalPista);
        notificacion.setParteIndividual(parteIndividual);
        notificacion.setReservadoPorUsuarioId(reservadoPorUsuarioId);
        notificacion.setReservadoPorNombre(reservadoPorNombre);
        notificacion.setPagada(pagada);
        notificacion.setCreadaEn(LocalDateTime.now());
        notificacion.setRespondidaEn(LocalDateTime.now());
        return invitacionRepository.save(notificacion);
    }

    @Transactional
    public InvitacionEntity marcarReservaComoPagada(Long invitacionId, UsuarioEntity usuario) {
        InvitacionEntity invitacion = invitacionRepository.findById(invitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitación", invitacionId));

        if (invitacion.getUsuario() == null || !invitacion.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No puedes modificar esta notificación");
        }

        if (invitacion.getPrecioTotalPista() == null || invitacion.getParteIndividual() == null) {
            throw new RuntimeException("Esta notificación no corresponde a una reserva de pista");
        }

        invitacion.setPagada(true);
        invitacion.setRespondidaEn(LocalDateTime.now());
        return invitacionRepository.save(invitacion);
    }

    @Transactional
    public void eliminarInvitacion(Long invitacionId) {
        invitacionRepository.deleteById(invitacionId);
    }
}
