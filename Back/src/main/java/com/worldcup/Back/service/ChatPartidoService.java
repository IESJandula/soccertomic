package com.worldcup.Back.service;

import com.worldcup.Back.entity.ChatPartidoEntity;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.repository.ChatPartidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatPartidoService {

    @Autowired
    private ChatPartidoRepository chatPartidoRepository;

    @Transactional
    public ChatPartidoEntity enviarMensaje(PartidoEntity partido, UsuarioEntity usuario, String mensajePredefinido) {
        ChatPartidoEntity mensaje = new ChatPartidoEntity();
        mensaje.setPartido(partido);
        mensaje.setUsuario(usuario);
        mensaje.setMensajePredefinido(mensajePredefinido);
        mensaje.setCreadoEn(LocalDateTime.now());
        return chatPartidoRepository.save(mensaje);
    }

    public List<ChatPartidoEntity> obtenerChatDePartido(PartidoEntity partido) {
        return chatPartidoRepository.findByPartido(partido);
    }

    @Transactional
    public void limpiarChatDePartido(PartidoEntity partido) {
        List<ChatPartidoEntity> mensajes = chatPartidoRepository.findByPartido(partido);
        chatPartidoRepository.deleteAll(mensajes);
    }
}
