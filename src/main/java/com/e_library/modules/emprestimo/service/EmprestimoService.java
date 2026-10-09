package com.e_library.modules.emprestimo.service;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;

    private final LivroRepository livroRepository;

    private final UserRepository userRepository;

    EmprestimoService(EmprestimoRepository emprestimoRepository, LivroRepository livroRepository, UserRepository userRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
        this.userRepository = userRepository;
    }

    // Realizar uma nova reserva/empréstimo com 14 dias de prazo automático
    public EmprestimoEntity realizarEmprestimo(UUID userId, UUID livroId) {
        
        // 1. Busca os objetos completos no banco de dados para garantir a integridade relacional
        UserEntity usuario = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        LivroEntity livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado."));

        // 2. Instancia a entidade vazia e preenche com os Setters
        EmprestimoEntity novoEmprestimo = new EmprestimoEntity();
        novoEmprestimo.setUsuario(usuario);
        novoEmprestimo.setLivro(livro);
        novoEmprestimo.setDataRetirada(LocalDate.now());
        novoEmprestimo.setDataPrevista(LocalDate.now().plusDays(14)); // 14 dias de prazo
        novoEmprestimo.setStatus("ativo");

        // 3. Salva no banco de dados
        return emprestimoRepository.save(novoEmprestimo);
    }

    // Listar empréstimos de um aluno específico
    public List<EmprestimoEntity> listarPorUsuario(UUID userId) {
        return emprestimoRepository.findByUsuarioId(userId);
    }

    // Listar empréstimos para o painel do bibliotecário (ordenados por urgência/vencimento)
    public List<EmprestimoEntity> listarParaPainelBibliotecario() {
        return emprestimoRepository.findAllAtivosOrdenadosPorUrgencia();
    }
}