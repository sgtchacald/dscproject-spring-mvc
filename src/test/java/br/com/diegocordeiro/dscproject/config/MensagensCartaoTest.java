package br.com.diegocordeiro.dscproject.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A tela "Meus Cartões" (listar.html) referencia chaves de messages.properties
 * via th:data-msg-*. Se a chave não existir, o Thymeleaf renderiza o texto cru
 * "??chave_pt_BR??" na interface. Este teste resolve exatamente as chaves que o
 * template consome, contra o mesmo MessageSource da aplicação.
 */
class MensagensCartaoTest {

    private final MessageSource messageSource = new WebMvcConfig().messageSource();
    private final Locale locale = new Locale("pt", "BR");

    @Test
    @DisplayName("MSG09 - msg.cartao.filtro.vazio deve resolver (listar.html referencia essa chave)")
    void filtroVazio_deveResolverParaOTextoDoDocumento() {
        String mensagem = messageSource.getMessage("msg.cartao.filtro.vazio", null, locale);
        assertThat(mensagem).isEqualTo("Nenhum cartão encontrado com os filtros informados.");
    }

    @Test
    @DisplayName("MSG06 - msg.cartao.confirma.exclusao deve resolver (listar.html referencia essa chave)")
    void confirmaExclusao_deveResolverParaOTextoDoDocumento() {
        String mensagem = messageSource.getMessage("msg.cartao.confirma.exclusao", new Object[]{"Nubank"}, locale);
        assertThat(mensagem).isEqualTo("Confirma a exclusão do cartão \"Nubank\"?");
    }
}
