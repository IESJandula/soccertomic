package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.RasgosRequestDTO;
import com.worldcup.Back.dto.request.UsuarioPerfilRequestDTO;
import com.worldcup.Back.dto.response.UsuarioPublicoDTO;
import com.worldcup.Back.dto.response.UsuarioResumenDTO;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.UsuarioRepository;
import com.worldcup.Back.security.FirebaseRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<UsuarioEntity> listarUsuarios(){
        return usuarioRepository.findAll();
    }

    public Optional<UsuarioEntity> buscarPorId(Long id){
        return usuarioRepository.findById(id);
    }

    public Optional<UsuarioEntity> buscarPorFirebaseUid(String firebaseUid) {
        return usuarioRepository.findByFirebaseUid(firebaseUid);
    }

    private String resolveStorageEmail(String firebaseUid, String emailToken) {
        if (emailToken != null && !emailToken.isBlank()) {
            return emailToken.trim();
        }
        return firebaseUid + "@firebase.local";
    }

    @Transactional
    public UsuarioEntity obtenerOCrearPorFirebaseUid(String firebaseUid, String emailToken) {
        return usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    UsuarioEntity nuevo = new UsuarioEntity();
                    String resolvedEmail = resolveStorageEmail(firebaseUid, emailToken);
                    nuevo.setFirebaseUid(firebaseUid);
                    nuevo.setEmail(resolvedEmail);
                    nuevo.setNombre(resolvedEmail);
                    nuevo.setRasgos(new ArrayList<>());
                    return usuarioRepository.save(nuevo);
                });
    }

    @Transactional
    public UsuarioEntity obtenerOCrearDesdeRequest(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        String email = FirebaseRequestContext.getEmail(request);
        return obtenerOCrearPorFirebaseUid(uid, email);
    }

    @Transactional
    public UsuarioResumenDTO upsertPerfil(String firebaseUid, String emailToken, UsuarioPerfilRequestDTO request){
        UsuarioEntity usuario = obtenerOCrearPorFirebaseUid(firebaseUid, emailToken);
        String resolvedEmail = resolveStorageEmail(firebaseUid, emailToken);

        usuario.setFirebaseUid(firebaseUid);
        usuario.setEmail(resolvedEmail);

        if (request != null && request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombre(request.getNombre());
        } else if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            usuario.setNombre(resolvedEmail);
        }

        if (usuario.getRasgos() == null) {
            usuario.setRasgos(new ArrayList<>());
        }


        UsuarioEntity guardado = usuarioRepository.save(usuario);
        return entityToResumenDTO(guardado, resolvedEmail);
    }

    @Transactional
    public UsuarioResumenDTO actualizarRasgos(String firebaseUid, RasgosRequestDTO request) {
        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setRasgos(request.getRasgos());
        UsuarioEntity guardado = usuarioRepository.save(usuario);
        return entityToResumenDTO(guardado, null);
    }

    public UsuarioResumenDTO obtenerResumenPorFirebaseUid(String firebaseUid) {
        return obtenerResumenPorFirebaseUid(firebaseUid, null);
    }

    public UsuarioResumenDTO obtenerResumenPorFirebaseUid(String firebaseUid, String emailContexto) {
        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return entityToResumenDTO(usuario, emailContexto);
    }

    public UsuarioResumenDTO obtenerResumen(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        return entityToResumenDTO(usuario, null);
    }

    public UsuarioResumenDTO entityToResumenDTO(UsuarioEntity usuario) {
        return entityToResumenDTO(usuario, null);
    }

    public UsuarioResumenDTO entityToResumenDTO(UsuarioEntity usuario, String emailContexto) {
        String playTendency = lazySafe(() -> {
            if (usuario.getPlayerProfile() != null) {
                return usuario.getPlayerProfile().getPlayTendency();
            }
            return null;
        }, null);

        Long id = lazySafe(usuario::getId, null);
        String nombre = lazySafe(usuario::getNombre, null);
        String nivel = lazySafe(usuario::getNivel, null);
        Integer reputacionPositiva = lazySafe(usuario::getReputacionPositiva, null);
        List<String> rasgos = lazySafe(usuario::getRasgos, List.of());
        
        return new UsuarioResumenDTO(
                id,
                nombre,
                emailContexto,
                nivel,
                reputacionPositiva,
                rasgos,
                playTendency
        );
    }

    private <T> T lazySafe(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (LazyInitializationException e) {
            return fallback;
        }
    }

    public List<UsuarioPublicoDTO> listarPublicos() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuario -> new UsuarioPublicoDTO(
                    lazySafe(usuario::getId, null),
                    lazySafe(usuario::getNombre, null),
                    lazySafe(usuario::getPuntos, null),
                    lazySafe(usuario::getRasgos, List.of())
                ))
                .collect(Collectors.toList());
    }
}
