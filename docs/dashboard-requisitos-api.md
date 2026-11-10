# Dashboard Administrativo Pediu Patrão

## Contexto e objetivo

Este documento especifica o dashboard administrativo integrado ao Pediu Patrão. O painel resume o desempenho de vendas, a situação dos pedidos e os produtos mais vendidos, usando dados persistidos na coleção MongoDB de pedidos. O público autorizado é o ADMIN e o GERENTE.

O escopo inclui a tela, os endpoints de consulta, as regras de cálculo, os filtros, o tratamento de erros e os testes automatizados executados.

## Arquitetura e decisões

A aplicação mantém a arquitetura existente em Java 21, Spring Boot, Spring Security, Thymeleaf e MongoDB. A tela Thymeleaf consome três endpoints JSON da mesma aplicação, e o Chart.js já distribuído com o projeto desenha os gráficos. A API consulta pedidos pelo instante de entrada; esse campo recebe um índice MongoDB.

O faturamento líquido soma o valor total de pedidos que não foram cancelados. O ticket médio divide esse valor pela quantidade de pedidos não cancelados. A distribuição por situação mostra todos os pedidos, inclusive cancelados, para não esconder o volume de cancelamentos. O ranking mede unidades vendidas e exclui os pedidos cancelados.

As datas são inclusivas e interpretadas no fuso America/Sao_Paulo. A ausência das duas datas consulta os últimos 30 dias, incluindo o dia atual. A série diária aceita até 367 datas; para intervalos maiores, o usuário escolhe a série mensal, que aceita até 61 meses.

## Requisitos funcionais

| ID | Requisito | Critério atendido |
| --- | --- | --- |
| RF01 | Indicadores gerais de vendas | Mostra faturamento líquido, pedidos registrados, pedidos não cancelados, pedidos finalizados, cancelamentos e ticket médio. |
| RF02 | Análise de vendas | Gráfico por dia ou mês compara faturamento e quantidade de pedidos não cancelados. Períodos sem venda aparecem com zero. |
| RF03 | Situação dos pedidos | Gráfico mostra a quantidade por status, incluindo pedidos cancelados e status legados reconhecidos. |
| RF04 | Produtos mais vendidos | Ranking ordenado pela quantidade vendida, com receita por produto; pedidos cancelados são excluídos. |
| RF05 | Filtros de consulta | Permite escolher data inicial, data final e agrupamento diário ou mensal. |
| RF06 | Integração com a API | A página consulta os endpoints JSON autenticados; os endpoints usam o repositório MongoDB de pedidos. |
| RF07 | Atualização e tratamento de dados | Botão atualiza os dados; atualização automática ocorre a cada 60 segundos; erros e estado de carregamento são exibidos na tela. |

## Requisitos não funcionais

| ID | Requisito | Implementação |
| --- | --- | --- |
| RNF01 | Compatibilidade tecnológica | Java 21, Spring Boot 3.4.4, MongoDB, Thymeleaf e Chart.js já fornecido pelo projeto. |
| RNF02 | Controle de acesso | Página e endpoints aceitam somente ADMIN e GERENTE. |
| RNF03 | Desempenho da consulta | Consulta limita pedidos ao intervalo de datas e usa índice em dataHoraEntrada. |
| RNF04 | Consistência monetária | Valores são processados em BigDecimal e apresentados com duas casas decimais. |
| RNF05 | Segurança das respostas | Erros da API não incluem stack trace; respostas de acesso usam 401 ou 403. |
| RNF06 | Usabilidade | Interface responsiva, rótulos em português, feedback de carregamento e erros compreensíveis. |
| RNF07 | Manutenibilidade | Cálculos ficam em DashboardService e possuem testes automatizados independentes do MongoDB. |

## Regras de cálculo

- Total de pedidos é a quantidade de documentos encontrados no intervalo por dataHoraEntrada.
- Pedidos não cancelados inclui qualquer pedido do intervalo cujo status não seja CANCELADO.
- Pedidos concluídos conta somente o status FINALIZADO.
- Faturamento líquido soma valorTotal dos pedidos não cancelados. Cancelamentos não geram faturamento.
- Ticket médio é faturamento líquido dividido pelo total de pedidos não cancelados. Sem pedidos, o ticket é R$ 0,00.
- A distribuição por status inclui todas as etapas do enum StatusPedido. Status persistidos desconhecidos são agrupados como OUTROS.
- O ranking soma a quantidade dos itens. A receita dos itens atuais soma seus subtotais antes dos descontos do pedido. Pedidos legados com sabor ou tipoPizza e quantidade também podem aparecer no ranking.
- Pedidos antigos sem dataHoraEntrada não entram nos relatórios filtrados por data.

## API do dashboard

Base local: http://localhost:8080

Os três endpoints exigem sessão autenticada com perfil ADMIN ou GERENTE. Os parâmetros de data são opcionais e usam o formato ISO yyyy-MM-dd. As datas inicial e final são inclusivas no fuso America/Sao_Paulo. Se omitidas, o padrão é do dia atual menos 29 dias até hoje.

### GET /api/dashboard/resumo

Retorna indicadores e distribuição por status.

Parâmetros:

| Nome | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| inicio | data yyyy-MM-dd | Não | Primeiro dia incluído. |
| fim | data yyyy-MM-dd | Não | Último dia incluído. |

Exemplo de requisição:

