package com.manicure.manicure
;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rotas de agendamento, acessíveis em /agendamentos
 */
@Path("/agendamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgendamentoResource {

    /** Lista todos os agendamentos. GET /agendamentos */
    @GET
    public List<Agendamento> listar() {
        return Agendamento.listAll();
    }

    /** Cria um agendamento. POST /agendamentos */
    @POST
    @Transactional // abre uma transação no banco: ou salva tudo, ou nada
    public Response criar(AgendamentoRequest req) {

        // 1) Validação básica dos dados recebidos
        if (req == null || req.nomeCliente() == null || req.nomeCliente().isBlank()
                || req.telefone() == null || req.telefone().isBlank()
                || req.servicoId() == null || req.inicio() == null) {
            return erro(Response.Status.BAD_REQUEST,
                    "Informe nomeCliente, telefone, servicoId e inicio.");
        }

        // 2) O serviço escolhido precisa existir
        Servico servico = Servico.findById(req.servicoId());
        if (servico == null) {
            return erro(Response.Status.NOT_FOUND, "Serviço não encontrado.");
        }

        // 3) Não permite agendar no passado
        if (req.inicio().isBefore(LocalDateTime.now())) {
            return erro(Response.Status.BAD_REQUEST,
                    "Não é possível agendar em um horário que já passou.");
        }

        // 4) Calcula quando termina: início + duração do serviço
        LocalDateTime fim = req.inicio().plusMinutes(servico.duracaoMinutos);

        // 5) REGRA PRINCIPAL: procura agendamentos confirmados que se sobreponham.
        //    Conflito = (existente.inicio < novo.fim) E (existente.fim > novo.inicio)
        long conflitos = Agendamento.count(
                "status = 'CONFIRMADO' and inicio < ?1 and fim > ?2",
                fim, req.inicio());

        if (conflitos > 0) {
            return erro(Response.Status.CONFLICT,
                    "Esse horário já está ocupado. Escolha outro.");
        }

        // 6) Tudo certo: salva o agendamento
        Agendamento ag = new Agendamento();
        ag.nomeCliente = req.nomeCliente();
        ag.telefone = req.telefone();
        ag.servico = servico;
        ag.inicio = req.inicio();
        ag.fim = fim;
        ag.persist();

        return Response.status(Response.Status.CREATED).entity(ag).build();
    }

    /** Cancela um agendamento. DELETE /agendamentos/{id} */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response cancelar(@PathParam("id") Long id) {
        Agendamento ag = Agendamento.findById(id);
        if (ag == null) {
            return erro(Response.Status.NOT_FOUND, "Agendamento não encontrado.");
        }
        // Não apaga do banco: só marca como cancelado, mantendo o histórico
        ag.status = "CANCELADO";
        return Response.ok(ag).build();
    }

    /** Monta uma resposta de erro em JSON: {"erro": "mensagem"} */
    private Response erro(Response.Status status, String mensagem) {
        return Response.status(status)
                .entity(java.util.Map.of("erro", mensagem))
                .build();
    }
}