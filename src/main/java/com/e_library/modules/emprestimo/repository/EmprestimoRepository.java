package com.e_library.modules.emprestimo.repository;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EmprestimoRepository extends JpaRepository<EmprestimoEntity, UUID> {

    List<EmprestimoEntity> findByStatus(String status);

    List<EmprestimoEntity> findByUsuarioId(UUID usuarioId);

    // Empréstimos e reservas de um usuário (pelo e-mail do login), do prazo mais próximo para o mais distante
    List<EmprestimoEntity> findByUsuario_EmailOrderByDataPrevistaAsc(String email);

    // Livros que estão com o usuário (aguardando devolução), os que vencem primeiro no topo
    List<EmprestimoEntity> findByStatusInOrderByDataPrevistaAsc(Collection<String> status);

    // Pedidos de renegociação de prazo aguardando decisão do bibliotecário
    List<EmprestimoEntity> findByRenegociacaoStatus(String renegociacaoStatus);

    // Usado para impedir que o mesmo usuário reserve o mesmo livro duas vezes
    boolean existsByUsuario_IdAndLivro_IdAndStatusIn(UUID usuarioId, UUID livroId, Collection<String> status);

    @Query("SELECT e FROM EmprestimoEntity e WHERE e.status = 'ativo' ORDER BY e.dataPrevista ASC")
    List<EmprestimoEntity> findAllAtivosOrdenadosPorUrgencia();
}
