package br.edu.infnet.produtos.controller;

import br.edu.infnet.produtos.model.Produto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final Map<Long, Produto> produtos = new ConcurrentHashMap<>();

    public ProdutoController() {
        produtos.put(1L, new Produto(1L, "MacBook Pro M3 Max", new BigDecimal("19999.00"), 10));
        produtos.put(2L, new Produto(2L, "Monitor Dell UltraSharp 32 4K", new BigDecimal("3600.00"), 25));
        produtos.put(3L, new Produto(3L, "Teclado Mecanico Keychron K2", new BigDecimal("650.00"), 40));
        produtos.put(4L, new Produto(4L, "Mouse Logitech MX Master 3S", new BigDecimal("550.00"), 50));
        produtos.put(5L, new Produto(5L, "Headset Sony WH-1000XM5", new BigDecimal("2200.00"), 15));
    }

    @GetMapping
    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos.values());
    }

    @GetMapping("/{id}")
    public Produto buscarPorId(@PathVariable Long id) {
        Produto produto = produtos.get(id);
        if (produto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado com id: " + id);
        }
        return produto;
    }
}
