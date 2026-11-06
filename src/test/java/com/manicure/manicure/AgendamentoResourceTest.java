package com.manicure.manicure;
 
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
 
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
/**
 * Testes automáticos das rotas de agendamento, com foco na REGRA DE CONFLITO:
 * dois agendamentos confirmados não podem ocupar o mesmo horário.
 *
 * Como funciona:
 *  - @QuarkusTest sobe a aplicação de verdade (na porta 8081) usando o perfil "test",
 *    que aponta para o banco manicure_test (veja o application.properties).
 *  - O RestAssured faz as chamadas HTTP, como o navegador faria.
 *  - Cada teste começa com a tabela de agendamentos vazia (veja @BeforeEach).
 *
 * Serviços usados (cadastrados pela classe DadosIniciais):
 *  - Manicure: 45 minutos
 *  - Pedicure: 60 minutos
 */
@QuarkusTest
public class AgendamentoResourceTest {
 
    // Ids dos serviços neste banco de testes (descobertos no @BeforeEach)
    private long manicureId;
    private long pedicureId;
 
    // ------------------------------------------------------------------
    // PREPARAÇÃO: roda antes de CADA teste
    // ------------------------------------------------------------------
 
    @BeforeEach
    void prepararCenario() {
        // Apaga todos os agendamentos, para um teste não interferir no outro.
        // QuarkusTransaction abre e fecha uma transação só para esta operação.
        QuarkusTransaction.requiringNew().run(() -> Agendamento.deleteAll());
 
        // Descobre os ids dos serviços pela rota /servicos (em vez de supor 1 e 2)
        manicureId = idDoServico("Manicure");
        pedicureId = idDoServico("Pedicure");
    }
 
    // ------------------------------------------------------------------
    // TESTES DO CAMINHO FELIZ
    // ------------------------------------------------------------------
 
    @Test
    void servicosIniciaisForamCadastrados() {
        // A classe DadosIniciais deve ter criado os 3 serviços no banco vazio
        given()
            .when().get("/servicos")
            .then()
            .statusCode(200)
            .body("size()", is(3));
    }
 
    @Test
    void criaAgendamentoValido() {
        LocalDateTime inicio = amanhaAs(10, 0);
 
        agendar(manicureId, inicio)
            .then()
            .statusCode(201)                       // 201 = criado
            .body("id", notNullValue())            // ganhou um id
            .body("status", is("CONFIRMADO"))      // nasce confirmado
            // Manicure dura 45 min: 10:00 + 45 min = 10:45
            .body("fim", startsWith(inicio.toLocalDate() + "T10:45"));
    }
 
    // ------------------------------------------------------------------
    // TESTES DA REGRA DE CONFLITO: casos que DEVEM ser recusados (409)
    // ------------------------------------------------------------------
 
    @Test
    void recusaMesmoHorario() {
        LocalDateTime inicio = amanhaAs(10, 0);
 
        agendar(manicureId, inicio).then().statusCode(201);
 
        // Segunda tentativa, exatamente no mesmo horário
        agendar(manicureId, inicio)
            .then()
            .statusCode(409)                       // 409 = conflito
            .body("erro", containsString("ocupado"));
    }
 
    @Test
    void recusaQuandoNovoComecaNoMeioDeOutro() {
        // Existente: 10:00 até 10:45
        agendar(manicureId, amanhaAs(10, 0)).then().statusCode(201);
 
        // Novo começa às 10:30, ainda durante o primeiro
        agendar(manicureId, amanhaAs(10, 30)).then().statusCode(409);
    }
 
    @Test
    void recusaQuandoNovoTerminaNoMeioDeOutro() {
        // Existente: 10:00 até 10:45
        agendar(manicureId, amanhaAs(10, 0)).then().statusCode(201);
 
        // Novo (Pedicure, 60 min) começa às 09:30 e iria até 10:30,
        // invadindo o começo do primeiro
        agendar(pedicureId, amanhaAs(9, 30)).then().statusCode(409);
    }
 
