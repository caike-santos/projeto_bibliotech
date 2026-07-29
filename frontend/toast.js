// toast.js - Global Utility for Toast Notifications

// Inject toast container into the DOM if it doesn't exist
document.addEventListener('DOMContentLoaded', () => {
    if (!document.getElementById('toast-container')) {
        const container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }
    
    // Inject global loader if it doesn't exist
    if (!document.getElementById('global-loader')) {
        const loader = document.createElement('div');
        loader.id = 'global-loader';
        loader.innerHTML = `
            <div class="loader-spinner"></div>
            <div class="loader-text" id="loader-text">Carregando...</div>
        `;
        document.body.appendChild(loader);
    }
});

/**
 * Exibe a tela de carregamento global
 * @param {string} message 
 */
function showGlobalLoader(message = 'Carregando...') {
    const loader = document.getElementById('global-loader');
    const textEl = document.getElementById('loader-text');
    if (loader && textEl) {
        textEl.innerText = message;
        loader.classList.add('show');
    }
}

/**
 * Oculta a tela de carregamento global
 */
function hideGlobalLoader() {
    const loader = document.getElementById('global-loader');
    if (loader) {
        loader.classList.remove('show');
    }
}

/**
 * Exibe um toast na tela.
 * @param {string} message - A mensagem a ser exibida
 * @param {string} type - 'success', 'error', 'info', 'warning'
 */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return; // Segurança

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'ph-info';
    if (type === 'success') icon = 'ph-check-circle';
    if (type === 'error') icon = 'ph-warning-circle';
    if (type === 'warning') icon = 'ph-warning';

    toast.innerHTML = `
        <i class="ph ${icon} toast-icon"></i>
        <div class="toast-content">${message}</div>
        <button class="toast-close" onclick="this.parentElement.remove()"><i class="ph ph-x"></i></button>
    `;

    container.appendChild(toast);

    // Entrada animada (por padrão o CSS cuidará do estado final se usarmos animação keyframes ou setTimeout)
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // Remover após 5 segundos
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300); // tempo da transição
    }, 5000);
}

/**
 * Trata erros de API de forma elegante, decodificando JSON caso exista.
 * @param {Response} response - O objeto de resposta do Fetch
 * @param {string} defaultMessage - Mensagem padrão caso não consiga extrair erro
 */
async function handleApiError(response, defaultMessage = 'Ocorreu um erro inesperado.') {
    let finalMessage = defaultMessage;
    try {
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const errorObj = await response.json();
            finalMessage = errorObj.mensagem || errorObj.message || errorObj.error || defaultMessage;
            
            // Se ainda for um JSON encadeado na mensagem (comum em erros do Spring tratados de certa forma)
            if (typeof finalMessage === 'string' && finalMessage.startsWith('{')) {
                 try {
                     const innerJson = JSON.parse(finalMessage);
                     finalMessage = innerJson.mensagem || innerJson.message || innerJson.error || finalMessage;
                 } catch(e) {}
            }
        } else {
            const text = await response.text();
            if (text) {
                 try {
                     const jsonText = JSON.parse(text);
                     finalMessage = jsonText.mensagem || jsonText.message || jsonText.error || text;
                 } catch(e) {
                     finalMessage = text;
                 }
            }
        }
    } catch (e) {
        console.error('Erro ao ler a resposta da API', e);
    }
    
    // Tratamento estético final: intercepta erros técnicos em inglês ou Java e traduz para algo amigável
    if (finalMessage.includes('Cannot invoke') || finalMessage.includes('java.lang') || finalMessage.includes('com.bibliotech')) {
        finalMessage = 'Ocorreu um erro interno no servidor ao tentar processar esta ação. Tente novamente mais tarde.';
    }

    showToast(finalMessage, 'error');
}

// --- Fetch Interceptor Global ---
// Captura todas as chamadas de rede para exibir e ocultar o Loader automaticamente
let activeRequests = 0;
const originalFetch = window.fetch;

window.fetch = async function(...args) {
    const options = args[1] || {};
    if (options.skipLoader) {
        return originalFetch.apply(this, args);
    }

    activeRequests++;
    
    // Determina a mensagem baseada na URL
    const url = typeof args[0] === 'string' ? args[0] : (args[0]?.url || '');
    let msg = 'Carregando...';
    
    if (url.includes('/login')) msg = 'Autenticando credenciais...';
    else if (url.includes('/cadastrar-por-isbn')) msg = 'A Lumina está lendo e catalogando o livro...';
    else if (url.includes('/recomendacoes') || url.includes('/clustering')) msg = 'Analisando perfis e recomendações...';
    else if (url.includes('/emprestimos') && url.includes('/renovar')) msg = 'Renovando empréstimo...';
    else if (url.includes('/emprestimos') && !url.includes('/usuario')) msg = 'Registrando transação...';
    else if (url.includes('/reservas')) msg = 'Processando reserva...';
    else if (url.includes('/livros') && args[1]?.method === 'POST') msg = 'Salvando livro...';
    
    showGlobalLoader(msg);

    try {
        const response = await originalFetch.apply(this, args);
        return response;
    } catch (error) {
        throw error;
    } finally {
        activeRequests--;
        if (activeRequests <= 0) {
            activeRequests = 0;
            hideGlobalLoader();
        }
    }
};
