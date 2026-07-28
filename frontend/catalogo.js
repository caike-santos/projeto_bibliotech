let listaGlobalLivros = [];

document.addEventListener('DOMContentLoaded', () => {
    verificarAutenticacao();
    carregarCatalogo();
    configurarBusca();
    configurarChatLumina();
    exibirNomeUsuario();
});

function verificarAutenticacao() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        window.location.href = 'index.html';
    }

    // Logout
    document.getElementById('btnSair').addEventListener('click', () => {
        localStorage.removeItem('jwtToken');
        window.location.href = 'index.html';
    });
}

async function carregarCatalogo() {
    const token = localStorage.getItem('jwtToken');
    const gridLivros = document.getElementById('gridLivros');

    try {
        const response = await fetch('http://localhost:8080/livros', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                alert('Sessão expirada. Faça login novamente.');
                localStorage.removeItem('jwtToken');
                window.location.href = 'index.html';
                return;
            }
            throw new Error('Erro ao buscar o catálogo');
        }

        const data = await response.json();
        listaGlobalLivros = data.content || data; 

        renderizarLivros(listaGlobalLivros);

    } catch (error) {
        console.error('Erro na requisição:', error);
        gridLivros.innerHTML = '<p style="color: red;">Erro ao carregar o acervo. Verifique a API.</p>';
    }
}

function renderizarLivros(livros) {
    const gridLivros = document.getElementById('gridLivros');
    gridLivros.innerHTML = '';

    if (livros.length === 0) {
        gridLivros.innerHTML = '<p style="color: var(--text-muted);">Nenhum livro encontrado com esse termo.</p>';
        return;
    }

    livros.forEach(livro => {
        const card = document.createElement('div');
        card.className = 'livro-card';
        card.style.cursor = 'pointer';
        
        // Ao clicar no card, abre o modal completão
        card.addEventListener('click', () => abrirDetalhesLivro(livro));

        card.innerHTML = `
            <div class="livro-capa" style="background-image: url('${livro.capaUrl || 'https://via.placeholder.com/150x200?text=Sem+Capa'}')"></div>
            <div class="livro-info">
                <span class="livro-genero">${livro.generoPrincipal || 'Geral'}</span>
                <h3 class="livro-titulo">${livro.titulo}</h3>
                <p class="livro-autor">${livro.autor}</p>
                <div class="livro-estoque">
                    <i class="ph ph-books"></i> Disponíveis: ${livro.quantidadeDisponivel}
                </div>
                <button class="btn-primary btn-emprestar" onclick="event.stopPropagation(); realizarEmprestimo(${livro.id})">Solicitar Empréstimo</button>
            </div>
        `;
        
        gridLivros.appendChild(card);
    });
}

function abrirDetalhesLivro(livro) {
    document.getElementById('detalheTituloHeader').innerText = livro.titulo || 'Livro';
    document.getElementById('detalheTitulo').innerText = livro.titulo || 'Sem título';
    document.getElementById('detalheAutor').innerText = livro.autor || 'Desconhecido';
    
    // Tratamento com fallbacks caso o banco venha nulo para este livro
    document.getElementById('detalheEditora').innerText = livro.editora || 'Editora não informada';
    document.getElementById('detalheAno').innerText = livro.ano || 'N/D';
    document.getElementById('detalheIsbn').innerText = livro.isbn || 'N/D';
    document.getElementById('detalheGenero').innerText = livro.generoPrincipal || 'Geral';
    document.getElementById('detalheSinopse').innerText = livro.sinopse || 'Nenhuma sinopse cadastrada para esta obra no sistema.';
    document.getElementById('detalheEstoque').innerText = `${livro.quantidadeDisponivel ?? 0} de ${livro.quantidadeTotal ?? 0}`;
    
    // Fallback para a capa: se a URL falhar ou estiver vazia, usa uma imagem padrão
    const capaDiv = document.getElementById('detalheCapa');
    const urlCapa = livro.capaUrl || 'https://via.placeholder.com/150x200?text=Sem+Capa';
    capaDiv.style.backgroundImage = `url("${urlCapa}")`;

    // Renderiza as tags secundárias
    const tagsContainer = document.getElementById('detalheTags');
    tagsContainer.innerHTML = '';
    if (livro.tagsSecundarias && livro.tagsSecundarias.length > 0) {
        livro.tagsSecundarias.forEach(tag => {
            const badge = document.createElement('span');
            badge.className = 'tag-badge';
            badge.innerText = tag.nome;
            tagsContainer.appendChild(badge);
        });
    } else {
        tagsContainer.innerHTML = '<span style="font-size: 0.8rem; color: var(--text-muted);">Nenhuma tag secundária cadastrada.</span>';
    }

    const btnModal = document.getElementById('btnEmprestarModal');
    btnModal.onclick = () => {
        realizarEmprestimo(livro.id);
        fecharDetalhesLivro();
    };

    const modal = document.getElementById('modalDetalhes');
    modal.style.transform = 'translateX(-50%) scale(1)';
}

function fecharDetalhesLivro() {
    const modal = document.getElementById('modalDetalhes');
    modal.style.transform = 'translateX(-50%) scale(0)';
}

