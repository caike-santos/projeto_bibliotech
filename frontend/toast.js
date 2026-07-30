// toast.js - Global Utility for Toast Notifications

/**
 * Corrige o problema de Timezone Javascript onde new Date('YYYY-MM-DD') 
 * subtrai 3 horas no Brasil, recuando o dia.
 */
window.formatarDataLocal = function(dataString) {
    if (!dataString) return '-';
    // Se a string tem formato ISO curto (somente data), injeta meio-dia UTC
    const dateToParse = dataString.length === 10 ? `${dataString}T12:00:00` : dataString;
    return new Date(dateToParse).toLocaleDateString();
};

window.renovarEmprestimo = async function(emprestimoId, renderCallback) {
    const confirmed = await showCustomConfirm('Renovar EmprÃ©stimo', 'Deseja renovar este emprÃ©stimo por mais 14 dias? (Regra: Apenas 1 renovaÃ§Ã£o permitida por livro)', 'info');
    if (!confirmed) return;

    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/emprestimos/${emprestimoId}/renovar`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            showToast('EmprÃ©stimo renovado com sucesso por mais 14 dias!', 'success');
            // Se estiver no catÃ¡logo, chama renderCatalog()
            if (typeof window.renderCatalog === 'function') {
                window.renderCatalog();
            }
            // Se estiver no perfil, chama carregarHistorico()
            if (typeof window.carregarHistorico === 'function') {
                window.carregarHistorico(window.dadosUsuarioGlobal?.id, token);
            }
        } else {
            await window.handleApiError(response, 'Falha ao renovar. Verifique se o livro jÃ¡ foi renovado ou hÃ¡ atrasos.');
        }
    } catch (error) {
        showToast('Erro de conexÃ£o ao tentar renovar o emprÃ©stimo.', 'error');
    }
};

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
    if (!container) return; // SeguranÃ§a

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

    // Entrada animada (por padrÃ£o o CSS cuidarÃ¡ do estado final se usarmos animaÃ§Ã£o keyframes ou setTimeout)
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // Remover apÃ³s 5 segundos
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300); // tempo da transiÃ§Ã£o
    }, 5000);
}

/**
 * Exibe um alert personalizado
 * @param {string} title - O tÃ­tulo do modal
 * @param {string} message - A mensagem
 * @param {string} type - 'info', 'warning', 'error', 'success'
 * @returns {Promise<void>}
 */
function showCustomAlert(title, message, type = 'info') {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
            z-index: 10000; display: flex; justify-content: center; align-items: center;
            opacity: 0; transition: opacity 0.3s ease;
        `;

        let iconClass = 'ph-info';
        let colorVar = 'var(--primary-color)';
        
        if (type === 'error') {
            iconClass = 'ph-x-circle';
            colorVar = '#EF4444';
        } else if (type === 'warning') {
            iconClass = 'ph-warning';
            colorVar = 'var(--warning-color)';
        } else if (type === 'success') {
            iconClass = 'ph-check-circle';
            colorVar = '#10B981';
        }

        const modal = document.createElement('div');
        modal.style.cssText = `
            background: var(--surface-color); padding: 2rem; border-radius: 12px;
            max-width: 400px; width: 90%; text-align: center;
            box-shadow: 0 10px 25px rgba(0,0,0,0.3);
            transform: scale(0.9); transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            border-top: 4px solid ${colorVar};
        `;

        modal.innerHTML = `
            <i class="ph ${iconClass}" style="font-size: 3.5rem; color: ${colorVar}; margin-bottom: 1rem;"></i>
            <h3 style="color: var(--text-color); margin-bottom: 0.5rem; font-size: 1.25rem;">${title}</h3>
            <p style="color: var(--text-muted); font-size: 0.95rem; margin-bottom: 1.5rem; line-height: 1.5;">${message}</p>
            <div style="display: flex; justify-content: center;">
                <button id="btnOkAlert" class="btn-primary" style="background: ${colorVar}; border-color: ${colorVar}; color: #fff; width: 100%;">OK</button>
            </div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        requestAnimationFrame(() => {
            overlay.style.opacity = '1';
            modal.style.transform = 'scale(1)';
        });

        const fechar = () => {
            overlay.style.opacity = '0';
            modal.style.transform = 'scale(0.9)';
            setTimeout(() => {
                if (document.body.contains(overlay)) document.body.removeChild(overlay);
                resolve();
            }, 300);
        };

        modal.querySelector('#btnOkAlert').addEventListener('click', fechar);
    });
}

/**
 * Exibe um confirm personalizado
 * @param {string} title - O tÃ­tulo do modal
 * @param {string} message - A mensagem
 * @param {string} type - 'warning', 'danger', 'info'
 * @returns {Promise<boolean>}
 */
function showCustomConfirm(title, message, type = 'warning') {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
            z-index: 10000; display: flex; justify-content: center; align-items: center;
            opacity: 0; transition: opacity 0.3s ease;
        `;

        let iconClass = 'ph-warning';
        let colorVar = 'var(--warning-color)';
        
        if (type === 'danger') {
            iconClass = 'ph-warning-circle';
            colorVar = '#EF4444';
        } else if (type === 'info') {
            iconClass = 'ph-question';
            colorVar = 'var(--primary-color)';
        }

        const modal = document.createElement('div');
        modal.style.cssText = `
            background: var(--surface-color); padding: 2rem; border-radius: 12px;
            max-width: 400px; width: 90%; text-align: center;
            box-shadow: 0 10px 25px rgba(0,0,0,0.3);
            transform: scale(0.9); transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            border-top: 4px solid ${colorVar};
        `;

        modal.innerHTML = `
            <i class="ph ${iconClass}" style="font-size: 3.5rem; color: ${colorVar}; margin-bottom: 1rem;"></i>
            <h3 style="color: var(--text-color); margin-bottom: 0.5rem; font-size: 1.25rem;">${title}</h3>
            <p style="color: var(--text-muted); font-size: 0.95rem; margin-bottom: 1.5rem; line-height: 1.5;">${message}</p>
            <div style="display: flex; gap: 1rem; justify-content: center;">
                <button id="btnCancelConfirm" class="btn-primary" style="flex: 1; background: transparent; color: var(--text-color); border: 1px solid var(--border-color);">Cancelar</button>
                <button id="btnOkConfirm" class="btn-primary" style="flex: 1; background: ${colorVar}; border-color: ${colorVar}; color: #fff;">Confirmar</button>
            </div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        requestAnimationFrame(() => {
            overlay.style.opacity = '1';
            modal.style.transform = 'scale(1)';
        });

        const fechar = (resultado) => {
            overlay.style.opacity = '0';
            modal.style.transform = 'scale(0.9)';
            setTimeout(() => {
                if (document.body.contains(overlay)) document.body.removeChild(overlay);
                resolve(resultado);
            }, 300);
        };

        modal.querySelector('#btnCancelConfirm').addEventListener('click', () => fechar(false));
        modal.querySelector('#btnOkConfirm').addEventListener('click', () => fechar(true));
    });
}

/**
 * Trata erros de API de forma elegante, decodificando JSON caso exista.
 * @param {Response} response - O objeto de resposta do Fetch
 * @param {string} defaultMessage - Mensagem padrÃ£o caso nÃ£o consiga extrair erro
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
    
    // Tratamento estÃ©tico final: intercepta erros tÃ©cnicos em inglÃªs ou Java e traduz para algo amigÃ¡vel
    if (finalMessage.includes('Cannot invoke') || finalMessage.includes('java.lang') || finalMessage.includes('com.bibliotech')) {
        finalMessage = 'Ocorreu um erro interno no servidor ao tentar processar esta aÃ§Ã£o. Tente novamente mais tarde.';
    }

    showToast(finalMessage, 'error');
}

// --- Fetch Interceptor Global ---
// Captura todas as chamadas de rede para exibir e ocultar o Loader automaticamente
let activeRequests = 0;
const originalFetch = window.fetch;

window.fetch = async function(...args) {
    let options = args[1];
    let skipLoader = false;
    
    if (options && options.skipLoader) {
        skipLoader = true;
        // Remove a propriedade customizada para não poluir o RequestInit nativo
        const { skipLoader: _, ...rest } = options;
        args[1] = rest;
    }

    if (skipLoader) {
        return originalFetch.apply(this, args);
    }

    activeRequests++;
    
    // Determina a mensagem baseada na URL
    const url = typeof args[0] === 'string' ? args[0] : (args[0]?.url || '');
    let msg = 'Carregando...';
    
    if (url.includes('/login')) msg = 'Autenticando credenciais...';
    else if (url.includes('/cadastrar-por-isbn')) msg = 'A Lumina estÃ¡ lendo e catalogando o livro...';
    else if (url.includes('/recomendacoes') || url.includes('/clustering')) msg = 'Analisando perfis e recomendaÃ§Ãµes...';
    else if (url.includes('/emprestimos') && url.includes('/renovar')) msg = 'Renovando emprÃ©stimo...';
    else if (url.includes('/emprestimos') && !url.includes('/usuario')) msg = 'Registrando transaÃ§Ã£o...';
    else if (url.includes('/reservas')) msg = 'Processando reserva...';
    else if (url.includes('/livros') && args[1]?.method === 'POST') msg = 'Salvando livro...';
    
    showGlobalLoader(msg);

    try {
        const response = await originalFetch.apply(this, args);
        return response;
    } catch (error) {
        if (error.message === 'Failed to fetch') {
            showToast('O servidor de IA estÃ¡ acordando da inatividade. Isso pode levar atÃ© 60 segundos. Aguarde um instante e tente novamente.', 'warning');
        }
        throw error;
    } finally {
        activeRequests--;
        if (activeRequests <= 0) {
            activeRequests = 0;
            hideGlobalLoader();
        }
    }
};

