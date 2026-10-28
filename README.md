# Locadora

CRUD de locadora de filmes em Java puro (sem Spring, sem ORM, sem camada web) usando
**Garnet** como banco key-value.

## Stack

| Item | Versão / escolha |
| --- | --- |
| Java | 23 (`maven.compiler.release=23`, bytecode major 67) |
| Build | Maven 3.9.16 via `./mvnw` (wrapper incluso, não precisa instalar Maven) |
| Banco | Garnet `ghcr.io/microsoft/garnet:latest` (RESP, porta 6379) |
| Cliente | Jedis 8.0.2 |
| JSON | Jackson 2.22.4 (`databind` + `jsr310`) |
| Testes | JUnit 5.14.4 |

Garnet foi escolhido por ser um banco de servidor, key-value, com licença MIT e engine
própria (Tsavorite, derivada do FASTER). Ele fala RESP, então o cliente é o Jedis — não
existe driver JDBC. Não é Redis nem Valkey: o protocolo é o mesmo, a implementação não.

## Requisitos

- **JDK 23+** para compilar. Na IDE, o Project SDK está em JDK 23
  (aqui: Corretto 23.0.2 em `~/.jdks/corretto-23.0.2`).
- **Docker** com Compose plugin (a imagem do Garnet é baixada no primeiro `up`).
- Maven **não** precisa estar instalado: o build passa pelo `./mvnw`.

O `./mvnw` usa o `JAVA_HOME` quando definido; senão, o `java` do `PATH`. Se houver mais de
um JDK na máquina, force o 23 para o build:

```bash
JAVA_HOME=~/.jdks/corretto-23.0.2 ./mvnw clean package
```

O `pom.xml` fixa `--release 23`, então mesmo um JDK mais novo gera bytecode 23. O
`.idea/` fica fora do Git: quem clonar o repositório só precisa apontar o SDK para um
JDK 23+ e importar o `pom.xml`.

No IntelliJ, se o projeto ainda aparecer como "Java puro" (com `out/` em vez de `target/`),
rode **Maven → Reload All Maven Projects** para o módulo ser reconhecido como Maven.

## Como rodar

```bash
docker compose up -d          # sobe o Garnet e espera o healthcheck
./mvnw compile exec:java      # compila e roda com.locadora.app.Main
docker compose logs -f garnet # logs do servidor
docker compose down           # para o container, mantendo os dados
docker compose down -v        # para e APAGA o volume (dados perdidos)
```

O `Main` executa uma demonstração do CRUD de cliente (inserir, buscar, atualizar, remover)
e apaga o que criou no final. Host e porta podem ser trocados por `GARNET_HOST` e
`GARNET_PORT`.

## Estrutura

```
src/main/java/com/locadora/
├── app/          Main (demonstração do CRUD de cliente; depois vira o menu)
├── model/        Cliente (JavaBean + Jackson)
├── repository/   ClienteRepository (único lugar que escreve no Garnet)
├── service/      ClienteService (casos de uso)
├── exception/    CpfJaCadastradoException, ClienteNaoEncontradoException
└── util/         Cpf, Json, Keys (montagem de chaves), ConexaoGarnet
```

## Camadas

```
Main (CLI)  ->  Service (casos de uso)  ->  Repository (Garnet)  ->  model/util
```

- **`app/Main`** — entrada e saída. Não monta chave, não chama `SET`/`DEL` e não constrói
  entidade: só chama o service. (Na demonstração ele ainda usa `Jedis` para mostrar as
  chaves cruas; isso é didático, não é o fluxo do sistema.)
- **`service`** — casos de uso (`cadastrar`, `atualizar`, `remover`, `buscar`, `listar`) e
  as regras que **não** são de persistência. Hoje o `ClienteService` é fino, porque o CRUD
  de cliente sozinho não tem regra de negócio. A camada passa a valer quando entrar
  `Locacao`: é o service que vai checar cliente/filme, calcular o período e impedir
  remover cliente com locação aberta — coisas que envolvem mais de um repositório.
