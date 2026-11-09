# Verificação da entrega

Executada em 09/11/2026 com JDK 17 e Maven 3.9.11.

- `mvn verify`: BUILD SUCCESS, Spring Boot 3.5.16.
- 7 testes de integração: zero falhas, zero erros, zero ignorados.
- Contexto Spring, JPA, Flyway e endpoints exercitados com H2 no modo MySQL.
- Duas migrações aplicadas com sucesso durante os testes.
- JAR executável gerado pelo plugin Spring Boot.
- YAML das configurações, Compose, Kubernetes e workflow validado por parser.
- JSON da coleção Postman validado por parser.

Não executados neste ambiente: Docker Compose, testes com MySQL real, workflow do GitHub, implantação no EKS e integração com o serviço de Pedidos. Docker não está instalado aqui. O workflow contém um job para testes com MySQL 8.4; ele deve ser executado no repositório da equipe. Os manifests exigem registry, Secret e infraestrutura do cluster.

O catálogo possui apenas os endpoints de consulta descritos no plano da equipe. Não inclui CRUD administrativo, frontend nem os outros microsserviços.