    // ------------------------------------------------------------------
    // TESTES DE LIMITE: horários "colados" DEVEM ser aceitos (201)
    // ------------------------------------------------------------------
 
    @Test
    void aceitaHorarioLogoDepoisDeOutro() {
        // Existente: 10:00 até 10:45
        agendar(manicureId, amanhaAs(10, 0)).then().statusCode(201);
 
        // Novo começa exatamente quando o outro termina: não há sobreposição
        agendar(manicureId, amanhaAs(10, 45)).then().statusCode(201);
    }
 
    @Test
    void aceitaHorarioLogoAntesDeOutro() {
        // Existente: 10:00 até 10:45
        agendar(manicureId, amanhaAs(10, 0)).then().statusCode(201);
 
        // Novo (Manicure, 45 min) começa às 09:15 e termina exatamente às 10:00
        agendar(manicureId, amanhaAs(9, 15)).then().statusCode(201);
    }
 
    // ------------------------------------------------------------------
    // CANCELAMENTO
    // ------------------------------------------------------------------
 
    @Test
    void cancelamentoLiberaOHorario() {
        LocalDateTime inicio = amanhaAs(10, 0);
 
        // Cria e guarda o id do agendamento
        long id = agendar(manicureId, inicio)
            .then().statusCode(201)
            .extract().jsonPath().getLong("id");
 
        // Cancela: o agendamento continua no banco, mas com status CANCELADO
        given()
            .when().delete("/agendamentos/" + id)
            .then()
            .statusCode(200)
            .body("status", is("CANCELADO"));
 
        // Como o antigo está cancelado, o mesmo horário volta a ficar livre
        agendar(manicureId, inicio).then().statusCode(201);
    }
 
    @Test
    void cancelarAgendamentoInexistenteRetorna404() {
        given()
            .when().delete("/agendamentos/999999")
            .then()
            .statusCode(404);
    }
 
    // ------------------------------------------------------------------
    // VALIDAÇÕES
    // ------------------------------------------------------------------
 
    @Test
    void recusaDadosIncompletos() {
        // Falta telefone, servicoId e inicio
        given()
            .contentType(ContentType.JSON)
            .body("{ \"nomeCliente\": \"Ana\" }")
            .when().post("/agendamentos")
            .then()
            .statusCode(400);
    }
 
    @Test
    void recusaServicoInexistente() {
        agendar(999999L, amanhaAs(10, 0))
            .then()
            .statusCode(404);
    }
 
    @Test
    void recusaHorarioNoPassado() {
        // Ontem, 10:00
        LocalDateTime ontem = LocalDate.now().minusDays(1).atTime(10, 0);
 
        agendar(manicureId, ontem)
            .then()
            .statusCode(400);
    }
 
    // ------------------------------------------------------------------
    // MÉTODOS AUXILIARES (evitam repetir código nos testes)
    // ------------------------------------------------------------------
 
    /** Monta uma data/hora de AMANHÃ (sempre no futuro, então a regra do passado não atrapalha). */
    private LocalDateTime amanhaAs(int hora, int minuto) {
        return LocalDate.now().plusDays(1).atTime(hora, minuto);
    }
 
    /** Busca o id de um serviço pelo nome, usando GET /servicos. */
    private long idDoServico(String nome) {
        return given()
            .when().get("/servicos")
            .then().statusCode(200)
            .extract().jsonPath()
            .getLong("find { it.nome == '" + nome + "' }.id");
    }
 
    /** Faz POST /agendamentos com dados fixos de cliente e devolve a resposta. */
    private Response agendar(long servicoId, LocalDateTime inicio) {
        String json = """
                {
                  "nomeCliente": "Cliente Teste",
                  "telefone": "63999990000",
                  "servicoId": %d,
                  "inicio": "%s"
                }
                """.formatted(servicoId, inicio);
 
        return given()
            .contentType(ContentType.JSON)
            .body(json)
            .when().post("/agendamentos");
    }
}