- **`repository`** — só persistência: gravar a entidade e manter os índices na mesma
  transação. Um repositório por entidade, e nenhum outro lugar escreve no banco.
- **`model`** — os dados e a validação de campo (nome obrigatório, CPF com 11 dígitos).
- **`util`** — chaves, JSON, CPF e conexão.
- **`exception`** — exceções de negócio, separadas para o `Main` e o service não dependerem
  de detalhes do repositório para tratá-las.

## Modelo de chaves

```
cliente:{id}                     JSON do cliente
filme:{id}                       JSON do filme
locacao:{id}                     JSON da locação
pagamento:{codigo}               JSON do pagamento (existir = pago)

seq:cliente / seq:filme / seq:locacao     contadores (INCR)
idx:cliente:cpf:{cpf}            → id       garante CPF único
idx:filme:genero:{genero}        → SET de ids
idx:filme:disponivel             → SET de ids
idx:locacao:cliente:{clienteId}  → SET de ids
idx:locacao:aberta               → SET de ids
idx:pagamento:locacao:{id}       → SET de códigos
```

Regras que o repositório precisa respeitar:

- Entidade + índices sempre na mesma transação (`MULTI/EXEC` via `Transaction` do Jedis).
  `MSET` **não** é atômico no Garnet — não usar para gravar dado + índice.
- Nenhuma escrita/remoção fora do repositório.
- Sem JOIN: a listagem é montada no Java (locação → cliente/filme).
- `Locacao` guarda `dias` e `valorDiaria` (preço congelado no dia do empréstimo);
  `valorTotal() = dias * valorDiaria`. O pagamento nunca relê o preço do filme.
- `Pagamento` é namespace próprio (`codigo`, `locacaoId`, `valor`, `dataPagamento`),
  sem campo `status`: se o registro existe, está pago.
- Pagamento é gerado **na devolução**.

## CRUD de cliente (implementado)

`Cliente` é um JavaBean clássico (construtor sem argumentos + getters/setters, que é o
formato que o Jackson espera): `id`, `nome`, `cpf`, com o CPF sempre normalizado para 11
dígitos. Para rodar a demonstração:

```bash
./mvnw compile exec:java
```

Cada operação toca estas chaves:

| Operação | Chaves | Comandos |
| --- | --- | --- |
| inserir | `seq:cliente`, `cliente:{id}`, `idx:cliente:cpf:{cpf}` | `INCR` → `WATCH`/`GET` → `MULTI` `SET` `SET` `EXEC` |
| buscar por id | `cliente:{id}` | `GET` |
| buscar por CPF | `idx:cliente:cpf:{cpf}`, `cliente:{id}` | `GET` + `GET` |
| atualizar | `cliente:{id}`, índice novo e antigo | `WATCH`/`GET` → `MULTI` `SET` `SET` `DEL` `EXEC` |
| remover | `cliente:{id}`, `idx:cliente:cpf:{cpf}` | `GET` → `MULTI` `DEL` `DEL` `EXEC` |
| listar | `seq:cliente`, `cliente:1..N` | `GET` + N × `GET` |

O que cada peça resolve:

- **`INCR seq:cliente`** — id atômico. Se a inserção falhar depois, o número é perdido
  (id com buraco); isso é normal, o que não pode é repetir.
- **`idx:cliente:cpf:{cpf}`** — é a constraint UNIQUE. O valor é o id do dono do CPF;
  quem garante a unicidade é o `WATCH` + a ausência da chave.
- **`WATCH` + `MULTI/EXEC`** — o `WATCH` marca a chave do CPF; se ela mudar antes do
  `EXEC`, o `EXEC` devolve `null` e o código tenta de novo. Sem isso, dois cadastros
  simultâneos com o mesmo CPF passariam.
