Módulo: Frente de Caixa (PDV)

**Funcionalidades:**

- Abertura e fechamento de caixa (com valor inicial)
- Busca de produto por código de barras, nome ou código interno
- Adição / remoção de itens na venda
- Aplicação de desconto por item ou por venda (valor fixo ou %)
- Múltiplas formas de pagamento na mesma venda:
  - 💵 Dinheiro (com cálculo de troco)
  - 💳 Cartão Débito / Crédito (integração com TEF ou registro manual)
  - 📱 PIX (geração de QR Code ou chave Pix) 
  - 📒 Fiado (vinculado ao cadastro do cliente)
- Finalização e impressão do cupom
- Modo rápido (sem identificar cliente)

**Regras de Negócio:**

- Não permitir venda se estoque insuficiente (configurável por tenant)
- Caixa deve estar aberto para realizar vendas
- Fiado exige cliente selecionado e limite de crédito respeitado

---

### 4.2 Módulo: Cancelamento / Estorno

- Cancelar venda inteira (dentro do mesmo dia / caixa aberto)
- Cancelar item da venda (parcial)
- Estornar pagamento (registra estorno, não processa no gateway automaticamente)
- Motivo obrigatório para cancelamento
- Auditoria completa: quem cancelou, quando, motivo
- Repõe estoque automaticamente ao cancelar

---

### 4.3 Módulo: Produto

- Cadastro completo: nome, descrição, código de barras, SKU, NCM
- Categorias e subcategorias
- Preço de custo e preço de venda
- Margem de lucro calculada automaticamente
- Suporte a múltiplas unidades (UN, KG, CX, PCT...)
- Foto do produto
- Produto ativo / inativo
- Produto composto (kits)
- Histórico de alterações de preço

---

### 4.4 Módulo: Estoque

- **Entrada:** via lançamento de compra (vinculado ao fornecedor)
- **Saída:** baixa automática ao vender
- **Ajuste Manual:** entrada/saída avulsa com justificativa
- **Estoque Mínimo:** alerta visual e notificação quando atingido
- **Inventário:** contagem periódica com ajuste de divergências
- Rastreabilidade de movimentações (quem, quando, quantidade, motivo)
- Relatório de giro de estoque

---

### 4.5 Módulo: Cliente

- Cadastro completo: nome, CPF/CNPJ, e-mail, telefone, endereço
- Limite de crédito (para fiado)
- Histórico de compras
- Saldo devedor (fiado em aberto)
- Classificação / tags
- Aniversário (CRM básico)

---

### 4.6 Módulo: Fornecedor / Compras

- Cadastro de fornecedor: razão social, CNPJ, contato, endereço
- Lançamento de compras com múltiplos itens
- Atualiza estoque automaticamente ao confirmar compra
- Atualiza preço de custo do produto (configurável)
- Status da compra: Pedido → Recebido → Cancelado
- Contas a pagar geradas automaticamente pela compra

---

### 4.7 Módulo: Caixa

- Abertura com valor inicial em dinheiro
- Sangria (retirada de dinheiro)
- Suprimento (reforço de dinheiro)
- Fechamento com conferência de valores por forma de pagamento
- Histórico de movimentações do caixa
- Relatório de fechamento de caixa (Z)

---

### 4.8 Módulo: Pagamento / Financeiro

- Contas a receber (fiado)
- Contas a pagar (fornecedores)
- Registro de recebimento de fiado
- Extrato financeiro por período
- Fluxo de caixa

---

### 4.9 Módulo: Usuários / Permissões

- Papéis (Roles): `ADMIN`, `GERENTE`, `OPERADOR`, `FINANCEIRO`
- Permissões granulares por módulo e operação
- Controle de quem pode: cancelar venda, aplicar desconto acima de X%, ver relatórios...
- Log de ações (auditoria)
- Autenticação: JWT quando online, token local assinado quando offline

---

### 4.10 Módulo: Relatórios

| Relatório               | Filtros disponíveis                        |
|-------------------------|--------------------------------------------|
| Vendas por período      | Data, operador, forma de pagamento, PDV    |
| Produtos mais vendidos  | Período, categoria                         |
| Curva ABC de produtos   | Período                                    |
| Extrato de caixa        | PDV, período                               |
| Estoque atual           | Categoria, abaixo do mínimo                |
| Fiado em aberto         | Cliente, vencimento                        |
| Margem de lucro         | Produto, categoria, período                |
| Compras por fornecedor  | Período, fornecedor                        |

---

### 4.11 Módulo: Dashboard

Visão geral em tempo real (ou ao sincronizar):

- 💰 Faturamento do dia / semana / mês
- 🧾 Número de vendas e ticket médio
- 📦 Produtos com estoque crítico
- 👥 Ranking de produtos mais vendidos
- 💳 Breakdown por forma de pagamento
- 📉 Comparativo período anterior
- ⚠️ Alertas (estoque mínimo, fiado vencido, caixa não fechado)

