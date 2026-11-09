(() => {
    const ESTADOS = ['carregando', 'erro', 'vazio', 'conteudo'];
    const COR_NEUTRA = '#adb5bd';
    const COR_DESTAQUE = '#0d6efd';
    const CORES_STATUS = {
        RECEBIDO: '#0dcaf0',
        EM_PREPARACAO: '#ffc107',
        PRONTO: '#fd7e14',
        SAIU_PARA_ENTREGA: '#6f42c1',
        RETIRADO: '#20c997',
        FINALIZADO: '#198754',
        CANCELADO: '#dc3545'
    };
    const ROTULO_ATENDIMENTO = { ENTREGA: 'Entrega', RETIRADA: 'Retirada' };

    const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const formatoMoedaComSinal = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', signDisplay: 'always' });
    const formatoInteiro = new Intl.NumberFormat('pt-BR');
    const formatoPercentual = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const formatoPercentualComSinal = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: 'always' });

    const raiz = document.getElementById('dashboard');
    const campoInicio = document.getElementById('filtro-inicio');
    const campoFim = document.getElementById('filtro-fim');
    const campoTipo = document.getElementById('filtro-tipo');
    const botaoAtualizar = document.getElementById('botao-atualizar');
    const botoesAtalho = Array.from(raiz.querySelectorAll('[data-atalho]'));
    const seletorProdutos = document.getElementById('produtos-ordenar');
    const alerta = document.getElementById('dashboard-alerta');
    const alertaTexto = document.getElementById('dashboard-alerta-texto');
    const botaoTentar = document.getElementById('dashboard-tentar');
    const erroFiltro = document.getElementById('filtro-erro');
    const atualizadoEm = document.getElementById('atualizado-em');

    let filtroAplicado = { inicio: '', fim: '', tipo: '' };
    let dataHojeServidor = null;
    let cargaEmAndamento = false;
    let blocosComFalha = [];
    const graficos = {};
    const sequencias = {};

    class ErroRequisicao extends Error {
        constructor(status, mensagem) {
            super(mensagem);
            this.status = status;
        }
    }

    function dois(numero) {
        return String(numero).padStart(2, '0');
    }

    function formatarData(iso) {
        if (!iso) {
            return '—';
        }
        const [ano, mes, dia] = iso.slice(0, 10).split('-');
        return `${dia}/${mes}/${ano}`;
    }

    function formatarDataHora(iso) {
        if (!iso) {
            return '—';
        }
        const [data, hora] = iso.split('T');
        const [ano, mes, dia] = data.split('-');
        const [h, m] = (hora || '00:00').split(':');
        return `${dia}/${mes}/${ano} ${h}:${m}`;
    }

    function formatarDataCurta(iso) {
        const [, mes, dia] = iso.slice(0, 10).split('-');
        return `${dia}/${mes}`;
    }

    function diasDesdeEpoca(ano, mes, dia) {
        const a = mes <= 2 ? ano - 1 : ano;
        const era = Math.floor(a / 400);
        const anoDaEra = a - era * 400;
        const diaDoAno = Math.floor((153 * (mes + (mes > 2 ? -3 : 9)) + 2) / 5) + dia - 1;
        const diaDaEra = anoDaEra * 365 + Math.floor(anoDaEra / 4) - Math.floor(anoDaEra / 100) + diaDoAno;
        return era * 146097 + diaDaEra - 719468;
    }

    function dataDeDiasDesdeEpoca(dias) {
        const z = dias + 719468;
        const era = Math.floor(z / 146097);
        const diaDaEra = z - era * 146097;
        const anoDaEra = Math.floor((diaDaEra - Math.floor(diaDaEra / 1460) + Math.floor(diaDaEra / 36524) - Math.floor(diaDaEra / 146096)) / 365);
        const diaDoAno = diaDaEra - (365 * anoDaEra + Math.floor(anoDaEra / 4) - Math.floor(anoDaEra / 100));
        const mp = Math.floor((5 * diaDoAno + 2) / 153);
        const dia = diaDoAno - Math.floor((153 * mp + 2) / 5) + 1;
        const mes = mp < 10 ? mp + 3 : mp - 9;
        const ano = anoDaEra + era * 400 + (mes <= 2 ? 1 : 0);
        return `${ano}-${dois(mes)}-${dois(dia)}`;
    }

    function subtrairDias(iso, quantidade) {
        const [ano, mes, dia] = iso.split('-').map(Number);
        return dataDeDiasDesdeEpoca(diasDesdeEpoca(ano, mes, dia) - quantidade);
    }

    function dataLocalDeHoje() {
        const agora = new Date();
        return `${agora.getFullYear()}-${dois(agora.getMonth() + 1)}-${dois(agora.getDate())}`;
    }

    function horaLocalAtual() {
        const agora = new Date();
        return `${dois(agora.getHours())}:${dois(agora.getMinutes())}:${dois(agora.getSeconds())}`;
    }

    function criar(tag, classe, texto) {
        const elemento = document.createElement(tag);
        if (classe) {
            elemento.className = classe;
        }
        if (texto !== undefined) {
            elemento.textContent = texto;
        }
        return elemento;
    }

    function bloco(nome) {
        return raiz.querySelector(`[data-bloco="${nome}"]`);
    }

    function definirEstado(nome, estado) {
        const alvo = bloco(nome);
        ESTADOS.forEach(e => {
            const parte = alvo.querySelector(`[data-estado="${e}"]`);
            if (parte) {
                parte.classList.toggle('d-none', e !== estado);
            }
        });
    }

    function destruirGrafico(nome) {
        if (graficos[nome]) {
            graficos[nome].destroy();
            delete graficos[nome];
        }
    }

    function parametrosFiltro(incluirPeriodo) {
        const parametros = new URLSearchParams();
        if (incluirPeriodo) {
            if (filtroAplicado.inicio) {
                parametros.set('inicio', filtroAplicado.inicio);
            }
            if (filtroAplicado.fim) {
                parametros.set('fim', filtroAplicado.fim);
            }
        }
        if (filtroAplicado.tipo) {
            parametros.set('tipoAtendimento', filtroAplicado.tipo);
        }
        return parametros;
    }

    function montarUrl(caminho, parametros) {
        const texto = parametros.toString();
        return texto ? `${caminho}?${texto}` : caminho;
    }

    async function buscar(url) {
        let resposta;
        try {
            resposta = await fetch(url, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin',
                redirect: 'manual'
            });
        } catch (e) {
            throw new ErroRequisicao(0, 'Falha de conexão com o servidor.');
        }
        if (resposta.type === 'opaqueredirect') {
            throw new ErroRequisicao(401, 'Sessão expirada.');
        }
        let corpo = null;
        try {
            corpo = await resposta.json();
        } catch (e) {
            corpo = null;
        }
        if (!resposta.ok) {
            const mensagem = (corpo && corpo.erro) || `Erro HTTP ${resposta.status}`;
            throw new ErroRequisicao(resposta.status, mensagem);
        }
        return corpo;
    }

    function textoKpi(nome, texto) {
        raiz.querySelector(`[data-kpi="${nome}"]`).textContent = texto;
    }

    function renderizarResumo(dados) {
        if (!filtroAplicado.inicio && !filtroAplicado.fim) {
            filtroAplicado.inicio = dados.periodo.inicio;
            filtroAplicado.fim = dados.periodo.fim;
            campoInicio.value = dados.periodo.inicio;
            campoFim.value = dados.periodo.fim;
        }
        dataHojeServidor = dataHojeServidor || dados.periodo.fim;
        textoKpi('faturamento', formatoMoeda.format(dados.faturamento));
        textoKpi('quantidadeVendas', formatoInteiro.format(dados.quantidadeVendas));
        textoKpi('ticketMedio', formatoMoeda.format(dados.ticketMedio));
        textoKpi('quantidadeCancelados', formatoInteiro.format(dados.quantidadeCancelados));
        textoKpi('totalDescontos', formatoMoeda.format(dados.totalDescontos));
        definirEstado('resumo', 'conteudo');
    }

    function opcoesBase() {
        return { responsive: true, maintainAspectRatio: false };
    }

    function opcoesEixoMoeda() {
        return {
            beginAtZero: true,
            ticks: { callback: valor => formatoMoeda.format(valor) }
        };
    }

    function renderizarVendasPorDia(dados) {
        destruirGrafico('vendasPorDia');
        if (!dados.some(item => item.quantidade > 0)) {
            definirEstado('vendasPorDia', 'vazio');
            return;
        }
        definirEstado('vendasPorDia', 'conteudo');
        graficos.vendasPorDia = new Chart(document.getElementById('grafico-vendas-por-dia'), {
            type: 'line',
            data: {
                labels: dados.map(item => formatarDataCurta(item.data)),
                datasets: [{
                    label: 'Faturamento',
                    data: dados.map(item => item.faturamento),
                    borderColor: COR_DESTAQUE,
                    backgroundColor: 'rgba(13, 110, 253, 0.15)',
                    fill: true,
                    tension: 0.25,
                    pointRadius: dados.length > 60 ? 0 : 3
                }]
            },
            options: {
                ...opcoesBase(),
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            title: itens => formatarData(dados[itens[0].dataIndex].data),
                            label: item => `Faturamento: ${formatoMoeda.format(item.parsed.y)}`,
                            afterLabel: item => `Vendas: ${formatoInteiro.format(dados[item.dataIndex].quantidade)}`
                        }
                    }
                },
                scales: { y: opcoesEixoMoeda() }
            }
        });
    }

    function renderizarPedidosPorStatus(dados) {
        destruirGrafico('pedidosPorStatus');
        if (!dados.some(item => item.quantidade > 0)) {
            definirEstado('pedidosPorStatus', 'vazio');
            return;
        }
        definirEstado('pedidosPorStatus', 'conteudo');
        graficos.pedidosPorStatus = new Chart(document.getElementById('grafico-pedidos-por-status'), {
            type: 'doughnut',
            data: {
                labels: dados.map(item => item.descricao),
                datasets: [{
                    data: dados.map(item => item.quantidade),
                    backgroundColor: dados.map(item => CORES_STATUS[item.status] || COR_NEUTRA)
                }]
            },
            options: {
                ...opcoesBase(),
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            label: item => `${item.label}: ${formatoInteiro.format(item.parsed)}`
                        }
                    }
                }
            }
        });
    }

    function renderizarBarras(nomeBloco, idCanvas, itens, rotulos, destacar) {
        destruirGrafico(nomeBloco);
        if (!itens.some(item => item.quantidade > 0)) {
            definirEstado(nomeBloco, 'vazio');
            return;
        }
        definirEstado(nomeBloco, 'conteudo');
        graficos[nomeBloco] = new Chart(document.getElementById(idCanvas), {
            type: 'bar',
            data: {
                labels: rotulos,
                datasets: [{
                    label: 'Vendas',
                    data: itens.map(item => item.quantidade),
                    backgroundColor: itens.map((item, indice) => destacar(item, indice) ? COR_DESTAQUE : COR_NEUTRA)
                }]
            },
            options: {
                ...opcoesBase(),
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: item => `Vendas: ${formatoInteiro.format(item.parsed.y)}`,
                            afterLabel: item => `Faturamento: ${formatoMoeda.format(itens[item.dataIndex].faturamento)}`
                        }
                    }
                },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
            }
        });
    }

    function renderizarPicosHorarios(dados) {
        renderizarBarras('picosHorarios', 'grafico-picos-horarios', dados.itens,
            dados.itens.map(item => `${item.hora}h`),
            item => dados.horaPico !== null && dados.horaPico !== undefined && item.hora === dados.horaPico);
    }

    function renderizarPicosDiasSemana(dados) {
        renderizarBarras('picosDiasSemana', 'grafico-picos-dias-semana', dados.itens,
            dados.itens.map(item => item.nome),
            item => dados.diaPico !== null && dados.diaPico !== undefined && item.dia === dados.diaPico);
    }

    function renderizarProdutos(nomeBloco, dados) {
        if (dados.length === 0) {
            definirEstado(nomeBloco, 'vazio');
            return;
        }
        const corpo = bloco(nomeBloco).querySelector('[data-corpo]');
        corpo.replaceChildren();
        dados.forEach(produto => {
            const linha = criar('tr');
            linha.appendChild(criar('td', 'ps-3', produto.nome));
            linha.appendChild(criar('td', 'text-end', formatoInteiro.format(produto.quantidadeVendida)));
            linha.appendChild(criar('td', 'text-end', formatoMoeda.format(produto.receita)));
            const percentual = produto.percentualDoFaturamento;
            linha.appendChild(criar('td', 'text-end pe-3',
                percentual === null || percentual === undefined ? '—' : `${formatoPercentual.format(percentual)}%`));
            corpo.appendChild(linha);
        });
        definirEstado(nomeBloco, 'conteudo');
    }

    function criarDiferenca(pedido) {
        const diferenca = pedido.diferencaTicketMedio;
        const percentual = pedido.diferencaTicketMedioPercentual;
        if (diferenca === null || diferenca === undefined) {
            return criar('span', 'text-muted', '—');
        }
        const classe = diferenca > 0 ? 'text-bg-success' : (diferenca < 0 ? 'text-bg-danger' : 'text-bg-secondary');
        const percentualTexto = percentual === null || percentual === undefined
            ? ''
            : ` (${formatoPercentualComSinal.format(percentual)}%)`;
        return criar('span', `badge ${classe}`, `${formatoMoedaComSinal.format(diferenca)}${percentualTexto}`);
    }

    function renderizarUltimosPedidos(dados) {
        if (dados.length === 0) {
            definirEstado('ultimosPedidos', 'vazio');
            return;
        }
        const corpo = bloco('ultimosPedidos').querySelector('[data-corpo]');
        corpo.replaceChildren();
        dados.forEach(pedido => {
            const linha = criar('tr');
            linha.appendChild(criar('td', 'ps-3'));
            linha.firstChild.appendChild(criar('code', '', pedido.idCurto));
            linha.appendChild(criar('td', '', formatarDataHora(pedido.dataFinalizacao)));
            linha.appendChild(criar('td', '', ROTULO_ATENDIMENTO[pedido.tipoAtendimento] || '—'));
            linha.appendChild(criar('td', 'text-end', formatoInteiro.format(pedido.quantidadeItens)));
            linha.appendChild(criar('td', 'text-end', formatoMoeda.format(pedido.valorTotal)));
            const celulaDiferenca = criar('td', 'text-end pe-3');
            celulaDiferenca.appendChild(criarDiferenca(pedido));
            linha.appendChild(celulaDiferenca);
            corpo.appendChild(linha);
        });
        definirEstado('ultimosPedidos', 'conteudo');
    }

    const definicoes = {
        resumo: {
            url: () => montarUrl('/api/dashboard/resumo', parametrosFiltro(true)),
            renderizar: renderizarResumo
        },
        vendasPorDia: {
            url: () => montarUrl('/api/dashboard/vendas-por-dia', parametrosFiltro(true)),
            renderizar: renderizarVendasPorDia
        },
        pedidosPorStatus: {
            url: () => montarUrl('/api/dashboard/pedidos-por-status', parametrosFiltro(true)),
            renderizar: renderizarPedidosPorStatus
        },
        picosHorarios: {
            url: () => montarUrl('/api/dashboard/picos/horarios', parametrosFiltro(true)),
            renderizar: renderizarPicosHorarios
        },
        picosDiasSemana: {
            url: () => montarUrl('/api/dashboard/picos/dias-semana', parametrosFiltro(true)),
            renderizar: renderizarPicosDiasSemana
        },
        produtosTop: {
            url: () => {
                const parametros = parametrosFiltro(true);
                parametros.set('ordenarPor', seletorProdutos.value);
                parametros.set('ordem', 'MAIOR');
                parametros.set('limite', '10');
                return montarUrl('/api/dashboard/produtos', parametros);
            },
            renderizar: dados => renderizarProdutos('produtosTop', dados)
        },
        produtosMenor: {
            url: () => {
                const parametros = parametrosFiltro(true);
                parametros.set('ordenarPor', 'RECEITA');
                parametros.set('ordem', 'MENOR');
                parametros.set('limite', '5');
                return montarUrl('/api/dashboard/produtos', parametros);
            },
            renderizar: dados => renderizarProdutos('produtosMenor', dados)
        },
        ultimosPedidos: {
            url: () => montarUrl('/api/dashboard/ultimos-pedidos', parametrosFiltro(false)),
            renderizar: renderizarUltimosPedidos
        }
    };

    const nomesBlocos = {
        resumo: 'Indicadores',
        vendasPorDia: 'Faturamento por dia',
        pedidosPorStatus: 'Pedidos por status',
        picosHorarios: 'Vendas por hora',
        picosDiasSemana: 'Vendas por dia da semana',
        produtosTop: 'Produtos mais vendidos',
        produtosMenor: 'Produtos com menor receita',
        ultimosPedidos: 'Últimos pedidos'
    };

    function esconderAlerta() {
        alerta.classList.add('d-none');
        botaoTentar.classList.add('d-none');
        alertaTexto.textContent = '';
    }

    function mostrarAlerta(texto, permitirNovaTentativa) {
        alertaTexto.textContent = texto;
        botaoTentar.classList.toggle('d-none', !permitirNovaTentativa);
        alerta.classList.remove('d-none');
    }

    async function carregarBloco(nome) {
        sequencias[nome] = (sequencias[nome] || 0) + 1;
        const minhaSequencia = sequencias[nome];
        definirEstado(nome, 'carregando');
        try {
            const dados = await buscar(definicoes[nome].url());
            if (minhaSequencia !== sequencias[nome]) {
                return { nome, ok: true, ignorado: true };
            }
            definicoes[nome].renderizar(dados);
            return { nome, ok: true };
        } catch (e) {
            if (minhaSequencia !== sequencias[nome]) {
                return { nome, ok: true, ignorado: true };
            }
            if (e.status === 401) {
                window.location.href = '/login';
            }
            if (nome === 'vendasPorDia' || nome === 'pedidosPorStatus' || nome === 'picosHorarios' || nome === 'picosDiasSemana') {
                destruirGrafico(nome);
            }
            definirEstado(nome, 'erro');
            return { nome, ok: false, status: e.status, mensagem: e.message };
        }
    }

    function alternarControles(desabilitado) {
        botaoAtualizar.disabled = desabilitado;
        botoesAtalho.forEach(botao => { botao.disabled = desabilitado; });
        botaoTentar.disabled = desabilitado;
        seletorProdutos.disabled = desabilitado;
    }

    async function carregarBlocos(nomes) {
        if (cargaEmAndamento) {
            return;
        }
        cargaEmAndamento = true;
        alternarControles(true);
        esconderAlerta();
        const resultados = await Promise.all(nomes.map(carregarBloco));
        const falhas = resultados.filter(r => !r.ok);
        blocosComFalha = falhas.map(r => r.nome);
        if (falhas.length === 0) {
            atualizadoEm.textContent = `Atualizado às ${horaLocalAtual()}`;
        } else {
            const erroValidacao = falhas.find(r => r.status === 400);
            if (erroValidacao) {
                blocosComFalha = [];
                mostrarAlerta(erroValidacao.mensagem, false);
            } else {
                const lista = falhas.map(r => nomesBlocos[r.nome]).join(', ');
                mostrarAlerta(`Não foi possível carregar: ${lista}.`, true);
            }
        }
        alternarControles(false);
        cargaEmAndamento = false;
    }

    function carregarTudo() {
        return carregarBlocos(Object.keys(definicoes));
    }

    function lerFormulario() {
        return { inicio: campoInicio.value, fim: campoFim.value, tipo: campoTipo.value };
    }

    function validarFormulario(filtro) {
        if (filtro.inicio && filtro.fim && filtro.inicio > filtro.fim) {
            erroFiltro.textContent = 'A data inicial não pode ser posterior à data final.';
            erroFiltro.classList.remove('d-none');
            return false;
        }
        erroFiltro.classList.add('d-none');
        erroFiltro.textContent = '';
        return true;
    }

    function aplicarFormulario() {
        const filtro = lerFormulario();
        if (!validarFormulario(filtro)) {
            return;
        }
        filtroAplicado = filtro;
        carregarTudo();
    }

    function aplicarAtalho(dias) {
        const fim = dataHojeServidor || dataLocalDeHoje();
        campoFim.value = fim;
        campoInicio.value = subtrairDias(fim, dias - 1);
        aplicarFormulario();
    }

    botaoAtualizar.addEventListener('click', aplicarFormulario);
    botoesAtalho.forEach(botao => {
        botao.addEventListener('click', () => aplicarAtalho(Number(botao.dataset.atalho)));
    });
    seletorProdutos.addEventListener('change', () => carregarBlocos(['produtosTop']));
    botaoTentar.addEventListener('click', () => carregarBlocos(blocosComFalha.slice()));

    carregarTudo();
})();
