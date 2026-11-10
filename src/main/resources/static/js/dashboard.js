(function () {
    "use strict";

    var dashboard = document.getElementById("dashboard");
    if (!dashboard) return;

    var form = document.getElementById("dashboardFiltros");
    var inicio = document.getElementById("dashboardInicio");
    var fim = document.getElementById("dashboardFim");
    var agrupamento = document.getElementById("dashboardAgrupamento");
    var atualizar = document.getElementById("dashboardAtualizar");
    var erro = document.getElementById("dashboardErro");
    var carregando = document.getElementById("dashboardCarregando");
    var vendasChart = null;
    var statusChart = null;
    var moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
    var inteiro = new Intl.NumberFormat("pt-BR");

    function parametros() {
        var query = new URLSearchParams();
        if (inicio.value) query.set("inicio", inicio.value);
        if (fim.value) query.set("fim", fim.value);
        return query.toString();
    }

    async function buscarJson(url) {
        var response = await fetch(url, { headers: { "Accept": "application/json" } });
        var data = await response.json().catch(function () { return {}; });
        if (!response.ok) throw new Error(data.erro || "Não foi possível consultar os dados.");
        return data;
    }

    function mostrarErro(mensagem) {
        erro.textContent = mensagem;
        erro.classList.remove("d-none");
    }

    function atualizarIndicadores(resumo) {
        document.getElementById("kpiFaturamento").textContent = moeda.format(resumo.faturamentoLiquido || 0);
        document.getElementById("kpiPedidos").textContent = inteiro.format(resumo.totalPedidos || 0);
        document.getElementById("kpiPedidosAtivos").textContent =
            inteiro.format(resumo.pedidosNaoCancelados || 0) + " não cancelado(s) · "
            + inteiro.format(resumo.pedidosConcluidos || 0) + " finalizado(s)";
        document.getElementById("kpiTicket").textContent = moeda.format(resumo.ticketMedio || 0);
        document.getElementById("kpiCancelados").textContent = inteiro.format(resumo.pedidosCancelados || 0);
    }

    function rotuloPeriodo(periodo, tipo) {
        if (tipo === "MES") {
            var partes = periodo.split("-");
            return partes[1] + "/" + partes[0].slice(2);
        }
        var data = new Date(periodo + "T12:00:00");
        return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(data);
    }

    function desenharVendas(dados) {
        if (typeof Chart === "undefined") {
            mostrarErro("A biblioteca de gráficos não foi carregada. Atualize a página.");
            return;
        }
        var labels = dados.serie.map(function (item) { return rotuloPeriodo(item.periodo, dados.agrupamento); });
        var receita = dados.serie.map(function (item) { return Number(item.faturamento || 0); });
        var quantidade = dados.serie.map(function (item) { return Number(item.pedidos || 0); });
        if (vendasChart) vendasChart.destroy();
        vendasChart = new Chart(document.getElementById("dashboardVendas").getContext("2d"), {
            type: "bar",
            data: {
                labels: labels,
                datasets: [
                    { label: "Faturamento", data: receita, backgroundColor: "rgba(196, 61, 43, .72)", borderColor: "#c43d2b", borderWidth: 1, yAxisID: "eixoValor" },
                    { label: "Pedidos", data: quantidade, type: "line", borderColor: "#d99a2b", backgroundColor: "rgba(217,154,43,.12)", pointBackgroundColor: "#d99a2b", fill: false, lineTension: .25, yAxisID: "eixoPedidos" }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                legend: { position: "bottom" },
                tooltips: {
                    mode: "index",
                    intersect: false,
                    callbacks: {
                        label: function (item, data) {
                            var nome = data.datasets[item.datasetIndex].label;
                            return nome === "Faturamento" ? nome + ": " + moeda.format(item.yLabel) : nome + ": " + inteiro.format(item.yLabel);
                        }
                    }
                },
                scales: {
                    xAxes: [{ gridLines: { display: false }, ticks: { autoSkip: true, maxTicksLimit: 12, maxRotation: 0 } }],
                    yAxes: [
                        { id: "eixoValor", position: "left", ticks: { beginAtZero: true, callback: function (v) { return moeda.format(v); } } },
                        { id: "eixoPedidos", position: "right", ticks: { beginAtZero: true, precision: 0 }, gridLines: { drawOnChartArea: false } }
                    ]
                }
            }
        });
    }

    function desenharStatus(dados) {
        if (typeof Chart === "undefined") return;
        var estados = (dados.pedidosPorStatus || []).filter(function (item) { return item.quantidade > 0; });
        var semDados = estados.length === 0;
        var labels = semDados ? ["Sem pedidos"] : estados.map(function (item) { return item.descricao; });
        var valores = semDados ? [1] : estados.map(function (item) { return item.quantidade; });
        var paleta = ["#c43d2b", "#ed8b35", "#f0be4d", "#58a36b", "#3287a8", "#5864a6", "#9b6bb3", "#9ba3ad"];
        if (statusChart) statusChart.destroy();
        statusChart = new Chart(document.getElementById("dashboardStatus").getContext("2d"), {
            type: "doughnut",
            data: { labels: labels, datasets: [{ data: valores, backgroundColor: semDados ? ["#e5e7eb"] : paleta, borderWidth: 2, borderColor: "#ffffff" }] },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutoutPercentage: 66,
                legend: { position: "bottom" },
                tooltips: { enabled: !semDados, callbacks: { label: function (item, data) {
                    return data.labels[item.index] + ": " + inteiro.format(data.datasets[0].data[item.index]);
                } } }
            }
        });
    }

    function desenharProdutos(dados) {
        var tbody = document.getElementById("dashboardProdutos");
        tbody.textContent = "";
        if (!dados.produtos || dados.produtos.length === 0) {
            var vazio = document.createElement("tr");
            var celula = document.createElement("td");
            celula.colSpan = 4;
            celula.className = "text-center text-muted py-4";
            celula.textContent = "Nenhuma venda de produto no período selecionado.";
            vazio.appendChild(celula);
            tbody.appendChild(vazio);
            return;
        }
        dados.produtos.forEach(function (produto, index) {
            var linha = document.createElement("tr");
            var posicao = document.createElement("td");
            var nome = document.createElement("td");
            var unidades = document.createElement("td");
            var receita = document.createElement("td");
            posicao.className = "ps-4 text-muted";
            nome.className = "fw-semibold";
            unidades.className = "text-end";
            receita.className = "text-end pe-4";
            posicao.textContent = String(index + 1).padStart(2, "0");
            nome.textContent = produto.produto;
            unidades.textContent = inteiro.format(produto.quantidadeVendida);
            receita.textContent = moeda.format(produto.receitaGerada || 0);
            linha.append(posicao, nome, unidades, receita);
            tbody.appendChild(linha);
        });
    }

    async function carregar() {
        erro.classList.add("d-none");
        if (!inicio.value || !fim.value || inicio.value > fim.value) {
            mostrarErro("Informe um intervalo válido: a data inicial deve ser anterior ou igual à final.");
            return;
        }
        atualizar.disabled = true;
        carregando.classList.remove("d-none");
        try {
            var query = parametros();
            var resultados = await Promise.all([
                buscarJson("/api/dashboard/resumo?" + query),
                buscarJson("/api/dashboard/vendas?" + query + "&agrupamento=" + encodeURIComponent(agrupamento.value)),
                buscarJson("/api/dashboard/produtos-mais-vendidos?" + query + "&limite=5")
            ]);
            atualizarIndicadores(resultados[0]);
            desenharVendas(resultados[1]);
            desenharStatus(resultados[0]);
            desenharProdutos(resultados[2]);
            document.getElementById("dashboardAtualizadoEm").textContent =
                "Atualizado às " + new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date());
        } catch (exception) {
            mostrarErro(exception.message || "Ocorreu um erro ao carregar o dashboard.");
        } finally {
            atualizar.disabled = false;
            carregando.classList.add("d-none");
        }
    }

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        carregar();
    });
    carregar();
    window.setInterval(carregar, 60000);
})();