- **Atualizar CPF** — na mesma transação: grava o cliente, cria o índice novo e apaga o
  antigo. Se esquecer o `DEL`, o CPF antigo fica reservado para sempre.
- **Sem JOIN** — buscar por CPF são duas leituras (índice → id → JSON).
- **Listar** — percorre os ids de 1 até o `seq`; ids removidos devolvem `GET null` e são
  pulados. Se a base crescer, o próximo passo é um `SET idx:cliente:todos` (não criado
  para não mudar o modelo de chaves combinado).
- **`MSET` não é usado em lugar nenhum**: no Garnet ele não é atômico, então chaves
  relacionadas sempre vão dentro de `MULTI/EXEC`.

## Persistência do Garnet

O container sobe com estas flags (ver `docker-compose.yml`):

| Flag | Para que serve |
| --- | --- |
| `--checkpointdir /data` + volume nomeado `garnet-data` | Os dados ficam no volume, não na camada efêmera do container. `down` preserva; `down -v` apaga. |
| `--recover` | Relê checkpoint + AOF no startup. **Sem essa flag o servidor sobe vazio**, mesmo com arquivos no disco. |
| `--aof` | Append-only file: registra cada escrita em disco. |
| `--aof-commit-freq 0` + `--aof-commit-wait` | Commita a cada operação e só responde ao cliente depois do flush. Custo medido: ~10 ms por escrita. Com `freq 1000` + `wait` seriam ~1 s por escrita. |
| `--checkpoint-freq 300` | Checkpoint a cada 5 minutos (full, não delta) e trunca o AOF. Sem checkpoint o AOF cresce sem limite. |

Detalhes que costumam morder:

- **Checkpoint é full.** Cada checkpoint reescreve a parte do log que ainda está em memória;
  para um dataset que cabe na RAM, é praticamente o dataset inteiro. O disco usado cresce
  com `tamanho dos dados / intervalo`.
- **AOF não substitui checkpoint.** Com AOF ligado, um checkpoint concluído trunca o AOF.
  Se você desligar `--checkpoint-freq`, o AOF cresce para sempre.
- **Use volume nomeado, não bind mount.** O processo roda como uid 1654 (`app`). Um bind
  mount de `./data` pertence ao seu usuário e o AOF falha com
  `open('/data/AOF/aof.log.0') failed: 2 (No such file or directory)`.
- **Não abra os arquivos de dados.** O formato é binário interno do Tsavorite; só o servidor
  sabe lê-lo. Para inspecionar, use comandos RESP.
- A imagem não inclui `redis-cli`. Além disso, o Garnet **não aceita comando inline com
  argumentos** (`SET a b` fica sem resposta); só array RESP. Para testar na mão, use o
  heredoc abaixo (o `$N` antes de cada valor é o tamanho em bytes):

  ```bash
  # grava teste=123 (dentro do container, sem depender da porta publicada)
  docker compose exec -T garnet bash <<'EOF'
  exec 3<>/dev/tcp/127.0.0.1/6379
  printf '*3\r\n$3\r\nSET\r\n$5\r\nteste\r\n$3\r\n123\r\n' >&3
  head -c 5 <&3; echo
  EOF

  # reinicia e lê de volta
  docker compose restart garnet && sleep 6
  docker compose exec -T garnet bash <<'EOF'
  exec 3<>/dev/tcp/127.0.0.1/6379
  printf '*2\r\n$3\r\nGET\r\n$5\r\nteste\r\n' >&3
  head -c 9 <&3; echo
  EOF
  ```

  A segunda leitura deve devolver `$3` + `123`.

## Pendências

- Índice `idx:pagamento:data` (ZSET) para extrato ordenado: ainda não decidido.
- Nome do repositório/artefato: o remote já é `Locadora-Aplicacao`; o `artifactId` segue
  esse nome. Se o projeto for renomeado para `locadora-garnet`, é só trocar aqui e no `pom.xml`.
