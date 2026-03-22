package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.RasgosRequestDTO;
import com.worldcup.Back.dto.request.UsuarioPerfilRequestDTO;
import com.worldcup.Back.dto.response.UsuarioPublicoDTO;
import com.worldcup.Back.dto.response.UsuarioResumenDTO;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Transactional
    public UsuarioResumenDTO upsertPerfil(String firebaseUid, String emailToken, UsuarioPerfilRequestDTO request){
        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(UsuarioEntity::new);

        usuario.setFirebaseUid(firebaseUid);
        if (emailToken != null && !emailToken.isBlank()) {
            usuario.setEmail(emailToken);
        }

        if (request != null && request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombre(request.getNombre());
        } else if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            usuario.setNombre(emailToken != null ? emailToken : firebaseUid);
        }

        if (usuario.getRasgos() == null) {
            usuario.setRasgos(new ArrayList<>());
        }


        UsuarioEntity guardado = usuarioRepository.save(usuario);
        return entityToResumenDTO(guardado);
    }

    @Transactional
    public UsuarioResumenDTO actualizarRasgos(String firebaseUid, RasgosRequestDTO request) {
        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setRasgos(request.getRasgos());
        UsuarioEntity guardado = usuarioRepository.save(usuario);
        return entityToResumenDTO(guardado);
    }

    public UsuarioResumenDTO obtenerResumenPorFirebaseUid(String firebaseUid) {
        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return entityToResumenDTO(usuario);
    }

    public UsuarioResumenDTO obtenerResumen(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        return entityToResumenDTO(usuario);
    }

    public UsuarioResumenDTO entityToResumenDTO(UsuarioEntity usuario) {
        String skillTier = null;
        String playTendency = null;
        
        if (usuario.getPlayerProfile() != null) {
            skillTier = usuario.getPlayerProfile().getSkillTier();
            playTendency = usuario.getPlayerProfile().getPlayTendency();
        }
        
        return new UsuarioResumenDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getNivel(),
                usuario.getReputacionPositiva(),
                usuario.getRasgos(),
                skillTier,
                playTendency
        );
    }

        public List<UsuarioPublicoDTO> listarPublicos() {
        return usuarioRepository.findAll()
            .stream()
            .map(usuario -> new UsuarioPublicoDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getPuntos(),
                usuario.getRasgos()
            ))
            .collect(Collectors.toList());
        }
}
