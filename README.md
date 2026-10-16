# PediuPatrão

Sistema de pizzaria em Java 21, Spring Boot, Spring Security, Thymeleaf e MongoDB. O projeto preserva o cadastro de clientes e produtos e acrescenta perfis de funcionário, fluxo controlado de pedidos, descontos, cancelamentos e auditoria.

## Requisitos

- JDK 21
- MongoDB local ou uma instância MongoDB Atlas

## Configuração

A aplicação lê configurações sensíveis do ambiente. Não grave URI com senha ou credenciais no `application.properties`.

PowerShell, para uma execução local:

```powershell
$env:MONGODB_URI = 'mongodb://localhost:27017/pizzaria'
$env:MONGODB_DATABASE = 'pizzaria'
$env:BOOTSTRAP_ADMIN_USERNAME = 'admin'
$env:BOOTSTRAP_ADMIN_PASSWORD = 'defina-uma-senha-forte'
.\mvnw.cmd spring-boot:run
```

Para MongoDB Atlas, informe a URI no `MONGODB_URI` e mantenha as credenciais fora do repositório. A conta inicial ADMIN é criada quando o usuário configurado ainda não existe. Se ele já existir, a inicialização não troca a senha; ela apenas garante o perfil ADMIN.

Não há cadastro público. Depois do primeiro acesso, o ADMIN cria as contas GERENTE e ATENDENTE na tela Usuários. As senhas são armazenadas com BCrypt.

## Perfis e autorização

| Perfil | Permissões |
| --- | --- |
| ADMIN | Gerenciar contas e perfis; consultar pedidos e auditoria. Não aplica desconto nem cancela pedido. |
| GERENTE | Consultar clientes, gerenciar produtos e pedidos, avançar etapas, aplicar descontos de até 20%, cancelar pedidos antes da saída e consultar auditoria. |
| ATENDENTE | Cadastrar e atualizar clientes, registrar pedidos e acompanhar/avançar seu andamento. |

As permissões estão nas rotas e nos serviços com Spring Security. Os formulários Thymeleaf usam CSRF; as rotas de auditoria são somente leitura e não há endpoints comuns para editar ou apagar eventos.

## Regras de pedidos

- O pedido começa como `RECEBIDO`; a entrada registra data/hora e funcionário.
- Transições permitidas: `RECEBIDO` → `EM_PREPARACAO` → `PRONTO` → `SAIU_PARA_ENTREGA` ou `RETIRADO` → `FINALIZADO`.
- A saída registra data/hora e responsável. Pedidos cancelados não podem ser alterados.
- Só GERENTE aplica desconto percentual de até 20% ou cancela, sempre antes da saída.
- Cancelamento exige justificativa. Descontos, cancelamentos e mudanças de status geram auditoria.
- O pedido recebe produto e quantidade; o servidor busca os preços atuais e calcula subtotal, desconto e total. A requisição não define preço nem total.

## Auditoria

Os eventos são persistidos na coleção MongoDB `auditoria` com usuário, perfil, instante, operação, entidade, identificador, campos anteriores/novos e justificativa quando aplicável. Alterações em telefone e endereço de clientes guardam somente os campos que mudaram. A consulta mostra no máximo os 500 eventos mais recentes e exige ADMIN ou GERENTE.

## Rotas principais

- Interface: `/login`, `/pedidos`, `/clientes`, `/produtos`, `/usuarios`, `/auditoria`
- API de pedidos: `GET/POST /api/pedidos`, `PUT /api/pedidos/{id}/status?status=...`, `POST /api/pedidos/{id}/desconto`, `POST /api/pedidos/{id}/cancelamento`
- API de auditoria: somente `GET /api/auditoria`
- API de usuários: `/api/usuarios/**`, somente ADMIN

As operações POST/PUT/DELETE protegidas por CSRF devem enviar o token CSRF do Spring Security, inclusive em chamadas à API feitas pelo navegador.

## Dados e segurança

`application.properties` usa `MONGODB_URI`, `MONGODB_DATABASE`, `BOOTSTRAP_ADMIN_USERNAME` e `BOOTSTRAP_ADMIN_PASSWORD`. Não use credenciais de demonstração fixas e não envie arquivos com segredos ao GitHub. Se uma URI real já foi compartilhada em um arquivo, troque a senha do usuário do MongoDB e atualize a variável de ambiente.