~~~http
GET /api/dashboard/resumo?inicio=2026-10-01&fim=2026-10-31
Accept: application/json
~~~

Exemplo ilustrativo de resposta:

~~~json
{
  "inicio": "2026-10-01",
  "fim": "2026-10-31",
  "totalPedidos": 14,
  "pedidosNaoCancelados": 12,
  "pedidosConcluidos": 6,
  "pedidosCancelados": 2,
  "faturamentoLiquido": 840.00,
  "ticketMedio": 70.00,
  "pedidosPorStatus": [
    { "status": "RECEBIDO", "descricao": "Recebido", "quantidade": 1 },
    { "status": "EM_PREPARACAO", "descricao": "Em preparação", "quantidade": 2 },
    { "status": "PRONTO", "descricao": "Pronto", "quantidade": 1 },
    { "status": "SAIU_PARA_ENTREGA", "descricao": "Saiu para entrega", "quantidade": 1 },
    { "status": "RETIRADO", "descricao": "Retirado", "quantidade": 1 },
    { "status": "FINALIZADO", "descricao": "Finalizado", "quantidade": 6 },
    { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 2 }
  ]
}
~~~

### GET /api/dashboard/vendas

Retorna a série usada no gráfico de vendas.

Parâmetros de data: os mesmos de resumo.

| Nome | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| agrupamento | texto | Não | DIA ou MES. Padrão DIA. |

Exemplo:

~~~http
GET /api/dashboard/vendas?inicio=2026-10-01&fim=2026-10-03&agrupamento=DIA
Accept: application/json
~~~

Resposta ilustrativa:

~~~json
{
  "inicio": "2026-10-01",
  "fim": "2026-10-03",
  "agrupamento": "DIA",
  "serie": [
    { "periodo": "2026-10-01", "pedidos": 2, "faturamento": 120.00 },
    { "periodo": "2026-10-02", "pedidos": 0, "faturamento": 0.00 },
    { "periodo": "2026-10-03", "pedidos": 1, "faturamento": 45.00 }
  ]
}
~~~

Os pedidos cancelados não entram em pedidos nem em faturamento da série. A agrupação mensal usa chaves de período yyyy-MM.

### GET /api/dashboard/produtos-mais-vendidos

Retorna os produtos ordenados pela quantidade vendida no intervalo.

Parâmetros de data: os mesmos de resumo.

| Nome | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| limite | inteiro | Não | Quantidade de itens no ranking, de 1 a 20. Padrão 5. |

Exemplo:

~~~http
GET /api/dashboard/produtos-mais-vendidos?inicio=2026-10-01&fim=2026-10-31&limite=5
Accept: application/json
~~~

Resposta ilustrativa:

~~~json
{
  "inicio": "2026-10-01",
  "fim": "2026-10-31",
  "produtos": [
    {
      "produtoId": "produto-123",
      "produto": "Portuguesa",
      "quantidadeVendida": 18,
      "receitaGerada": 1080.00
    }
  ]
}
~~~

## Erros e autorização

| HTTP | Situação | Resposta |
| --- | --- | --- |
| 400 | Intervalo invertido, agrupamento diferente de DIA/MES, limite fora de 1 a 20, intervalo acima do limite do gráfico ou data inválida. | JSON com a propriedade erro, por exemplo { "erro": "A data inicial deve ser anterior ou igual à data final." }. |
| 401 | Requisição à API sem sessão autenticada. | Não autorizado. |
| 403 | Perfil autenticado sem ADMIN ou GERENTE. | JSON { "erro": "Acesso negado para este perfil." }. |
| 500 | Falha inesperada do servidor ou da conexão MongoDB. | Mensagem interna e stack trace não são expostos. |

## Testes e evidências

Os testes automatizados exercitam os cálculos e filtros do serviço com um repositório falso, sem depender de uma instância MongoDB:

1. Cálculo do faturamento e ticket médio excluindo cancelamentos, com verificação do intervalo de consulta e distribuição de status.
2. Séries diária e mensal com períodos sem venda preenchidos e exclusão de pedidos cancelados.
3. Rejeição de datas invertidas antes da consulta ao banco.
4. Ranking de itens atuais e pedidos legados, excluindo itens de pedidos cancelados.

Em 10 de novembro de 2026, os 6 testes passaram, com 0 falhas, 0 erros e 0 ignorados. O Maven retornou BUILD SUCCESS.

Para repetir os testes no Windows, execute o comando abaixo.

~~~powershell
.\mvnw.cmd test
~~~

Na validação integrada com um MongoDB temporário, os três endpoints retornaram os valores esperados. A consulta incluiu o primeiro instante do período e excluiu o primeiro instante após a data final. Os pedidos cancelados apareceram na contagem e ficaram fora do faturamento. A API retornou 400 para datas invertidas, 401 sem sessão e 403 para o perfil ATENDENTE.

## Execução e demonstração

Com MongoDB disponível, configure as variáveis MONGODB_URI e MONGODB_DATABASE conforme o README e execute a aplicação. Acesse http://localhost:8080/dashboard com uma conta ADMIN ou GERENTE. Escolha um período com pedidos existentes, atualize o painel e confira os três gráficos/indicadores.

O vídeo está em docs/Apresentacao_Dashboard.mp4. O roteiro está em docs/roteiro-apresentacao-3-minutos.md.
