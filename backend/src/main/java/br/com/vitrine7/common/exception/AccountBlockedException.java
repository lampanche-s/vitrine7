package br.com.vitrine7.common.exception;

public class AccountBlockedException
        extends RuntimeException {

    public static final String CODE = "ACCOUNT_BLOCKED";

    public AccountBlockedException() {
        super("Este usuário está bloqueado.");
    }
}