6. Fluxo Offline-First e Sincronização

### Estados de Conectividade

```
[ONLINE]  → Operações diretas na API + IndexedDB local espelhado
[OFFLINE] → Operações apenas no IndexedDB, flag pendingSync = true
[SYNC]    → Ao reconectar: processa FilaSincronizacao em ordem FIFO
```

### Estratégia de Sincronização

1. **Detecção:** `navigator.onLine` + heartbeat para a API
2. **Fila de Operações:** cada mutação offline gera um registro em `FilaSincronizacao`
3. **Replay:** ao reconectar, o `SyncService` envia as operações para a API
4. **Conflito:** comparação por `updated_at` + versão incremental (`version: number`)
5. **Feedback:** indicador visual no PDV (🟢 Online / 🔴 Offline / 🔄 Sincronizando)

### Dados sempre disponíveis offline

- Produtos e preços (sincronizados ao abrir o app)
- Clientes (cache local)
- Configurações do tenant/PDV
- Histórico de vendas recentes (últimos N dias)

---

## 7. Multi PDV

- Cada PDV tem seu próprio `device_id` único
- Vendas geradas offline usam UUID para evitar conflito de IDs
- Número de venda: `[TENANT]-[PDV]-[SEQ]` (ex: `LOJA01-CX01-000123`)
- Sincronização via WebSocket quando online (atualização em tempo real)
- Controle central: gerente vê todos os caixas abertos em tempo real
- Permissão para transferir venda entre PDVs (gerente)

---

## 8. Segurança e Permissões

### Modo Online

- Autenticação via JWT (Access Token + Refresh Token)
- Token armazenado em memória (não localStorage) + cookie HttpOnly para refresh
- Expiração curta (1 hora) com refresh automático

### Modo Offline

- Token local assinado com chave privada do dispositivo
- Permissões cacheadas localmente na última sincronização
- Operações críticas (cancelamento, desconto acima do limite) requerem PIN do supervisor

### Papéis e Permissões

| Ação                         | OPERADOR | GERENTE | ADMIN |
|------------------------------|----------|---------|-------|
| Realizar venda               | ✅        | ✅       | ✅     |
| Cancelar venda               | ❌        | ✅       | ✅     |
| Aplicar desconto > limite    | ❌        | ✅       | ✅     |
| Ver relatórios               | ❌        | ✅       | ✅     |
| Gerenciar produtos/estoque   | ❌        | ✅       | ✅     |
| Gerenciar usuários           | ❌        | ❌       | ✅     |
| Configurações do tenant      | ❌        | ❌       | ✅     |

---

## 9. Impressão de Cupom

### Integração com Impressora Térmica

- Protocolo **ESC/POS** (padrão universal para impressoras térmicas)
- No browser: via **Web Serial API** (Chrome/Edge, dispositivo USB/Serial)
- Fallback: impressão via `window.print()` com CSS de impressão específico
- Backend pode gerar PDF do cupom para impressão em rede

### Conteúdo do Cupom Não Fiscal

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━
     NOME DA EMPRESA
  Endereço | CNPJ: XX.XXX...
━━━━━━━━━━━━━━━━━━━━━━━━━━━
 CUPOM NÃO FISCAL
 Venda: LOJA01-CX01-000123
 Data: 16/04/2026 14:30:00
 Operador: Wilson
━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ITEM       QTD  UNIT  TOTAL
 Produto A   2  10,00  20,00
 Produto B   1  15,50  15,50
━━━━━━━━━━━━━━━━━━━━━━━━━━━
 SUBTOTAL          R$ 35,50
 DESCONTO           R$  2,00
 TOTAL             R$ 33,50
━━━━━━━━━━━━━━━━━━━━━━━━━━━
 DINHEIRO          R$ 50,00
 TROCO             R$ 16,50
━━━━━━━━━━━━━━━━━━━━━━━━━━━
   OBRIGADO PELA PREFERÊNCIA!
