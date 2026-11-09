# Cardápio da Lanchonete

Microsserviço de produtos e preços da lanchonete, em Java 17 e Spring Boot 3.5.16. Banco exclusivo MySQL (`cardapiodb`), API REST, migrações Flyway, Docker e Kubernetes.

## Rodar localmente

Instale Docker Desktop com suporte a containers Linux e Docker Compose. Abra um terminal nesta pasta:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose logs -f ms-cardapio
```

No Linux/macOS use `cp .env.example .env`. O primeiro build baixa as dependências e executa os testes. Aguarde o log `Started CardapioApplication`.

```powershell
curl.exe http://localhost:8080/cardapio/produtos
curl.exe http://localhost:8080/cardapio/produtos/1
curl.exe http://localhost:8080/cardapio/instance
```

No Linux/macOS use `curl`. Para parar: `docker compose down`. O volume mantém os dados. As credenciais de `.env.example` servem apenas para desenvolvimento; defina outras no cluster. Alterar a senha no `.env` não muda a senha de um MySQL já inicializado no volume.

## Contrato da API

| Método | Caminho | Resultado |
| --- | --- | --- |
| GET | `/cardapio/produtos` | Lista ordenada por ID |
| GET | `/cardapio/produtos/{id}` | Produto usado pelo serviço de Pedidos |
| GET | `/cardapio/instance` | Nome do Pod que respondeu |

Produto:

```json
{"id": 1, "nome": "X-Burger", "preco": 25.00}
```

Lista inicial: ID 1 X-Burger R$ 25,00; ID 2 Batata R$ 12,00; ID 3 Refri R$ 7,00. Os IDs são estáveis para coincidir com o Estoque. Preço é número JSON e usa `BigDecimal`/`DECIMAL(10,2)`, evitando cálculo monetário com `double`.

Produto inexistente retorna 404. ID inválido, não positivo ou fora do intervalo de `Long` retorna 400. Erros usam `application/problem+json` com `status`, `title`, `detail` e `instance`. Consultas de produtos retornam também o cabeçalho `X-Instance-Id`.

O identificador do produto na resposta é `id`; `produto_id` é o nome usado nos itens do pedido e do evento. Combine este JSON com o responsável por Pedidos: o plano define os caminhos, mas não especifica todos os campos da resposta de produto. O DTO de Pedidos deve ler `id`, `nome` e `preco`.

Esta entrega implementa o contrato de consulta definido no plano, com catálogo inicial versionado por migração. Não há endpoints administrativos de cadastro/edição/exclusão. Para mudar o catálogo, crie uma migração nova; nunca altere uma migração já aplicada.

## Integração com Pedidos

No mesmo namespace Kubernetes, configure no serviço de Pedidos:

```text
CARDAPIO_URL=http://ms-cardapio:8080
```

Pedidos consulta `GET http://ms-cardapio:8080/cardapio/produtos/1` e utiliza o `preco` retornado como preço unitário. Configure timeout no cliente HTTP e trate 404, indisponibilidade e falhas de conexão. Não use o IP de um Pod e não aceite o preço fornecido pelo cliente final.

Se Pedidos estiver em outro namespace, use `http://ms-cardapio.lanchonete.svc.cluster.local:8080`. No Compose, outro serviço na mesma rede pode usar `http://ms-cardapio:8080`.

Pelo plano, Pedidos publica `pedido.criado`; Cozinha e Estoque consomem. Cardápio participa pela consulta REST e não precisa se conectar ao RabbitMQ. Os demais serviços não estão incluídos nesta entrega.

## Organização

- `produto`: entidade, repositório, serviço, DTO e controller.
- `instance`: identificação da instância para demonstrar balanceamento.
- `error`: respostas de erro HTTP.
- `src/main/resources/db/migration`: criação da tabela e carga inicial.
- `k8s`: namespace, banco com volume persistente, Deployment/Service e Ingress.
- `postman/Cardapio.postman_collection.json`: consultas e verificações básicas.

Injeção por construtor, sem Lombok, entidade separada do JSON e transações de leitura no serviço. Flyway executa as migrações uma vez e coordena a inicialização concorrente das réplicas pelo histórico de migrações; Hibernate apenas valida o esquema.

## Testar e compilar sem Docker

Requer JDK 17 e Maven 3.6.3 ou superior:

```text
mvn verify
```

Os sete testes de integração sobem o contexto Spring, aplicam as migrações em H2 no modo MySQL e exercitam os endpoints com MockMvc. Validam catálogo, contrato individual, 404, IDs inválidos, identificação do Pod, probes e rejeição de escrita. H2 não substitui a validação com MySQL real. O workflow do GitHub executa também os mesmos testes contra MySQL 8.4.

O build gera `target/ms-cardapio-1.0.0.jar`. Para executar com MySQL já disponível, configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, depois:

```text
java -jar target/ms-cardapio-1.0.0.jar
```

