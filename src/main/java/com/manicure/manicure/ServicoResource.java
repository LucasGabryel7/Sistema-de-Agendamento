package com.manicure.manicure;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Rota HTTP para listar os serviços.
 * Acessível em: GET http://localhost:8080/servicos
 */
@Path("/servicos")
@Produces(MediaType.APPLICATION_JSON)
public class ServicoResource {

    @GET
    public List<Servico> listar() {
        // listAll() vem do Panache e faz o SELECT * na tabela
        return Servico.listAll();
    }
}