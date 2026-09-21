package com.e_library.modules.emprestimo.service;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository) {
        this.emprestimoRepository = emprestimoRepository;
    }

    // Realizar uma nova reserva/empréstimo com 14 dias de prazo automático
    public EmprestimoEntity realizarEmprestimo(UUID userId, UUID livroId) {
        LocalDate hoje = LocalDate.now();
        LocalDate dataPrevista = hoje.plusDays(14); // 14 dias de prazo

        EmprestimoEntity novoEmprestimo = new EmprestimoEntity(
            userId,
            livroId,
            hoje,
            dataPrevista,
            "ativo"
        );

        return emprestimoRepository.save(novoEmprestimo);
    }

    // Listar empréstimos de um aluno específico
    public List<EmprestimoEntity> listarPorUsuario(UUID userId) {
        return emprestimoRepository.findByUserId(userId);
    }

    // Listar empréstimos para o painel do bibliotecário (ordenados por urgência/vencimento)
    public List<EmprestimoEntity> listarParaPainelBibliotecario() {
        return emprestimoRepository.findAllAtivosOrdenadosPorUrgencia();
    }
}