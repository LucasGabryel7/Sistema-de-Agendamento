package com.manicure.manicure;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/**
 * Representa um horário marcado por um cliente.
 * Cada Agendamento vira uma linha na tabela "Agendamento".
 */
@Entity
public class Agendamento extends PanacheEntity {

    // Dados do cliente (para o teste, guardamos direto aqui, sem tabela separada)
    public String nomeCliente;
    public String telefone;

    // Muitos agendamentos podem apontar para o mesmo serviço
    @ManyToOne
    public Servico servico;

    // Quando começa e quando termina (início + duração do serviço)
    public LocalDateTime inicio;
    public LocalDateTime fim;

    // "CONFIRMADO" ou "CANCELADO"
    public String status = "CONFIRMADO";
}