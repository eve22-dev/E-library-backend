package com.e_library.modules.suporte.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Anexos de mensagens e reclamações: valida o arquivo (tipo, tamanho e conteúdo real)
 * e guarda os anexos das mensagens ao bibliotecário na tabela anexos.
 */
@Service
public class AnexoService {

    public static final int MAX_ARQUIVOS = 5;
    public static final long MAX_TOTAL_BYTES = 4L * 1024 * 1024; // limite do e-mail da ouvidoria

    private static final Set<String> EXTENSOES = Set.of("jpg", "jpeg", "png", "pdf");

    public record ArquivoValidado(String nome, String tipo, byte[] conteudo) {}

    public record AnexoBaixado(String nome, String tipo, byte[] conteudo) {}

    private final JdbcTemplate jdbc;

    public AnexoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Confere os arquivos enviados. Lança erro 400/413 com mensagem clara se algo estiver fora da regra.
    public List<ArquivoValidado> validar(List<MultipartFile> arquivos) {

        List<ArquivoValidado> validos = new ArrayList<>();

        if (arquivos == null) {
            return validos;
        }

        List<MultipartFile> reais = arquivos.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();

        if (reais.size() > MAX_ARQUIVOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Envie no máximo " + MAX_ARQUIVOS + " arquivos por vez.");
        }

        long total = 0;

        for (MultipartFile arquivo : reais) {
            byte[] bytes;
            try {
                bytes = arquivo.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler um dos arquivos.");
            }

            total += bytes.length;

            if (total > MAX_TOTAL_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Os anexos juntos podem ter no máximo 4 MB.");
            }

            String nome = limparNome(arquivo.getOriginalFilename());
            String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase() : "";

            if (!EXTENSOES.contains(extensao)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tipo de arquivo não permitido (" + nome + "). Use JPG, PNG ou PDF.");
            }

            String tipo = detectarTipo(bytes);

            if (tipo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O conteúdo de \"" + nome + "\" não é um JPG, PNG ou PDF válido.");
            }

            validos.add(new ArquivoValidado(nome, tipo, bytes));
        }

        return validos;
    }

    // Guarda os anexos de uma mensagem ao bibliotecário
    public void salvar(UUID mensagemId, List<ArquivoValidado> arquivos) {
        for (ArquivoValidado a : arquivos) {
            jdbc.update(
                    "INSERT INTO anexos (mensagem_id, nome, tipo, tamanho, conteudo) VALUES (?, ?, ?, ?, ?)",
                    mensagemId, a.nome(), a.tipo(), a.conteudo().length, a.conteudo());
        }
    }

    // Lista os anexos de uma mensagem (sem o conteúdo)
    public List<Map<String, Object>> listarDaMensagem(UUID mensagemId) {
        return jdbc.queryForList(
                "SELECT id, nome, tipo, tamanho FROM anexos WHERE mensagem_id = ? ORDER BY criado_em",
                mensagemId);
    }

    // Busca um anexo para download
    public Optional<AnexoBaixado> buscar(UUID id) {
        List<AnexoBaixado> achados = jdbc.query(
                "SELECT nome, tipo, conteudo FROM anexos WHERE id = ?",
                (rs, i) -> new AnexoBaixado(rs.getString("nome"), rs.getString("tipo"), rs.getBytes("conteudo")),
                id);

        return achados.stream().findFirst();
    }

    // Reconhece o tipo pelo conteúdo (e não só pela extensão)
    private String detectarTipo(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return "application/pdf";
        }
        return null;
    }

    private String limparNome(String original) {
        String nome = original == null ? "arquivo" : original;
        nome = nome.replace("\\", "/");
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        nome = nome.replaceAll("[^\\p{L}\\p{N}._\\- ]", "_").trim();

        if (nome.isEmpty()) {
            nome = "arquivo";
        }

        return nome.length() > 100 ? nome.substring(nome.length() - 100) : nome;
    }
}
