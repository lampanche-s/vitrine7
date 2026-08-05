package br.com.vitrine7.system.user.security;

public enum Permission {

    BAR_ACCESS("bar:access"),
    BAR_MANAGE_CATALOG("bar:manage-catalog"),

    CLIENTS_MANAGE("clients:manage"),

    PAYMENT_REVERSE("payment:reverse"),

    ADMIN_USERS("admin:users"),
    ADMIN_PAYMENT_CONFIG("admin:payment-config"),

    REPORTS_ACCESS("reports:access");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
