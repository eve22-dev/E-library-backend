package com.e_library.modules.emprestimo.repository;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface EmprestimoRepository extends JpaRepository<EmprestimoEntity, UUID> {

    // Busca todos os empréstimos de um usuário específico
    List<EmprestimoEntity> findByUserId(UUID userId);

    // Busca empréstimos ordenados por data prevista (os mais urgentes primeiro para o painel do bibliotecário)
    @Query("SELECT e FROM EmprestimoEntity e WHERE e.status = 'ativo' ORDER BY e.dataPrevista ASC")
    List<EmprestimoEntity> findAllAtivosOrdenadosPorUrgencia();
}