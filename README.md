# Serviço Auditor DLQ

Serviço independente responsável por consumir mensagens da Dead Letter Queue (DLQ) e persistir os detalhes em um banco de dados de auditoria com triagem de severidade.

---

## Decisão Arquitetural: Layered Architecture (Arquitetura em Camadas)

### Por que não Hexagonal?

No projeto principal (`t02n-arquitetura-hexagonal`), a escolha pela Arquitetura Hexagonal faz todo sentido: há múltiplos domínios ricos (Pedido, Produto, Pessoa, Estoque), diversas regras de negócio complexas, múltiplos adaptadores de entrada (REST + SQS) e múltiplos adaptadores de saída (H2 + SQS publisher). A Hexagonal permite isolar o domínio de qualquer detalhe de infraestrutura e trocar adaptadores sem impacto no core.

Este serviço é diferente. Ele tem **uma única responsabilidade**: capturar mensagens de erro da DLQ, classificar sua severidade e persisti-las. Não há operações de leitura por REST, não há publicação em outras filas, não há domínio rico. Aplicar Hexagonal completo aqui seria **over-engineering** — criaria ports e adapters para um fluxo que é intrinsecamente linear e sem variação.

### Por que Layered Architecture?

A **Arquitetura em Camadas** (ou Layered Architecture) é o padrão mais adequado para este serviço pelos seguintes motivos:

#### 1. Fluxo unidirecional e simples
O fluxo de dados deste serviço é sempre o mesmo:

```
[SQS DLQ] → [Listener] → [Service] → [Repository] → [H2 Database]
```

Não há ramificações, múltiplas fontes de entrada ou múltiplos destinos de saída. Uma arquitetura em camadas representa esse fluxo de forma direta e legível.

#### 2. Responsabilidade única por camada
Cada camada tem uma responsabilidade clara e isolada:

| Camada | Pacote | Responsabilidade |
|--------|--------|-----------------|
| **Listener** | `listener/` | Adaptador de entrada: consome mensagens do SQS, desserializa o payload e controla o acknowledgement manual |
| **Service** | `service/` | Regras de negócio: triagem de severidade com base no total de itens do pedido |
| **Repository** | `repository/` | Persistência: abstração da camada de dados via Spring Data JPA |
| **Model** | `model/` | Entidades e enums do domínio de auditoria |
| **DTO** | `dto/` | Objetos de transferência que representam a mensagem que chega da DLQ |

#### 3. Regra de dependência entre camadas
As camadas seguem a regra de dependência clássica: camadas superiores dependem de camadas inferiores, nunca o contrário.

```
Listener → Service → Repository → Model
```

O `DlqListener` conhece o `AuditService`, mas o `AuditService` não sabe nada sobre SQS. O `AuditService` conhece o `AuditErrorRepository`, mas o repositório não sabe nada sobre regras de negócio. Isso garante coesão e facilita testes unitários de cada camada de forma isolada.

#### 4. Serviço de "apoio" não justifica abstração de ports
A Hexagonal exige que o domínio exponha **ports** (interfaces) para que adaptadores externos se conectem. Isso é valioso quando você precisa trocar o broker de mensagens (de SQS para RabbitMQ, por exemplo) sem tocar no domínio. Neste serviço de auditoria, tal troca é altamente improvável — o serviço existe especificamente para monitorar esta DLQ desta aplicação. Criar interfaces abstratas adicionaria complexidade sem benefício real.

#### 5. Facilidade de manutenção e leitura
Um desenvolvedor que abre este projeto pela primeira vez consegue entender o fluxo completo em minutos:
- Olha para `listener/`: "aqui é onde a mensagem chega"
- Olha para `service/`: "aqui é onde a lógica de negócio vive"
- Olha para `repository/`: "aqui é onde os dados são salvos"

Não há necessidade de entender o conceito de ports e adapters para contribuir com este serviço.

---

## Regra de Negócio: Triagem de Severidade

A camada de serviço (`AuditService`) implementa a triagem de severidade com base na quantidade total de itens do pedido que falhou:

| Total de Itens | Severidade |
|---------------|------------|
| > 100 | `HIGH` |
| >= 50 e <= 100 | `MEDIUM` |
| < 50 | `LOW` |

A lógica está encapsulada no método privado `classificarSeveridade(int totalItens)` dentro do `AuditService`, separada da lógica de persistência e do consumo de mensagens.

---

## Contrato do Banco de Dados

A tabela `audit_errors` armazena os seguintes campos:

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `error_id` | UUID (String) | Identificador único gerado pelo serviço |
| `queue_name` | String | Nome da fila DLQ de origem |
| `payload` | TEXT | Conteúdo bruto da mensagem em formato JSON |
| `timestamp` | Instant | Momento em que o erro foi registrado |
| `status` | Enum | Sempre `PENDING_ANALYSIS` nesta versão |
| `severity` | Enum | `HIGH`, `MEDIUM` ou `LOW` |

---

## Garantia de Entrega

O listener usa `acknowledgementMode = "MANUAL"`. Isso significa que:

1. A mensagem **não é removida** da DLQ automaticamente ao ser recebida.
2. O `acknowledge()` só é chamado **após** o salvamento bem-sucedido no banco de dados.
3. Se o salvamento falhar, a mensagem **permanece na DLQ** para reprocessamento.

Isso garante que nenhuma mensagem seja perdida: ou ela está no banco de dados, ou ainda está na fila.

---

## Estrutura do Projeto

```
src/main/java/com/fag/mautc/auditor/
├── ServicoAuditorDlqApplication.java   # Ponto de entrada
├── dto/
│   ├── OrderEventDTO.java              # Estrutura da mensagem da DLQ
│   └── OrderItemDTO.java               # Item do pedido
├── model/
│   ├── AuditError.java                 # Entidade JPA
│   ├── AuditStatus.java                # Enum: PENDING_ANALYSIS
│   └── Severity.java                   # Enum: HIGH, MEDIUM, LOW
├── repository/
│   └── AuditErrorRepository.java       # Spring Data JPA
├── service/
│   └── AuditService.java               # Triagem de severidade + persistência
└── listener/
    └── DlqListener.java                # Consumer SQS com ack manual
```

---

## Configuração

Configure as variáveis de ambiente antes de iniciar:

```bash
ACCESS_KEY=<sua-access-key-aws>
SECRET_KEY=<sua-secret-key-aws>
```

O console H2 estará disponível em: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:auditor_dlq`
- Usuário: `sa`
- Senha: (vazio)

A fila DLQ configurada é: `T02N_MAURILIO_TOMAZELI_COLODA_DLQ.fifo`
