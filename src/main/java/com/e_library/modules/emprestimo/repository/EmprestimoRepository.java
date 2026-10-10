package com.e_library.modules.emprestimo.repository;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface EmprestimoRepository extends JpaRepository<EmprestimoEntity, UUID> {

    List<EmprestimoEntity> findByStatus(String status);    

    List<EmprestimoEntity> findByUsuarioId(UUID usuarioId);
    
    @Query("SELECT e FROM EmprestimoEntity e WHERE e.status = 'ativo' ORDER BY e.dataPrevista ASC")
    List<EmprestimoEntity> findAllAtivosOrdenadosPorUrgencia();
    
}