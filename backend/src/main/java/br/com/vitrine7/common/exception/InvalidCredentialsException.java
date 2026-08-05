package br.com.vitrine7.common.exception;

public class InvalidCredentialsException
        extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuário ou senha inválidos.");
    }
}
