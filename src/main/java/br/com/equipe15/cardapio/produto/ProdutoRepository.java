package br.com.equipe15.cardapio.produto;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
