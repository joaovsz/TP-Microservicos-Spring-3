package br.edu.infnet.fornecedores.config;

import br.edu.infnet.fornecedores.model.Fornecedor;
import br.edu.infnet.fornecedores.repository.FornecedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final FornecedorRepository repository;

    public DataInitializer(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            log.info("Inicializando carga de 5 fornecedores padrão no banco de dados...");

            List<Fornecedor> fornecedores = Arrays.asList(
                    new Fornecedor("Tech Distribuidora LTDA", "11.222.333/0001-44", "contato@techdistribuidora.com.br", "(11) 3344-5566"),
                    new Fornecedor("Global Pecas e Componentes", "22.333.444/0001-55", "vendas@globalpecas.com.br", "(21) 2233-4455"),
                    new Fornecedor("Logistica Express Brasil", "33.444.555/0001-66", "operacoes@logisticaexpress.com.br", "(31) 98765-4321"),
                    new Fornecedor("Alimentos Brasil S/A", "44.555.666/0001-77", "comercial@alimentosbrasil.com.br", "(41) 3020-1000"),
                    new Fornecedor("Papelaria Central Atacadista", "55.666.777/0001-88", "pedidos@papelariacentral.com.br", "(51) 3210-9876")
            );

            repository.saveAll(fornecedores);
            log.info("Carga inicial concluída com sucesso: {} fornecedores cadastrados.", repository.count());
        } else {
            log.info("Banco de dados já contém fornecedores cadastrados ({})", repository.count());
        }
    }
}
