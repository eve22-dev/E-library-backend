package com.e_library.modules.user.controller;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@CrossOrigin(origins = "*")
public class PerfilController {

    private static final long MAX_FOTO_BYTES = 2L * 1024 * 1024;

    private record FotoPerfil(byte[] bytes, String tipo) {}

    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;
    private final AnexoService anexoService;

    public PerfilController(UserRepository userRepository, JdbcTemplate jdbc, AnexoService anexoService) {
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.anexoService = anexoService;
    }

    private UserEntity usuario(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
    }

    // Dados do usuário logado, inclusive a preferência de aviso por e-mail e se há foto
    @GetMapping
    public ResponseEntity<Map<String, Object>> meusDados(Authentication auth) {
        UserEntity u = usuario(auth);

        Map<String, Object> extra = jdbc.queryForMap(
                "SELECT rgm, curso, notificar_email, (foto IS NOT NULL) AS tem_foto FROM usuarios WHERE id = ?",
                u.getId());

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", u.getId());
        dados.put("nome", u.getName());
        dados.put("email", u.getEmail());
        dados.put("papel", u.getRole());
        dados.put("rgm", extra.get("rgm"));
        dados.put("curso", extra.get("curso"));
        dados.put("notificarEmail", extra.get("notificar_email"));
        dados.put("temFoto", extra.get("tem_foto"));
        return ResponseEntity.ok(dados);
    }

    // Liga ou desliga os avisos de prazo por e-mail
    @PutMapping("/preferencias")
    public ResponseEntity<String> preferencias(@RequestBody Map<String, Object> corpo, Authentication auth) {
        boolean notificar = Boolean.TRUE.equals(corpo.get("notificarEmail"));
        jdbc.update("UPDATE usuarios SET notificar_email = ? WHERE id = ?", notificar, usuario(auth).getId());

        return ResponseEntity.ok(notificar
                ? "Você vai receber avisos de prazo por e-mail."
                : "Avisos de prazo por e-mail desativados.");
    }

    // Foto de perfil (JPG ou PNG, até 2 MB)
    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> enviarFoto(@RequestParam("foto") MultipartFile foto, Authentication auth) {

        if (foto == null || foto.isEmpty()) {
            return ResponseEntity.badRequest().body("Escolha uma imagem.");
        }

        if (foto.getSize() > MAX_FOTO_BYTES) {
            return ResponseEntity.status(413).body("A foto pode ter no máximo 2 MB.");
        }

        try {
            AnexoService.ArquivoValidado arquivo = anexoService.validar(List.of(foto)).get(0);

            if (!arquivo.tipo().startsWith("image/")) {
                return ResponseEntity.badRequest().body("Envie uma foto em JPG ou PNG.");
            }

            jdbc.update("UPDATE usuarios SET foto = ?, foto_tipo = ? WHERE id = ?",
                    arquivo.conteudo(), arquivo.tipo(), usuario(auth).getId());

            return ResponseEntity.ok("Foto atualizada!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto(Authentication auth) {
        List<FotoPerfil> achadas = jdbc.query(
                "SELECT foto, foto_tipo FROM usuarios WHERE id = ?",
                (rs, i) -> new FotoPerfil(rs.getBytes("foto"), rs.getString("foto_tipo")),
                usuario(auth).getId());

        if (achadas.isEmpty() || achadas.get(0).bytes() == null) {
            return ResponseEntity.notFound().build();
        }

        FotoPerfil f = achadas.get(0);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.tipo())).body(f.bytes());
    }
}
