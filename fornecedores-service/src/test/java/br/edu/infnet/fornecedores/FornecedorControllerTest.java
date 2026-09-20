package br.edu.infnet.fornecedores;

import br.edu.infnet.fornecedores.client.ProdutoClient;
import br.edu.infnet.fornecedores.dto.ProdutoDto;
import br.edu.infnet.fornecedores.model.Fornecedor;
import br.edu.infnet.fornecedores.repository.FornecedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FornecedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FornecedorRepository repository;

    @MockBean
    private ProdutoClient produtoClient;

    @Test
    @DisplayName("GET /fornecedores deve retornar 200 OK e lista com os 5 fornecedores pre-cadastrados")
    void deveListarTodosFornecedores() throws Exception {
        mockMvc.perform(get("/fornecedores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[0].nome", notNullValue()))
                .andExpect(jsonPath("$[0].cnpj", notNullValue()));
    }

    @Test
    @DisplayName("GET /fornecedores/{id} existente deve retornar 200 OK com os dados do fornecedor")
    void deveBuscarFornecedorPorIdExistente() throws Exception {
        Fornecedor primeiro = repository.findAll().get(0);

        mockMvc.perform(get("/fornecedores/" + primeiro.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(primeiro.getId().intValue())))
                .andExpect(jsonPath("$.nome", is(primeiro.getNome())))
                .andExpect(jsonPath("$.cnpj", is(primeiro.getCnpj())));
    }

    @Test
    @DisplayName("GET /fornecedores/{id} inexistente deve retornar 404 Not Found")
    void deveRetornar404QuandoIdInexistente() throws Exception {
        mockMvc.perform(get("/fornecedores/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /fornecedores deve cadastrar novo fornecedor e retornar 201 Created")
    void deveCadastrarNovoFornecedor() throws Exception {
        String json = """
                {
                    "nome": "Distribuidora Nacional de Testes",
                    "cnpj": "99.888.777/0001-11",
                    "email": "contato@distribuidoranacional.com.br",
                    "telefone": "(11) 9999-8888"
                }
                """;

        mockMvc.perform(post("/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", is("Distribuidora Nacional de Testes")))
                .andExpect(jsonPath("$.cnpj", is("99.888.777/0001-11")));
    }

    @Test
    @DisplayName("GET /fornecedores/produtos deve retornar lista consumida via Feign")
    void deveRetornarProdutosViaFeign() throws Exception {
        when(produtoClient.listarProdutos()).thenReturn(List.of(
                new ProdutoDto(1L, "MacBook Pro M3 Max", new BigDecimal("19999.00"), 10)
        ));

        mockMvc.perform(get("/fornecedores/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", is("MacBook Pro M3 Max")));
    }
}
