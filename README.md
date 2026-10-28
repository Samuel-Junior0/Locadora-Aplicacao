# Locadora

CRUD de locadora de filmes em Java puro (sem Spring, sem ORM, sem camada web) usando
**Garnet** como banco key-value.

## Stack

| Item | Versão / escolha |
| --- | --- |
| Java | 23 |
| Build | Maven via `./mvnw` |
| Banco | Garnet (`ghcr.io/microsoft/garnet:latest`, porta 6379) |
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
docker compose up -d          # sobe o Garnet
./mvnw compile exec:java      # compila e roda com.locadora.app.Main
docker compose down           # para o banco, mantendo os dados
docker compose down -v        # para e apaga os dados
```

O `Main` executa uma demonstração do CRUD de cliente e apaga o que criou no final.
Host e porta podem ser trocados por `GARNET_HOST` e `GARNET_PORT`.

## Estrutura

```
src/main/java/com/locadora/
├── app/          Main (demonstração)
├── model/        Cliente
├── repository/   ClienteRepository
├── service/      ClienteService
├── exception/    Exceções de negócio
└── util/         Cpf, Json, Keys, ConexaoGarnet
```

## Como funciona

Cada entidade é gravada como JSON em uma chave (`cliente:{id}`), e os índices são chaves
separadas (`idx:cliente:cpf:{cpf}`). O `ClienteRepository` é o único lugar que escreve no
banco e mantém entidade + índices na mesma transação (`MULTI/EXEC`).

## Funcionalidades

- **Cliente** (implementado): cadastrar, buscar por id, buscar por CPF, atualizar, remover
  e listar. CPF único, normalizado para 11 dígitos.
- **Filme**, **Locação** e **Pagamento**: próximos passos.
