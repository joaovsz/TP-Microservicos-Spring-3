package br.edu.infnet.fornecedores.controller;

import br.edu.infnet.fornecedores.client.ProdutoClient;
import br.edu.infnet.fornecedores.dto.ProdutoDto;
import br.edu.infnet.fornecedores.model.Fornecedor;
import br.edu.infnet.fornecedores.service.FornecedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;
    private final ProdutoClient produtoClient;

    public FornecedorController(FornecedorService fornecedorService, ProdutoClient produtoClient) {
        this.fornecedorService = fornecedorService;
        this.produtoClient = produtoClient;
    }

    @GetMapping
    public ResponseEntity<List<Fornecedor>> listarTodos() {
        List<Fornecedor> fornecedores = fornecedorService.listarTodos();
        return ResponseEntity.ok(fornecedores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fornecedor> buscarPorId(@PathVariable Long id) {
        Fornecedor fornecedor = fornecedorService.buscarPorId(id);
        return ResponseEntity.ok(fornecedor);
    }

    @PostMapping
    public ResponseEntity<Fornecedor> cadastrar(@Valid @RequestBody Fornecedor fornecedor) {
        Fornecedor fornecedorSalvo = fornecedorService.salvar(fornecedor);
        URI location = URI.create("/fornecedores/" + fornecedorSalvo.getId());
        return ResponseEntity.created(location).body(fornecedorSalvo);
    }

    @GetMapping("/produtos")
    public ResponseEntity<List<ProdutoDto>> listarProdutosDoCatalogo() {
        List<ProdutoDto> produtos = produtoClient.listarProdutos();
        return ResponseEntity.ok(produtos);
    }
}
