

const PediuPatrao = (() => {
    function cabecalhoCsrf() {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        return token && header ? { [header.content]: token.content } : {};
    }

    async function enviar(metodo, url, corpo) {
        const headers = { 'Accept': 'application/json', ...cabecalhoCsrf() };
        const opcoes = { method: metodo, headers, credentials: 'same-origin' };
        if (corpo !== undefined) {
            headers['Content-Type'] = 'application/json';
            opcoes.body = JSON.stringify(corpo);
        }

        const resposta = await fetch(url, opcoes);
        let dados = null;
        try {
            dados = await resposta.json();
        } catch (e) {

        }
        if (!resposta.ok) {
            throw new Error((dados && dados.erro) || `Erro HTTP ${resposta.status}`);
        }
        return dados;
    }

    return { enviar };
})();
