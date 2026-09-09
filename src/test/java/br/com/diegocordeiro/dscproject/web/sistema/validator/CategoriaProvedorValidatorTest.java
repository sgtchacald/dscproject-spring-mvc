package br.com.diegocordeiro.dscproject.web.sistema.validator;

import br.com.diegocordeiro.dscproject.dto.categoriaprovedor.CategoriaProvedorFormDTO;
import br.com.diegocordeiro.dscproject.model.Categoria;
import br.com.diegocordeiro.dscproject.model.OpfiProvedor;
import br.com.diegocordeiro.dscproject.repository.CategoriaProvedorRepository;
import br.com.diegocordeiro.dscproject.repository.CategoriaRepository;
import br.com.diegocordeiro.dscproject.repository.OpfiProvedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaProvedorValidatorTest {

    @Mock
    private CategoriaProvedorRepository categoriaProvedorRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private OpfiProvedorRepository opfiProvedorRepository;

    @Mock
    private MessageSource messageSource;

    private CategoriaProvedorValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CategoriaProvedorValidator(
            categoriaProvedorRepository, categoriaRepository, opfiProvedorRepository, messageSource, Locale.getDefault());
    }

    @Test
    @DisplayName("Validação de unicidade de rótulo no provedor")
    void validate_quandoRotuloDuplicado_deveAdicionarErro() {
        when(categoriaProvedorRepository.contarPorProvedorERotulo(1L, "Food", null)).thenReturn(1L);
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(new Categoria()));
        when(opfiProvedorRepository.findById(1L)).thenReturn(Optional.of(new OpfiProvedor()));
        when(messageSource.getMessage(eq("categoriaprovedor.rotulo.duplicado"), any(), any()))
            .thenReturn("Já existe um vínculo para este rótulo neste provedor.");

        CategoriaProvedorFormDTO dto = new CategoriaProvedorFormDTO();
        dto.setProvedorId(1L);
        dto.setRotuloExterno("Food");
        dto.setCategoriaId(10L);

        Errors errors = new BeanPropertyBindingResult(dto, "vinculoForm");
        validator.validate(dto, errors);

        assertTrue(errors.hasFieldErrors("rotuloExterno"));
    }
}
