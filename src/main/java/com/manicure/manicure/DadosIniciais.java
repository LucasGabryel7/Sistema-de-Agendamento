package com.manicure.manicure;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

/**
 * Inicializa os dados padrão do sistema quando a aplicação sobe pela primeira vez.
 * Como o banco de dados agora mantém os dados entre reinícios, esta classe só
 * insere os serviços padrão caso a tabela ainda esteja vazia.
 */
@ApplicationScoped
public class DadosIniciais {

    /**
     * Executado automaticamente assim que o Quarkus inicializa.
     * A anotação @Transactional garante que as operações no banco sejam salvas dentro de uma transação.
     */
    @Transactional
    public void aoIniciar(@Observes StartupEvent evento) {
        // Se já existir algum serviço cadastrado, não faz nada para preservar os dados
        if (Servico.count() > 0) {
            return;
        }

        // Cria e salva os serviços padrão da manicure
        criar("Manicure", 45, 35.00);
        criar("Pedicure", 60, 45.00);
        criar("Manicure + Pedicure", 100, 70.00);
    }

    /**
     * Método auxiliar privado para evitar repetição na criação e persistência de cada serviço.
     */
    private void criar(String nome, int duracaoMinutos, double preco) {
        Servico servico = new Servico();
        servico.nome = nome;
        servico.duracaoMinutos = duracaoMinutos;
        servico.preco = preco;
        servico.persist();
    }
}

