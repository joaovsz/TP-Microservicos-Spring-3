# TP3: Autenticação e Autorização em Microsserviços

Trabalho prático focado em resolver autenticação e controle de acesso em arquitetura de microsserviços usando Spring Boot 3 e tokens JWT (JSON Web Tokens).

A ideia central do projeto foi separar completamente a responsabilidade de quem autentica e emite tokens (`auth-service`) de quem consome e valida as requisições de negócio (`order-service`), mantendo a comunicação descentralizada e sem estado (stateless).

## 1. Visão Geral da Arquitetura

O ecossistema é formado por dois serviços conteinerizados rodando em uma mesma bridge network no Docker:

![Diagrama de sequência do fluxo de autenticação](./screenshots/00-diagrama-sequencia-fluxo-auth.png)

### Divisão de Responsabilidades

- **`auth-service` (Porta 8081)**: Cuida exclusivamente de credenciais e ciclo de vida dos tokens. É onde o usuário bate para fazer login e renovar a sessão. Ele emite dois tipos de token:
  - **Access Token**: Curto (15 minutos), usado diretamente no cabeçalho `Authorization: Bearer <token>` para consumir as APIs.
  - **Refresh Token**: Longo (24 horas), guardado para renovar o acesso sem forçar o usuário a digitar a senha novamente.
- **`order-service` (Porta 8082)**: Cuida das regras de negócio e catálogo de pedidos. Não consulta banco de usuários nem chama o `auth-service` a cada requisição; ele intercepta a requisição via `JwtAuthenticationFilter`, confere se a assinatura do token bate com a chave secreta compartilhada e libera ou barra a rota na hora.

## 2. Por que JWT em vez de Keycloak?

A especificação do trabalho permitia escolher entre JWT direto ou Keycloak. A opção adotada foi implementar com **JWT puro via Spring Security 6 e JJWT (0.12.6)**.

O motivo principal é consumo de recursos: o Keycloak precisa de um banco de dados próprio (como PostgreSQL) e consome facilmente mais de 1 GB de RAM só para subir, o que pesa bastante em máquinas de desenvolvimento e contêineres locais. Já os nossos dois microsserviços juntos rodam com menos de 200 MB de RAM em contêineres Alpine JRE.

Também pesou a arquitetura desacoplada: com o JWT assinado simetricamente (HMAC-SHA), o `order-service` valida autenticidade e permissões de forma 100% autônoma na memória da JVM, sem latência extra de rede para checar sessão em servidor central. E dá controle fino sobre as regras: conseguimos definir claims específicos (`token_type: ACCESS` vs `REFRESH`) para evitar que tokens de renovação sejam usados indevidamente para acessar recursos de dados.

## 3. Usuários para Teste

Os usuários já sobem cadastrados em memória, com senhas criptografadas via **BCrypt**:

| Usuário | Senha | Perfil | Descrição |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | `ROLE_ADMIN` | Acesso administrativo completo |
| `user` | `user123` | `ROLE_USER` | Usuário padrão para criação de pedidos |

## 4. Endpoints Disponíveis

### 4.1. `auth-service` (Porta 8081)

| Método | Rota | Requer Autenticação | Objetivo | Status Sucesso |
| :--- | :--- | :---: | :--- | :---: |
| `POST` | `/auth/login` | Não | Valida usuário/senha e retorna os tokens | `200 OK` |
| `POST` | `/auth/refresh` | Não | Recebe refresh token e gera novo access token | `200 OK` |
| `GET` | `/auth/status` | Não | Healthcheck do serviço de autenticação | `200 OK` |

### 4.2. `order-service` (Porta 8082)

| Método | Rota | Requer Autenticação | Objetivo | Status Sucesso |
| :--- | :--- | :---: | :--- | :---: |
| `GET` | `/api/public/info` | **Não** | Informações públicas e status da API | `200 OK` |
| `GET` | `/api/orders` | **Sim (Bearer)** | Lista os pedidos cadastrados | `200 OK` |
| `GET` | `/api/orders/{id}` | **Sim (Bearer)** | Consulta detalhes de um pedido por ID | `200 OK` |
| `POST` | `/api/orders` | **Sim (Bearer)** | Cria um novo pedido atribuído ao usuário logado | `201 Created` |

## 5. Autenticação