```

---

## 10. Dashboard e Relatórios

### Dashboard (Widgets Configuráveis)

- Cards de KPIs: faturamento, ticket médio, qtd. vendas, margem
- Gráfico de linha: vendas por hora (dia atual)
- Gráfico de barras: faturamento dos últimos 7/30 dias
- Gráfico de pizza: breakdown por forma de pagamento
- Tabela: top 10 produtos mais vendidos
- Lista: alertas ativos (estoque crítico, caixas sem fechamento)

### Relatórios (Exportação)

- Formato: PDF / CSV / Excel
- Agendamento: relatório automático por e-mail (ex: fechamento diário)

---

## 11. Estrutura de Projetos

```
pdv-saas/
│
├── pdv-backend/                  # Spring Boot API Central
│   ├── src/main/java/com/pdv/
│   │   ├── config/               # Security, CORS, JWT
│   │   ├── modules/
│   │   │   ├── tenant/
│   │   │   ├── auth/
│   │   │   ├── produto/
│   │   │   ├── venda/
│   │   │   ├── estoque/
│   │   │   ├── cliente/
│   │   │   ├── fornecedor/
│   │   │   ├── caixa/
│   │   │   ├── relatorio/
│   │   │   └── sincronizacao/
│   │   └── shared/               # DTOs, exceções, utils
│   ├── 
│   └── pom.xml
│
├── pdv-frontend/                 # Angular 21 PWA
│   ├── src/app/
│   │   ├── core/                 # Auth, interceptors, guards
│   │   ├── shared/               # Componentes reutilizáveis
│   │   ├── features/
│   │   │   ├── pdv/              # Frente de caixa
│   │   │   ├── produto/
│   │   │   ├── estoque/
│   │   │   ├── cliente/
│   │   │   ├── fornecedor/
│   │   │   ├── caixa/
│   │   │   ├── relatorio/
│   │   │   └── dashboard/
│   │   └── offline/              # SyncService, IndexedDB, Dexie
│   ├── ngsw-config.json          # Service Worker config
│   └── package.json
│
└── # PostgreSQL + API + Nginx
```

---

## 12. Roadmap de Desenvolvimento

### 🟢 Fase 1 — MVP (Núcleo do PDV)

- [ ] Setup do projeto (Spring Boot + Angular )
- [ ] Autenticação JWT + Multi-tenant básico
- [ ] CRUD de Produtos e Categorias
- [ ] Frente de Caixa: venda simples com dinheiro
- [ ] Baixa automática de estoque
- [ ] Cupom não fiscal (impressão via browser)
- [ ] PWA básico com Service Worker

### 🟡 Fase 2 — Operacional Completo

- [ ] Todas as formas de pagamento (cartão, PIX, fiado)
- [ ] Módulo de Clientes completo
- [ ] Controle de Caixa (abertura, fechamento, sangria)
- [ ] Cancelamento / Estorno de vendas
- [ ] Módulo de Estoque (ajuste manual, estoque mínimo)
- [ ] Offline-first com IndexedDB + SyncService

### 🔵 Fase 3 — Gestão e Relatórios

- [ ] Fornecedores e Compras
- [ ] Dashboard com gráficos
- [ ] Relatórios exportáveis (PDF / CSV)
- [ ] Módulo Financeiro (contas a pagar/receber)
- [ ] Inventário de Estoque

### 🟣 Fase 4 — Multi PDV e SaaS

- [ ] Multi PDV com sincronização em tempo real (WebSocket)
- [ ] Gestão de Tenants (onboarding, planos)
- [ ] Permissões granulares e auditoria
- [ ] Integração com impressora térmica (ESC/POS via Web Serial)
- [ ] Relatório agendado por e-mail

### ⚫ Fase 5 — Diferenciais Competitivos

- [ ] Integração com TEF (cartão via maquininha)
- [ ] App mobile nativo (Angular + Capacitor)
- [ ] Integração com NF-e / NFC-e (Nota Fiscal)
- [ ] API pública para integrações de terceiros

---

## 13. Melhorias Propostas

As melhorias abaixo foram identificadas na análise e adicionadas ao escopo:

| # | Melhoria                           | Justificativa                                                         |
|---|------------------------------------|-----------------------------------------------------------------------|
| 1 | **UUID para IDs offline**          | Evita conflitos de ID entre PDVs ao sincronizar                       |
| 2 | **Numeração de venda composta**    | `[TENANT]-[PDV]-[SEQ]` garante unicidade e rastreabilidade            |
| 3 | **Versioning de registros**        | Campo `version` em entidades críticas para resolver conflitos de sync |
| 4 | **Modo supervisor (PIN)**          | Ações críticas offline exigem PIN do gerente sem precisar de login    |
| 5 | **Produto composto (kit)**         | Vender kits com baixa automática dos componentes                      |
| 6 | **Curva ABC de produtos**          | Relatório essencial para gestão de estoque inteligente                |
| 7 | **Inventário periódico**           | Contagem física com ajuste de divergências                            |
| 8 | **Contas a pagar/receber**         | Fecha o ciclo financeiro dentro do sistema                            |
| 9 | **Sangria e Suprimento de caixa**  | Operação padrão em PDV real para controle do fundo de caixa           |
| 10| **Agendamento de relatórios**      | Envio automático de fechamento diário por e-mail                      |
| 11| **Web Serial API (ESC/POS)**       | Impressão profissional sem driver em impressoras térmicas USB          |
| 12| **Capacitor (futuro)**             | Transformar o PWA em APK/IPA sem reescrever o código                  |
| 13| **Multi-tenant onboarding**        | Cadastro self-service de novas empresas (SaaS completo)               |
| 14| **Auditoria completa (AuditLog)**  | Rastrear toda ação crítica para compliance e reversão                 |

---
