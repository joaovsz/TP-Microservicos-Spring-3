package br.edu.infnet.fornecedores.repository;

import br.edu.infnet.fornecedores.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