### 5.1. Fazendo Login

Todo o fluxo começa em `POST /auth/login`, mandando usuário e senha no corpo da requisição. Se a senha estiver errada, a resposta é direta:

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "senha_incorreta"}'
```

```json
{
  "timestamp": "2026-09-07T16:57:18.596656250Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciais inválidas"
}
```

Com a senha certa, vem o par de tokens:

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwi...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwi...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

`expiresIn: 900` é em segundos, então o `accessToken` vale por 15 minutos. Ele vai no header de qualquer chamada protegida:

```bash
curl http://localhost:8082/api/orders \
  -H "Authorization: Bearer <accessToken>"
```

Sem esse header (ou com token expirado), o `order-service` devolve `401 Unauthorized` antes mesmo de tocar na regra de negócio, o `JwtAuthenticationFilter` barra na entrada.

### 5.2. Renovando o Token (Refresh)

Passados os 15 minutos, o `accessToken` expira e o cliente não precisa pedir login de novo, só usar o `refreshToken` (válido por 24h):

```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken recebido no login>"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwi...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwi...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Repara que o endpoint devolve os dois tokens de novo, não só o access. O `auth-service` confere se o token enviado tem o claim `token_type: REFRESH` antes de emitir um novo par; se alguém tentar usar um access token nesse endpoint, cai fora.

## 6. Como Subir o Projeto

### Opção 1: Via Docker Compose (Mais fácil)

Basta rodar na raiz do projeto:

```bash
# Build das imagens e inicialização dos contêineres
docker compose up -d --build

# Ver se os serviços estão de pé e quais portas estão mapeadas
docker compose ps

# Ver os logs subindo em tempo real
docker compose logs -f

# Para derrubar quando terminar
docker compose down
```

### Opção 2: Direto pelo Maven (Local)

Se quiser rodar fora do Docker, abra dois terminais:

```bash
# Terminal 1: auth-service (Porta 8081)
cd auth-service
mvn spring-boot:run

# Terminal 2: order-service (Porta 8082)
cd order-service
mvn spring-boot:run
```

## 7. Exemplos de Requisições (Testando na Prática)

Prints reais do fluxo completo, do `docker ps` até a criação de um pedido, estão em [`screenshots/`](./screenshots). Alguns destaques:

**Endpoint público, sem token** (`GET /api/public/info`):

```json
{
  "service": "order-service",
  "status": "ONLINE",
  "timestamp": "2026-09-07T16:59:29.026070638Z",
  "description": "API pública de informações do microsserviço de pedidos",
  "authRequirements": "Rotas protegidas em /api/orders/** exigem autenticação via cabeçalho Authorization: Bearer <token>"
}
```

**Listando pedidos com o Bearer token** (`GET /api/orders`):

```json
[
  { "id": 1, "customer": "admin", "item": "MacBook Pro M3 Max", "quantity": 1, "totalPrice": 19999.0, "status": "CONFIRMED" },
  { "id": 2, "customer": "user", "item": "Monitor Dell UltraSharp 32 4K", "quantity": 2, "totalPrice": 7200.0, "status": "CONFIRMED" },
  { "id": 3, "customer": "admin", "item": "Kit Manutenção Aviônica Garmin G1000", "quantity": 1, "totalPrice": 14500.0, "status": "CREATED" }
]
```

**Criando um pedido** (`POST /api/orders`):

```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"item": "Kit Manutenção Aviônica Garmin G1000", "quantity": 1, "totalPrice": 14500.00}'
```

Resposta `201 Created`, com o `customer` já preenchido a partir do token, sem precisar mandar isso no corpo:

```json
{ "id": 4, "customer": "admin", "item": "Kit Manutenção Aviônica Garmin G1000", "quantity": 1, "totalPrice": 14500.0, "status": "CREATED" }
```

Vale conferir também o print `02-orders-sem-token-401.png`: bater em `/api/orders` sem header nenhum devolve `401` com a mensagem "Acesso não autorizado: token JWT ausente, expirado ou inválido", antes de qualquer lógica de pedido rodar.

## 8. Coleção de Requisições (Bruno)

A coleção de requisições para validação dos endpoints e fluxo de renovação de tokens está organizada na pasta [`bruno/`](./bruno), pronta para uso com o ambiente `local`.
