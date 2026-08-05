package br.com.vitrine7.system.user.security;

public enum Permission {

    LAVA_ACCESS("lava:access"),
    LAVA_MANAGE_CLIENTS("lava:manage-clients"),
    LAVA_MANAGE_SERVICES("lava:manage-services"),

    BAR_ACCESS("bar:access"),
    BAR_MANAGE_CATALOG("bar:manage-catalog"),
    BAR_STOCK_MANAGE("bar:stock-manage"),

    PAYMENT_REVERSE("payment:reverse"),

    ADMIN_USERS("admin:users"),
    ADMIN_SETTINGS("admin:settings"),
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
