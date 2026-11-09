package br.com.equipe15.cardapio.instance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstanceController {
    private final InstanceProvider instance;

    public InstanceController(InstanceProvider instance) {
        this.instance = instance;
    }

    @GetMapping("/cardapio/instance")
    public InstanceResponse consultar() {
        return new InstanceResponse(instance.hostname());
    }

    public record InstanceResponse(String hostname) {
    }
}
