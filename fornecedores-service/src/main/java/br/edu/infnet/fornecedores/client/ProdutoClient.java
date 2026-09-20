package br.edu.infnet.fornecedores.client;

import br.edu.infnet.fornecedores.dto.ProdutoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "produtos-service")
public interface ProdutoClient {

    @GetMapping("/produtos")
    List<ProdutoDto> listarProdutos();

    @GetMapping("/produtos/{id}")
    ProdutoDto buscarProdutoPorId(@PathVariable("id") Long id);
}
