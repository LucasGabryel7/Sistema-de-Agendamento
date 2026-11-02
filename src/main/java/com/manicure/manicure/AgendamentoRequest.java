package com.manicure.manicure;

import java.time.LocalDateTime;

/**
 * Dados que chegam do cliente ao pedir um agendamento.
 * "record" é um jeito curto do Java de criar uma classe só de dados.
 */
public record AgendamentoRequest(
        String nomeCliente,
        String telefone,
        Long servicoId,
        LocalDateTime inicio
) {}