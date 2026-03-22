package com.worldcup.Back.controller;

import com.worldcup.Back.dto.response.ChatPartidoResponseDTO;
import com.worldcup.Back.entity.ChatPartidoEntity;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.ChatPartidoService;
import com.worldcup.Back.service.PartidoService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat-partidos")
public class ChatPartidoController {

    @Autowired
    private ChatPartidoService chatPartidoService;

    @Autowired
    private PartidoService partidoService;

    @Autowired
    private UserService userService;
    
    // Helper to convert Entity to DTO
    private ChatPartidoResponseDTO entityToDTO(ChatPartidoEntity mensaje) {
        return new ChatPartidoResponseDTO(
            mensaje.getId(),
            mensaje.getPartido().getId(),
            userService.entityToResumenDTO(mensaje.getUsuario()),
            mensaje.getMensajePredefinido(),
            mensaje.getCreadoEn()
        );
    }

    @PostMapping
    public ResponseEntity<ChatPartidoResponseDTO> enviarMensaje(
            HttpServletRequest request,
            @RequestParam Long partidoId,
            @RequestParam String mensajePredefinido
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(partidoId);
        Optional<UsuarioEntity> usuario = userService.buscarPorFirebaseUid(uid);

        if (partido.isEmpty() || usuario.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChatPartidoEntity mensaje = chatPartidoService.enviarMensaje(partido.get(), usuario.get(), mensajePredefinido);
        return ResponseEntity.ok(entityToDTO(mensaje));
    }

    @GetMapping("/{partidoId}")
    public ResponseEntity<List<ChatPartidoResponseDTO>> obtenerChatDePartido(@PathVariable Long partidoId) {
        Optional<PartidoEntity> partido = partidoService.obtenerPorId(partidoId);

        if (partido.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<ChatPartidoEntity> chat = chatPartidoService.obtenerChatDePartido(partido.get());
        List<ChatPartidoResponseDTO> dtos = chat.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
