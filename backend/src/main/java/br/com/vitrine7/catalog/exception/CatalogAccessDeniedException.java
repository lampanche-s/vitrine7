package br.com.vitrine7.catalog.exception;

public class CatalogAccessDeniedException extends RuntimeException {

    public static final String CODE = "CATALOG_ACCESS_DENIED";

    public CatalogAccessDeniedException() {
        super("Senha de acesso ao catálogo inválida.");
    }
}
