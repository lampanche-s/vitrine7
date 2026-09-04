package br.com.vitrine7.system.user.dto;

import br.com.vitrine7.system.user.entity.UserRole;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void usernameAcceptsSimpleInternalSpacesSymbolsAndAccents() {
        assertValidUsername("Jorge");
        assertValidUsername("Jorge Caixa 01");
        assertValidUsername("jorge@loja#1!");
        assertValidUsername("João Árvore");
    }

    @Test
    void usernameCannotBeBlank() {
        CreateUserRequest request = requestWithUsername("   ");

        assertEquals(
                "Informe um nome de usuário.",
                validator.validate(request)
                        .iterator()
                        .next()
                        .getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abcdef",
            "123456",
            "!!!!!!",
            "aaaaaa",
            "senha simples",
            "áááááá"
    })
    void passwordAcceptsAnyCompositionWithAtLeastSixCharacters(
            String password
    ) {
        CreateUserRequest request = new CreateUserRequest(
                "Operador",
                "operador",
                password,
                UserRole.OPERADOR
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void passwordRejectsLessThanSixCharacters() {
        CreateUserRequest request = new CreateUserRequest(
                "Operador",
                "operador",
                "12345",
                UserRole.OPERADOR
        );

        assertEquals(
                "A senha deve ter pelo menos 6 caracteres.",
                validator.validate(request)
                        .iterator()
                        .next()
                        .getMessage()
        );
    }

    private void assertValidUsername(String username) {
        assertTrue(
                validator.validate(requestWithUsername(username)).isEmpty()
        );
    }

    private CreateUserRequest requestWithUsername(String username) {
        return new CreateUserRequest(
                "Operador",
                username,
                "abc123",
                UserRole.OPERADOR
        );
    }
}
