package com.e_library.modules.livro.repository;

import com.e_library.modules.livro.entity.LivroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LivroRepository extends JpaRepository<LivroEntity, UUID> {

    List<LivroEntity> findByTituloContainingIgnoreCaseAndAtivoTrue(String titulo);

    List<LivroEntity> findByAutorContainingIgnoreCaseAndAtivoTrue(String autor);

    List<LivroEntity> findByIsbnContainingIgnoreCaseAndAtivoTrue(String isbn);

    List<LivroEntity> findByEditoraContainingIgnoreCaseAndAtivoTrue(String editora);

    boolean existsByCategoria_IdAndAtivoTrue(UUID categoriaId);

    // Procura pelo ISBN ignorando hífens e espaços (ex.: 978-85-... é igual a 97885...)
    @Query("SELECT l FROM LivroEntity l WHERE l.ativo = true " +
           "AND REPLACE(REPLACE(l.isbn, '-', ''), ' ', '') = :isbn")
    List<LivroEntity> buscarPorIsbnNormalizado(@Param("isbn") String isbn);

    // Soma exemplares novos ao total e ao disponível de uma vez só
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LivroEntity l SET l.qtdTotal = COALESCE(l.qtdTotal, 0) + :n, " +
           "l.qtdDisponivel = COALESCE(l.qtdDisponivel, 0) + :n WHERE l.id = :id")
    int acrescentarExemplares(@Param("id") UUID id, @Param("n") int n);

    // Tira 1 unidade do estoque de uma vez só. Retorna 0 se o livro está inativo ou sem unidades.
    // Por ser um único UPDATE, duas pessoas reservando ao mesmo tempo não passam do estoque.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LivroEntity l SET l.qtdDisponivel = l.qtdDisponivel - 1 " +
           "WHERE l.id = :id AND l.ativo = true AND l.qtdDisponivel > 0")
    int reservarUmaUnidade(@Param("id") UUID id);

    // Devolve 1 unidade ao estoque, sem nunca passar do total cadastrado.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LivroEntity l SET l.qtdDisponivel = " +
           "CASE WHEN l.qtdDisponivel < l.qtdTotal THEN l.qtdDisponivel + 1 ELSE l.qtdTotal END " +
           "WHERE l.id = :id AND l.qtdTotal IS NOT NULL")
    int devolverUmaUnidade(@Param("id") UUID id);

    // Extravio: o exemplar perdido sai do total do acervo (o disponível já tinha baixado na reserva).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LivroEntity l SET l.qtdTotal = l.qtdTotal - 1 WHERE l.id = :id AND l.qtdTotal > 0")
    int baixarUmExemplarDoTotal(@Param("id") UUID id);
}
