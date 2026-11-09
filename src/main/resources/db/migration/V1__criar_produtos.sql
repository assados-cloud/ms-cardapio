CREATE TABLE produtos (
    id BIGINT NOT NULL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    preco DECIMAL(10, 2) NOT NULL,
    CONSTRAINT ck_produtos_preco CHECK (preco > 0)
);
