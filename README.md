# Locadora

CRUD de locadora de filmes em Java puro (sem Spring, sem ORM, sem camada web) usando
**Garnet** como banco key-value.

## Stack

| Item | Versão / escolha |
| --- | --- |
| Java | 23 |
| Build | Maven via `./mvnw` |
| Banco | Garnet (`ghcr.io/microsoft/garnet:2.2.5`, porta 6379) |
| Cliente | Jedis 8.0.2 |
| JSON | Jackson 2.22.4 |
| Testes | JUnit 5.14.4 |

## Requisitos

- JDK 23+
- Docker com Compose

Se houver mais de um JDK instalado, aponte o 23 para o build:

```bash
JAVA_HOME=~/.jdks/corretto-23.0.2 ./mvnw clean package
```

## Como rodar

```bash
docker compose up -d --wait   # sobe o Garnet e espera o healthcheck
./mvnw compile exec:java      # compila e roda com.locadora.app.Main
docker compose down           # para o banco, mantendo os dados
docker compose down -v        # para e apaga os dados
```

O `Main` abre um **menu de console** para gerenciar a aplicação:

```
===== LOCADORA =====
1 - Clientes
2 - Filmes
3 - Locações
4 - Pagamentos
5 - Status do banco
0 - Sair
```

Cada submenu cobre cadastro, busca, listagem, atualização e remoção (quando aplicável),
além de alugar/devolver filmes e gerar pagamentos. Host e porta podem ser trocados por
`GARNET_HOST` e `GARNET_PORT`.

Para popular o banco com um cenário de demonstração que **fica gravado** (útil para
inspecionar na extensão do VS Code):

```bash
./mvnw compile exec:java -Dexec.mainClass=com.locadora.app.MainPopularBanco
```

O `MainPopularBanco` cria clientes, filmes, locações e pagamentos e não remove nada
ao final. Pode ser executado várias vezes: CPFs e títulos recebem valores únicos.

## Estrutura

```
src/main/java/com/locadora/
├── app/          Main, MainPopularBanco, MainLocadora, MainFilme
├── menu/         MenuPrincipal, MenuCliente, MenuFilme, MenuLocacao,
│                 MenuPagamento, MenuStatus e MenuBase
├── model/        Cliente, Filme, Locacao, Pagamento
├── repository/   BaseRepository + repositórios por entidade
├── service/      BaseService + serviços por entidade
├── exception/    Exceções de negócio
└── util/         Cpf, Json, Keys, ConexaoGarnet, validadores
```

## Como funciona

Cada entidade é gravada como JSON em uma chave (`cliente:{id}`), e os índices são chaves
separadas (`idx:cliente:cpf:{cpf}` e o conjunto `clientes:ids`). O `ClienteRepository` é o
único lugar que escreve no banco e mantém entidade + índices na mesma transação
(`MULTI/EXEC`). A listagem lê o conjunto com `SMEMBERS` e os valores com `MGET`.

## Funcionalidades

- **Cliente**: cadastrar, buscar por id, buscar por CPF, atualizar, remover e listar.
  CPF único, normalizado para 11 dígitos.
- **Filme**: cadastrar, buscar por id/título, atualizar, remover e listar. Título único
  e controle de disponibilidade.
- **Locação**: alugar, devolver, buscar por id, listar abertas/devolvidas e por
  cliente/filme.
- **Pagamento**: gerar para locação devolvida, buscar por id/locação e listar.
- **Status**: contadores de cada entidade e total de chaves no Garnet.

## Limpeza dos dados

```text
FLUSHDB
SAVE
```

O `SAVE` é necessário: apenas `FLUSHDB` é perdido no restart porque o AOF antigo é
reprocessado na inicialização.

Para um reset completo (apaga AOF, checkpoint e volume):

```bash
docker compose down -v && docker compose up -d --wait
```
