package com.e_library.modules.user.repository; // Ajuste se usar outro pacote

import com.e_library.modules.user.entity.MensagemSuporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MensagemSuporteRepository extends JpaRepository<MensagemSuporteEntity, UUID> {
    // O JpaRepository já traz os métodos de salvar, buscar, deletar etc.
}