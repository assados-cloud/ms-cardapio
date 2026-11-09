package br.com.equipe15.cardapio.produto;

import br.com.equipe15.cardapio.instance.InstanceProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cardapio/produtos")
public class ProdutoController {
    private static final Logger log = LoggerFactory.getLogger(ProdutoController.class);
    private final ProdutoService service;
    private final InstanceProvider instance;

    public ProdutoController(ProdutoService service, InstanceProvider instance) {
        this.service = service;
        this.instance = instance;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listar() {
        return ResponseEntity.ok().header("X-Instance-Id", instance.hostname()).body(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscar(@PathVariable Long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("O ID do produto deve ser positivo.");
        }
        ProdutoResponse produto = service.buscar(id);
        log.info("produto {} atendido por pod={}", id, instance.hostname());
        return ResponseEntity.ok().header("X-Instance-Id", instance.hostname()).body(produto);
    }
}
