package br.com.vitrine7.common.security;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;

    public static final String MESSAGE =
            "A senha deve ter pelo menos 6 caracteres.";

    private PasswordPolicy() {
    }
}