document.getElementById('btnCloseDetalhes').addEventListener('click', fecharDetalhesLivro);
// Lógica de Filtragem Instantânea na Barra de Pesquisa
function configurarBusca() {
    const inputBusca = document.getElementById('inputBusca');
    
    inputBusca.addEventListener('input', (e) => {
        const termo = e.target.value.toLowerCase();
        
        const livrosFiltrados = listaGlobalLivros.filter(livro => {
            const tituloMatch = livro.titulo.toLowerCase().includes(termo);
            const autorMatch = livro.autor.toLowerCase().includes(termo);
            return tituloMatch || autorMatch;
        });

        renderizarLivros(livrosFiltrados);
    });
}

// Rota de Empréstimo (Conectada ao Back-end)
async function realizarEmprestimo(livroId) {
    const token = localStorage.getItem('jwtToken');
    
    try {
        const response = await fetch('http://localhost:8080/emprestimos', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ livroId: livroId })
        });

        if (response.ok) {
            alert('Empréstimo realizado com sucesso! Verifique o prazo de devolução.');
            carregarCatalogo(); // Atualiza a quantidade de estoque na tela
        } else {
            const erroMsg = await response.text();
            alert('Não foi possível realizar o empréstimo: ' + (erroMsg || 'Limite excedido ou livro indisponível.'));
        }
    } catch (e) {
        console.error('Erro:', e);
        alert('Falha ao comunicar com o servidor de empréstimos.');
    }
}

// Configuração do Chat da Lumina
function configurarChatLumina() {
    const btnToggle = document.getElementById('btnLuminaToggle');
    const modal = document.getElementById('luminaChatModal');
    const btnClose = document.getElementById('btnCloseChat');
    const btnSend = document.getElementById('luminaSend');
    const input = document.getElementById('luminaInput');
    const messages = document.getElementById('luminaMessages');

    btnToggle.addEventListener('click', () => modal.classList.toggle('active'));
    btnClose.addEventListener('click', () => modal.classList.remove('active'));

    btnSend.addEventListener('click', enviarMensagem);
    input.addEventListener('keypress', (e) => { if (e.key === 'Enter') enviarMensagem(); });

    async function enviarMensagem() {
        const texto = input.value.trim();
        const token = localStorage.getItem('jwtToken');
        if (!texto) return;

        adicionarMsg(texto, 'user');
        input.value = '';
        messages.scrollTop = messages.scrollHeight;

        const idTemp = adicionarMsg('Processando dados...', 'bot');

        try {
            const res = await fetch('http://localhost:8080/assistente/chat', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ mensagem: texto })
            });

            if (!res.ok) throw new Error();
            const respostaIa = await res.text();

            document.getElementById(idTemp).remove();
            adicionarMsg(respostaIa, 'bot');
        } catch {
            document.getElementById(idTemp).remove();
            adicionarMsg('Erro de conexão com os sensores da Lumina.', 'bot');
        }
        messages.scrollTop = messages.scrollHeight;
    }

    function adicionarMsg(texto, remetente) {
        const div = document.createElement('div');
        div.className = `lumina-msg ${remetente}`;
        div.innerText = texto;
        const id = 'msg-' + Date.now();
        div.id = id;
        messages.appendChild(div);
        return id;
    }

    // Adicione esta função dentro do seu catalogo.js e chame-la no DOMContentLoaded
    
}

function exibirNomeUsuario() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return;

    try {
        // O JWT é dividido em 3 partes separadas por ponto. A do meio (payload) guarda os dados em Base64.
        const base64Payload = token.split('.')[1];
        const payloadJson = atob(base64Payload);
        const dadosUsuario = JSON.parse(payloadJson);

        // O Spring Security costuma guardar o login/email na propriedade 'sub' (subject)
        const emailOuUser = dadosUsuario.sub || 'Usuário';
        
        // Atualiza o texto do selo com o e-mail ou nome real
        const badgeUser = document.getElementById('userInfo');
        if (badgeUser) {
            badgeUser.innerText = emailOuUser;
        }
    } catch (e) {
        console.error('Erro ao decodificar o token:', e);
    }
}

function abrirDetalhesLivro(livro) {
    document.getElementById('detalheTituloHeader').innerText = livro.titulo;
    document.getElementById('detalheTitulo').innerText = livro.titulo;
    document.getElementById('detalheAutor').innerText = livro.autor;
    document.getElementById('detalheGenero').innerText = livro.generoPrincipal || 'Não informado';
    document.getElementById('detalheEstoque').innerText = livro.quantidadeDisponivel;
    
    // Configura o botão de empréstimo de dentro do modal
    const btnModal = document.getElementById('btnEmprestarModal');
    btnModal.onclick = () => {
        realizarEmprestimo(livro.id);
        fecharDetalhesLivro();
    };

    const modal = document.getElementById('modalDetalhes');
    modal.style.transform = 'translateX(-50%) scale(1)';
}

function fecharDetalhesLivro() {
    const modal = document.getElementById('modalDetalhes');
    modal.style.transform = 'translateX(-50%) scale(0)';
}

document.getElementById('btnCloseDetalhes').addEventListener('click', fecharDetalhesLivro);