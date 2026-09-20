package br.edu.infnet.fornecedores.service;

import br.edu.infnet.fornecedores.model.Fornecedor;
import br.edu.infnet.fornecedores.repository.FornecedorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FornecedorService {

    private final FornecedorRepository repository;

    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado com id: " + id));
    }

    @Transactional
    public Fornecedor salvar(Fornecedor fornecedor) {
        if (fornecedor.getCnpj() != null && repository.existsByCnpj(fornecedor.getCnpj())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um fornecedor cadastrado com este CNPJ: " + fornecedor.getCnpj());
        }
        return repository.save(fornecedor);
    }
}
