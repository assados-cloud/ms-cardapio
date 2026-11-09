package br.com.equipe15.cardapio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardapioIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void listaSeedComIdsEstaveisEPrecosCorretos() throws Exception {
        mvc.perform(get("/cardapio/produtos"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Instance-Id", "pod-teste"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("X-Burger"))
                .andExpect(jsonPath("$[0].preco").value(25.00))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].preco").value(12.00))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].preco").value(7.00));
    }

    @Test
    void consultaProdutoNoContratoUsadoPorPedidos() throws Exception {
        mvc.perform(get("/cardapio/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":1,"nome":"X-Burger","preco":25.00}
                        """, true));
    }

    @Test
    void retorna404ParaProdutoInexistente() throws Exception {
        mvc.perform(get("/cardapio/produtos/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Produto 999 não encontrado."));
    }

    @Test
    void rejeitaIdsInvalidosSemErroInterno() throws Exception {
        for (String id : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            mvc.perform(get("/cardapio/produtos/" + id))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void identificaPodParaDemonstrarBalanceamento() throws Exception {
        mvc.perform(get("/cardapio/instance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("pod-teste"));
    }

    @Test
    void probesIndicamAplicacaoEBancoSaudaveis() throws Exception {
        for (String probe : new String[]{"liveness", "readiness"}) {
            mvc.perform(get("/actuator/health/" + probe))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    void naoExpoeGravacaoNoContratoDeConsulta() throws Exception {
        mvc.perform(post("/cardapio/produtos").contentType("application/json")
                .content("{\"nome\":\"Outro\",\"preco\":1}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
