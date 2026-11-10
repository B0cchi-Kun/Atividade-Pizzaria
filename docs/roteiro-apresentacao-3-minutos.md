# Roteiro da apresentação do dashboard

Duração planejada: 2 minutos e 50 segundos. Grave a tela com a aplicação já aberta, autenticada como GERENTE, MongoDB em execução e pedidos reais de teste no período escolhido. Não mostre senhas nem credenciais de banco.

## 0:00 a 0:20 — Problema e objetivo

“Esta é a evolução do Pediu Patrão para apoiar as decisões do responsável pela pizzaria. O dashboard reúne indicadores de venda, andamento dos pedidos e produtos mais vendidos.”

## 0:20 a 0:40 — Acesso e filtro

“Só ADMIN e GERENTE acessam este painel. Vou selecionar o intervalo consultado e escolher a visualização por dia. O mesmo filtro é aplicado aos indicadores, à série de vendas e ao ranking.”

Mostre os campos de data e o botão Atualizar painel.

## 0:40 a 1:10 — Indicadores

“Os indicadores mostram o faturamento líquido, o total de pedidos, a quantidade não cancelada, o ticket médio e os cancelamentos. Pedidos cancelados ficam fora da receita, mas aparecem no indicador e na distribuição por status.”

Mostre os cartões e destaque valores da base de demonstração.

## 1:10 a 1:40 — Gráficos

“O gráfico compara faturamento e quantidade de pedidos ao longo do tempo. Dias sem venda aparecem zerados. Ao trocar para mês, o agrupamento muda. O gráfico ao lado mostra o volume em cada etapa do fluxo.”

Demonstre a mudança de agrupamento e os dois gráficos.

## 1:40 a 2:00 — Produtos mais vendidos

“O ranking é ordenado pelas unidades vendidas e mostra a receita gerada por produto. Itens de pedidos cancelados não entram no ranking.”

Aponte os primeiros itens da tabela.

## 2:00 a 2:30 — API e segurança

“A interface consome três endpoints: resumo, vendas e produtos mais vendidos. Todos usam o mesmo período e consultam pedidos do MongoDB. As rotas da API exigem ADMIN ou GERENTE; para outros perfis, o servidor responde acesso negado.”

Mostre rapidamente a documentação dos endpoints e, se possível, uma resposta JSON sem exibir dados pessoais.

## 2:30 a 2:50 — Decisões e encerramento

“Centralizei os cálculos em um serviço, usei BigDecimal para valores monetários, intervalo de datas com fuso de São Paulo e atualização automática a cada minuto. Os seis testes automatizados passaram. Assim, o painel complementa o fluxo de pedidos já existente.”

## Preparação antes de gravar

- Inicie MongoDB e a aplicação com as variáveis definidas no README.
- Entre como GERENTE e abra /dashboard.
- Deixe pedidos em mais de um status, pelo menos um cancelamento e produtos distintos no período.
- Faça uma gravação de ensaio para manter a demonstração abaixo de três minutos.
- Mostre a tela e a documentação; evite abrir arquivos de configuração com credenciais.
