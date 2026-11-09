package br.com.equipe15.cardapio.produto;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProdutoService {
    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public List<ProdutoResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(ProdutoResponse::from).toList();
    }

    public ProdutoResponse buscar(Long id) {
        return repository.findById(id).map(ProdutoResponse::from)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }
}
