package br.com.diegocordeiro.dscproject.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acessoRaiz_semAutenticacao_redirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void paginaLogin_semAutenticacao_retornaOk() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void webjars_semAutenticacao_naoRedirecionaParaLogin() throws Exception {
        var result = mockMvc.perform(get("/webjars/tabler__core/1.4.0/dist/css/tabler.min.css"))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(302);
    }
}