| Variável | Padrão / finalidade |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/cardapiodb` |
| `DB_USERNAME` | `cardapio` |
| `DB_PASSWORD` | Obrigatória |
| `SERVER_PORT` | `8080` |
| `HOSTNAME` | Nome da instância; no cluster vem do nome do Pod |

## Implantar no EKS

Pré-requisitos: cluster ativo e contexto kubectl correto, NGINX Ingress Controller instalado, registry acessível, EBS CSI configurado e StorageClass padrão para volumes. Use o namespace `lanchonete` também nos outros serviços ou ajuste os nomes DNS. Os arquivos não criam o cluster nem instalam o controller/CSI.

1. Publique a imagem, substituindo `SEU_REGISTRY` pelo Docker Hub ou ECR da equipe:

```text
docker build -t SEU_REGISTRY/ms-cardapio:1.0.0 .
docker push SEU_REGISTRY/ms-cardapio:1.0.0
```

2. Troque `SEU_REGISTRY` em `k8s/cardapio.yml` pela mesma referência. Para registro privado, configure autenticação de pull nos nós/ServiceAccount ou `imagePullSecrets`.

3. Crie o namespace e o Secret. No PowerShell, leia as senhas sem gravá-las no projeto (os comandos abaixo exigem PowerShell 7):

```powershell
kubectl apply -f k8s/namespace.yml
$dbPassword = Read-Host 'Senha do usuario cardapio' -MaskInput
$rootPassword = Read-Host 'Senha root do MySQL' -MaskInput
kubectl -n lanchonete create secret generic cardapio-db --from-literal="password=$dbPassword" --from-literal="root-password=$rootPassword"
Remove-Variable dbPassword, rootPassword
```

O comando de criação é para a primeira implantação. Se o Secret já existe, mantenha as credenciais combinadas com o MySQL existente. Secrets Kubernetes exigem controle de acesso adequado; não são criptografados só por estarem em base64.

4. Aplique o banco e aguarde sua inicialização:

```text
kubectl apply -f k8s/mysql.yml
kubectl -n lanchonete rollout status statefulset/mysql-cardapio --timeout=300s
kubectl apply -f k8s/cardapio.yml
kubectl -n lanchonete rollout status deployment/ms-cardapio --timeout=300s
kubectl apply -f k8s/ingress.yml
```

O MySQL usa StatefulSet com PVC de 5 GiB e uma réplica. Esse banco é adequado à demonstração acadêmica; não oferece alta disponibilidade ou backup automatizado. O namespace, as senhas e o banco são exclusivos do Cardápio. Não exponha o MySQL via Ingress ou LoadBalancer.

O Ingress mantém `/cardapio` no caminho, sem rewrite. O controller deve reconhecer `ingressClassName: nginx`. A API pública é de consulta e não exige autenticação, seguindo o escopo da demonstração. Não use esta configuração como administração pública de um catálogo.

## Roteiro de demonstração

```text
kubectl -n lanchonete get deployment ms-cardapio
kubectl -n lanchonete get pods -l app=ms-cardapio -o wide
kubectl -n lanchonete get svc mysql-cardapio ms-cardapio
kubectl -n lanchonete get pvc
kubectl -n lanchonete get ingress cardapio
kubectl -n ingress-nginx get svc
```

Use o hostname externo do controller como URL base, com `http://`. No PowerShell:

```powershell
$baseUrl = 'http://HOSTNAME-DO-INGRESS'
curl.exe "$baseUrl/cardapio/produtos"
curl.exe "$baseUrl/cardapio/produtos/1"
1..15 | ForEach-Object { curl.exe -s -H 'Connection: close' "$baseUrl/cardapio/instance"; Write-Output '' }
kubectl -n lanchonete logs -l app=ms-cardapio --prefix --tail=30
```

Cada chamada do loop abre uma conexão nova. A distribuição não é necessariamente round-robin nem garante mostrar os três Pods em poucas chamadas; repita se necessário e confira se as três réplicas estão Ready. Não use `port-forward deployment/ms-cardapio` como prova de balanceamento: ele escolhe um único Pod.

Para comprovar a consulta interna por DNS, use uma imagem de curl aprovada pela equipe:

```text
kubectl -n lanchonete run teste-cardapio --rm -i --restart=Never --image=curlimages/curl:8.12.1 -- http://ms-cardapio:8080/cardapio/produtos/1
```

Fala sugerida: “O Cardápio centraliza produtos e preços em um MySQL próprio. Pedidos consulta esta API por DNS do Service. Há três réplicas sem estado, compartilhando apenas o banco deste serviço. O endpoint instance mostra qual Pod respondeu. Flyway controla a criação e carga inicial; probes retiram instâncias indisponíveis do tráfego.”

## Verificação de saúde e diagnóstico

`/actuator/health/liveness` verifica a aplicação; `/actuator/health/readiness` inclui conectividade com o banco. Apenas rotas `/cardapio` são publicadas pelo Ingress.

- Pod `Pending`: confira recursos dos nós, PVC, StorageClass e EBS CSI.
- `ImagePullBackOff`: confira registry, tag e permissões de pull.
- `CrashLoopBackOff`: confira logs, Secret e acesso ao MySQL.
- Ingress 503: confira readiness e endpoints do Service.

```text
kubectl -n lanchonete describe pod NOME_DO_POD
kubectl -n lanchonete logs deployment/ms-cardapio --tail=100
kubectl -n lanchonete get endpointslice -l kubernetes.io/service-name=ms-cardapio
```

## Limites da entrega

Não foi realizada implantação na conta AWS da equipe, nem validação ponta a ponta com Pedidos. Os manifests precisam da imagem do registry e das configurações do cluster. A entrega do Cardápio sozinha não comprova a rubrica completa: a equipe ainda precisa demonstrar os outros serviços, dois bancos/linguagens e o fluxo RabbitMQ funcional.

Se esta pasta entrar em um monorepositório, ajuste o workflow para executar os comandos dentro da pasta `ms-cardapio` e configurar `cache-dependency-path` para seu `pom.xml`. O workflow incluído pressupõe esta pasta na raiz do repositório. Consulte `VERIFICACAO.md` para o que foi efetivamente testado.
