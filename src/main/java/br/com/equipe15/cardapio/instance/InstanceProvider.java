package br.com.equipe15.cardapio.instance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstanceProvider {
    private final String hostname;

    public InstanceProvider(@Value("${app.instance-id}") String hostname) {
        this.hostname = hostname;
    }

    public String hostname() {
        return hostname;
    }
}
