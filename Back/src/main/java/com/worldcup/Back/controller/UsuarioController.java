package com.worldcup.Back.controller;

import com.worldcup.Back.dto.request.RasgosRequestDTO;
import com.worldcup.Back.dto.request.UsuarioPerfilRequestDTO;
import com.worldcup.Back.dto.response.UsuarioResumenDTO;
import com.worldcup.Back.dto.response.UsuarioPublicoDTO;
import com.worldcup.Back.dto.response.UsuarioVotacionResumenDTO;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.PartidoVotacionService;
import com.worldcup.Back.service.AmistadService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UserService userService;

    @Autowired
    private PartidoVotacionService partidoVotacionService;

    @Autowired
    private AmistadService amistadService;

    @PutMapping("/me")
    public ResponseEntity<UsuarioResumenDTO> upsertPerfil(
            HttpServletRequest request,
            @RequestBody(required = false) UsuarioPerfilRequestDTO perfil
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        // Use email from request body if provided, otherwise from header
        String email = (perfil != null && perfil.getEmail() != null) 
            ? perfil.getEmail() 
            : FirebaseRequestContext.getEmail(request);
        return ResponseEntity.ok(userService.upsertPerfil(uid, email, perfil));
    }

    @PutMapping("/me/rasgos")
    public ResponseEntity<UsuarioResumenDTO> actualizarRasgos(
            HttpServletRequest request,
            @RequestBody RasgosRequestDTO body
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        return ResponseEntity.ok(userService.actualizarRasgos(uid, body));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResumenDTO> obtenerMiUsuario(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        return ResponseEntity.ok(userService.obtenerResumenPorFirebaseUid(uid));
    }

    @GetMapping("/me/votacion-resumen")
    public ResponseEntity<UsuarioVotacionResumenDTO> obtenerMiResumenVotacion(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(partidoVotacionService.obtenerResumenVotacionDeUsuario(usuario.get()));
    }

    @GetMapping("/{id}/votacion-resumen-publico")
    public ResponseEntity<UsuarioVotacionResumenDTO> obtenerResumenVotacionPublico(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<UsuarioEntity> solicitante = userService.buscarPorFirebaseUid(uid);
        Optional<UsuarioEntity> objetivo = userService.buscarPorId(id);

        if (solicitante.isEmpty() || objetivo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!amistadService.sonAmigos(solicitante.get(), objetivo.get())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(partidoVotacionService.obtenerResumenVotacionDeUsuario(objetivo.get()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResumenDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerResumen(id));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioPublicoDTO>> listarUsuarios() {
        return ResponseEntity.ok(userService.listarPublicos());
    }
}
