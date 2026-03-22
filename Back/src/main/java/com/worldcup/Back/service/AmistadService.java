package com.worldcup.Back.service;

import com.worldcup.Back.entity.AmistadEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoAmistad;
import com.worldcup.Back.exception.BusinessException;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.AmistadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AmistadService {

    @Autowired
    private AmistadRepository amistadRepository;

    @Transactional
    public AmistadEntity enviarSolicitud(UsuarioEntity usuarioA, UsuarioEntity usuarioB) {
        if (usuarioA.getId().equals(usuarioB.getId())) {
            throw new BusinessException("No puedes enviarte una solicitud de amistad a ti mismo");
        }

        // Verificar que no exista ya una solicitud
        Optional<AmistadEntity> existente = amistadRepository.findBetweenUsuarios(usuarioA, usuarioB);
        if (existente.isPresent()) {
            throw new BusinessException("Ya existe una solicitud de amistad entre estos usuarios");
        }

        AmistadEntity amistad = new AmistadEntity();
        amistad.setUsuarioA(usuarioA);
        amistad.setUsuarioB(usuarioB);
        amistad.setEstado(EstadoAmistad.SOLICITADA);
        amistad.setCreadaEn(LocalDateTime.now());
        return amistadRepository.save(amistad);
    }

    public List<AmistadEntity> obtenerAmigosDeUsuario(UsuarioEntity usuario) {
        return amistadRepository.findByUsuarioAndEstado(usuario, EstadoAmistad.ACEPTADA);
    }

    public List<AmistadEntity> obtenerSolicitudesPendientes(UsuarioEntity usuario) {
        return amistadRepository.findByUsuarioBAndEstado(usuario, EstadoAmistad.SOLICITADA);
    }

    @Transactional
    public AmistadEntity aceptarSolicitud(Long amistadId) {
        Optional<AmistadEntity> amistad = amistadRepository.findById(amistadId);
        if (amistad.isPresent()) {
            AmistadEntity a = amistad.get();
            if (a.getEstado() != EstadoAmistad.SOLICITADA) {
                throw new BusinessException("Solo se pueden aceptar solicitudes pendientes");
            }
            a.setEstado(EstadoAmistad.ACEPTADA);
            a.setAceptadaEn(LocalDateTime.now());
            return amistadRepository.save(a);
        }
        throw new ResourceNotFoundException("Amistad", amistadId);
    }

    @Transactional
    public AmistadEntity rechazarSolicitud(Long amistadId) {
        Optional<AmistadEntity> amistad = amistadRepository.findById(amistadId);
        if (amistad.isPresent()) {
            AmistadEntity a = amistad.get();
            if (a.getEstado() != EstadoAmistad.SOLICITADA) {
                throw new BusinessException("Solo se pueden rechazar solicitudes pendientes");
            }
            a.setEstado(EstadoAmistad.RECHAZADA);
            return amistadRepository.save(a);
        }
        throw new RuntimeException("Amistad no encontrada");
    }

    public List<AmistadEntity> obtenerSolicitudesEnviadas(UsuarioEntity usuario) {
        return amistadRepository.findSolicitudesEnviadas(usuario, EstadoAmistad.SOLICITADA);
    }

    @Transactional(readOnly = true)
    public boolean sonAmigos(UsuarioEntity usuarioA, UsuarioEntity usuarioB) {
        if (usuarioA == null || usuarioB == null) return false;
        if (usuarioA.getId().equals(usuarioB.getId())) return true;

        return amistadRepository.findBetweenUsuarios(usuarioA, usuarioB)
                .map(amistad -> amistad.getEstado() == EstadoAmistad.ACEPTADA)
                .orElse(false);
    }

    @Transactional
    public void eliminarAmistad(Long amistadId) {
        amistadRepository.deleteById(amistadId);
    }
}
