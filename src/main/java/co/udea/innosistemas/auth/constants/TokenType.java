package co.udea.innosistemas.auth.constants;

import lombok.Getter;

@Getter
public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");
    
    private final String value;
    
    TokenType(String value) {
        this.value = value;
    }

}