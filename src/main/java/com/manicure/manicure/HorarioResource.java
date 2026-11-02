package com.manicure.manicure;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rota que devolve os horários livres de um dia.
 * Exemplo: GET /horarios?data=2026-11-10&servicoId=1
 */
@Path("/horarios")
@Produces(MediaType.APPLICATION_JSON)
public class HorarioResource {

    // Regras do expediente (para o teste, fixas no código)
    private static final LocalTime ABERTURA = LocalTime.of(9, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    private static final int INTERVALO_MINUTOS = 30; // de quanto em quanto tempo há opção de horário

    @GET
    public Response listar(@QueryParam("data") String dataTexto,
                           @QueryParam("servicoId") Long servicoId) {

        // 1) Validação dos parâmetros recebidos
        if (dataTexto == null || servicoId == null) {
            return erro(Response.Status.BAD_REQUEST, "Informe data (AAAA-MM-DD) e servicoId.");
        }

        LocalDate data;
        try {
            data = LocalDate.parse(dataTexto); // espera o formato 2026-11-10
        } catch (DateTimeParseException e) {
            return erro(Response.Status.BAD_REQUEST, "Data inválida. Use o formato AAAA-MM-DD.");
        }

        Servico servico = Servico.findById(servicoId);
        if (servico == null) {
            return erro(Response.Status.NOT_FOUND, "Serviço não encontrado.");
        }

        // 2) Busca UMA vez todos os agendamentos confirmados do dia
        //    (mais eficiente do que consultar o banco a cada horário testado)
        LocalDateTime inicioDoDia = data.atStartOfDay();
        LocalDateTime fimDoDia = data.plusDays(1).atStartOfDay();
        List<Agendamento> ocupados = Agendamento.list(
                "status = 'CONFIRMADO' and inicio < ?1 and fim > ?2",
                fimDoDia, inicioDoDia);

        // 3) Percorre os horários do expediente e guarda os que estão livres
        List<String> livres = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime horario = data.atTime(ABERTURA);
        LocalDateTime limite = data.atTime(FECHAMENTO);

        while (!horario.isAfter(limite)) {
            LocalDateTime fim = horario.plusMinutes(servico.duracaoMinutos);

            // O serviço precisa terminar até o fechamento
            boolean cabeNoExpediente = !fim.isAfter(limite);
            // Não oferece horários que já passaram
            boolean ehFuturo = horario.isAfter(agora);
            // Não pode se sobrepor a nenhum agendamento confirmado
            final LocalDateTime inicioTeste = horario;
            boolean conflita = ocupados.stream()
                    .anyMatch(a -> a.inicio.isBefore(fim) && a.fim.isAfter(inicioTeste));

            if (cabeNoExpediente && ehFuturo && !conflita) {
                livres.add(horario.toLocalTime().toString()); // ex.: "09:00"
            }

            horario = horario.plusMinutes(INTERVALO_MINUTOS);
        }

        return Response.ok(Map.of(
                "data", data.toString(),
                "servico", servico.nome,
                "horarios", livres
        )).build();
    }

    private Response erro(Response.Status status, String mensagem) {
        return Response.status(status).entity(Map.of("erro", mensagem)).build();
    }
}