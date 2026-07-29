let listaGlobalLivros = [];
let usuarioLogadoId = null;
let usuarioLogadoNome = '';

document.addEventListener('DOMContentLoaded', async () => {
    verificarAutenticacao();
    await carregarDadosDoUsuario();
    carregarCatalogo();
    configurarBusca();
    configurarChatLumina();
    exibirNomeUsuario();
    
    // Novas funcionalidades
    if(usuarioLogadoId) {
        carregarGamificacao();
        carregarNotificacoes();
    }
    
    // Bind botão de clustering manual
    document.getElementById('btnGerarClustering').addEventListener('click', carregarRecomendacoes);
    
    configurarModalMeusEmprestimos();
});

async function carregarDadosDoUsuario() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return;
    
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const email = payload.sub;
        
        // Pega todos os usuários e filtra pelo email para achar o ID (fallback seguro)
        const response = await fetch('https://bibliotech-api-e9wg.onrender.com/usuarios', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if(response.ok) {
            const usuarios = await response.json();
            const user = usuarios.find(u => u.email === email);
            if(user) {
                usuarioLogadoId = user.id;
                usuarioLogadoNome = user.nome;
                localStorage.setItem('userName', user.nome);
            }
        }
    } catch(e) {
        console.error('Erro ao buscar dados do usuário logado', e);
    }
}

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
        const response = await fetch('https://bibliotech-api-e9wg.onrender.com/livros', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            if (response.status === 403 || response.status === 401) {
                localStorage.removeItem('jwtToken');
                showToast('Sessão expirada. Faça login novamente.', 'warning');
                setTimeout(() => { window.location.href = 'index.html'; }, 1500);
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
                    ${livro.quantidadeDisponivel > 0 
                        ? `<button class="btn-primary btn-emprestar" onclick="event.stopPropagation(); abrirModalTermos(${livro.id}, 'EMPRESTIMO')">Solicitar Empréstimo</button>` 
                        : `<button class="btn-primary btn-warning btn-emprestar" onclick="event.stopPropagation(); abrirModalTermos(${livro.id}, 'RESERVA')">Fazer Reserva</button>`
                    }
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
    
    // Fallback para a capa
    const capaDiv = document.getElementById('detalheCapa');
    const urlCapa = livro.capaUrl || 'https://via.placeholder.com/150x200?text=Sem+Capa';
    capaDiv.style.backgroundImage = `url("${urlCapa}")`;

    // Renderiza as tags
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
        tagsContainer.innerHTML = '<span style="font-size: 0.8rem; color: var(--text-muted);">Nenhuma tag secundária.</span>';
    }

    const btnEmprestar = document.getElementById('btnEmprestarModal');
    const btnReservar = document.getElementById('btnReservarModal');

    // Lógica de Fila de Espera vs Empréstimo
        if (livro.quantidadeDisponivel > 0) {
            btnEmprestar.style.display = 'block';
            btnReservar.style.display = 'none';
            btnEmprestar.onclick = () => {
                fecharDetalhesLivro();
                abrirModalTermos(livro.id, 'EMPRESTIMO');
            };
        } else {
            btnEmprestar.style.display = 'none';
            btnReservar.style.display = 'block';
            btnReservar.onclick = () => {
                fecharDetalhesLivro();
                abrirModalTermos(livro.id, 'RESERVA');
            };
        }

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

// ---------------- LÓGICA DO MODAL DE TERMOS ---------------- //
let acaoPendente = null;

function abrirModalTermos(livroId, tipo) {
    acaoPendente = { tipo, livroId };
    const checkbox = document.getElementById('checkTermos');
    const btn = document.getElementById('btnConfirmarTermos');
    
    if (checkbox) checkbox.checked = false;
    if (btn) {
        btn.disabled = true;
        btn.style.opacity = '0.5';
        btn.style.cursor = 'not-allowed';
        
        btn.onclick = () => {
            if (!checkbox.checked || !acaoPendente) return;
            
            // Salva os dados antes de fechar o modal
            const acao = acaoPendente;
            fecharModalTermos();
            
            if (acao.tipo === 'EMPRESTIMO') {
                realizarEmprestimo(acao.livroId);
            } else if (acao.tipo === 'RESERVA') {
                entrarFilaEspera(acao.livroId);
            }
        };
    }

    const modal = document.getElementById('modalTermos');
    if (modal) modal.style.transform = 'translate(-50%, -50%) scale(1)';
}

function fecharModalTermos() {
    acaoPendente = null;
    const modal = document.getElementById('modalTermos');
    if (modal) modal.style.transform = 'translate(-50%, -50%) scale(0)';
}

document.addEventListener('DOMContentLoaded', () => {
    const checkbox = document.getElementById('checkTermos');
    if (checkbox) {
        checkbox.addEventListener('change', function() {
            const btn = document.getElementById('btnConfirmarTermos');
            if (!btn) return;
            if (this.checked) {
                btn.disabled = false;
                btn.style.opacity = '1';
                btn.style.cursor = 'pointer';
            } else {
                btn.disabled = true;
                btn.style.opacity = '0.5';
                btn.style.cursor = 'not-allowed';
            }
        });
    }
});

// Rota de Empréstimo (Conectada ao Back-end)
async function realizarEmprestimo(livroId) {
    const token = localStorage.getItem('jwtToken');
    
    try {
        const response = await fetch('https://bibliotech-api-e9wg.onrender.com/emprestimos', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                livro: { id: livroId }, 
                usuario: { id: usuarioLogadoId } 
            })
        });

        if (response.ok) {
            showToast('Empréstimo realizado com sucesso! Verifique o prazo de devolução.', 'success');
            carregarCatalogo();
            if (usuarioLogadoId) {
                carregarNotificacoes();
                carregarGamificacao();
                carregarMeusEmprestimos();
            }
        } else {
            await handleApiError(response, 'Não foi possível realizar o empréstimo.');
        }
    } catch (erro) {
        console.error('Erro de conexão:', erro);
        showToast('Falha ao comunicar com o servidor de empréstimos.', 'error');
    }
}

// Configuração do Chat da Lumina
function configurarChatLumina() {
    const btnToggle = document.getElementById('btnLuminaToggle');
    const modal = document.getElementById('luminaChatModal');
    const closeBtn = document.getElementById('btnCloseChat');
    const btnSend = document.getElementById('luminaSend');
    const input = document.getElementById('luminaInput');
    const messages = document.getElementById('luminaMessages');
    const bubble = document.getElementById('luminaBubble');

    // Mostra o balão se for a primeira vez
    if (bubble && !localStorage.getItem('luminaWelcomeSeen')) {
        bubble.style.display = 'block';
    }

    btnToggle.addEventListener('click', () => {
        if (bubble) {
            bubble.style.display = 'none';
            localStorage.setItem('luminaWelcomeSeen', 'true');
        }
        modal.classList.add('active');
        if(messages.children.length === 0) {
            adicionarMsg('Olá! Eu sou a Lumina. Posso te ajudar a encontrar um livro, recomendar leituras ou tirar dúvidas sobre a biblioteca. O que deseja hoje?', 'bot');
        }
    });

    closeBtn.addEventListener('click', () => modal.classList.remove('active'));

    btnSend.addEventListener('click', enviarMensagem);
    input.addEventListener('keypress', (e) => { if (e.key === 'Enter') enviarMensagem(); });
    
    // Botão de sugestão para recomendar livros
    document.getElementById('btnLuminaRecomendar').addEventListener('click', async () => {
        adicionarMsg('Me recomende um livro', 'user');
        const idTemp = adicionarMsg('Analisando seu histórico de leitura e cruzando com nosso acervo...', 'bot');
        
        try {
            const token = localStorage.getItem('jwtToken');
            const res = await fetch(`https://bibliotech-api-e9wg.onrender.com/livros/recomendacoes/usuario/${usuarioLogadoId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!res.ok) throw new Error();
            const respostaIa = await res.text();
            
            document.getElementById(idTemp).remove();
            adicionarMsg(respostaIa, 'bot');
        } catch (e) {
            document.getElementById(idTemp).remove();
            adicionarMsg('Desculpe, tive um problema ao buscar suas recomendações no momento.', 'bot');
        }
        messages.scrollTop = messages.scrollHeight;
    });

    async function enviarMensagem() {
        const texto = input.value.trim();
        const token = localStorage.getItem('jwtToken');
        if (!texto) return;

        adicionarMsg(texto, 'user');
        input.value = '';
        messages.scrollTop = messages.scrollHeight;

        const idTemp = adicionarMsg('Processando dados...', 'bot');

        try {
            const res = await fetch('https://bibliotech-api-e9wg.onrender.com/assistente/chat', {
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
        const wrapper = document.createElement('div');
        wrapper.className = `lumina-msg-wrapper ${remetente}`;
        wrapper.style.display = 'flex';
        wrapper.style.alignItems = 'flex-end';
        wrapper.style.gap = '0.5rem';
        wrapper.style.marginBottom = '0.25rem';
        
        if (remetente === 'user') {
            wrapper.style.justifyContent = 'flex-end';
        }
        
        let html = '';
        if (remetente === 'bot') {
            html += `<img src="imagens/lumina.png" alt="Lumina" style="width: 38px; height: 38px; border-radius: 50%; border: 1px solid var(--primary-color); object-fit: cover; flex-shrink: 0; background: var(--surface-color);">`;
        }
        
        html += `<div class="lumina-msg ${remetente}">${texto}</div>`;
        wrapper.innerHTML = html;
        
        const id = 'msg-' + Date.now();
        wrapper.id = id;
        messages.appendChild(wrapper);
        return id;
    }
}

function exibirNomeUsuario() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return;
    try {
        const base64Payload = token.split('.')[1];
        const dadosUsuario = JSON.parse(atob(base64Payload));
        
        // Usa o nome salvo no localStorage ou cai para o email (sub)
        const nomeParaExibir = localStorage.getItem('userName') || dadosUsuario.sub || 'Usuário';
        
        const badgeUser = document.getElementById('userInfo');
        if (badgeUser) badgeUser.innerText = nomeParaExibir;
    } catch (e) {
        console.error('Erro ao decodificar o token:', e);
    }
}

// ----------------- Novas Funcionalidades ----------------- //

async function entrarFilaEspera(livroId) {
    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/reservas?usuarioId=${usuarioLogadoId}&livroId=${livroId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (response.ok) {
            showToast('Você entrou na Fila de Espera! Notificaremos quando estiver disponível.', 'success');
        } else {
            await handleApiError(response, 'Falha ao entrar na fila.');
        }
    } catch (e) {
        console.error('Erro:', e);
        showToast('Falha ao conectar com o servidor.', 'error');
    }
}

async function carregarGamificacao() {
    if(!usuarioLogadoId) return;
    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/usuarios/${usuarioLogadoId}/gamificacao`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if(response.ok) {
            const data = await response.json();
            const badge = document.getElementById('gamificacaoBadge');
            badge.innerHTML = `${data.selo} ${data.nivel} (${data.totalEmprestimos} lidos)`;
        }
    } catch(e) {
        console.error('Gamificação falhou', e);
    }
}

async function carregarRecomendacoes() {
    if(!usuarioLogadoId) return;
    const token = localStorage.getItem('jwtToken');
    const txtRecomendacao = document.getElementById('textoRecomendacao');
    const btnGerar = document.getElementById('btnGerarClustering');
    
    txtRecomendacao.innerText = "Analisando leitores similares...";
    btnGerar.disabled = true;

    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/livros/clustering/usuario/${usuarioLogadoId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if(response.ok) {
            const texto = await response.text();
            txtRecomendacao.innerText = texto;
        } else {
            txtRecomendacao.innerText = "Não foi possível gerar a recomendação no momento.";
        }
    } catch(e) {
        console.error('Recomendação falhou', e);
        txtRecomendacao.innerText = "Falha de conexão com os serviços de inteligência artificial.";
    } finally {
        btnGerar.disabled = false;
    }
}

function configurarModalMeusEmprestimos() {
    const btn = document.getElementById('btnMeusEmprestimos');
    const modal = document.getElementById('modalMeusEmprestimos');
    const btnClose = document.getElementById('btnCloseMeusEmprestimos');

    btn.addEventListener('click', async () => {
        await carregarMeusEmprestimos();
        modal.style.transform = 'translate(-50%, -50%) scale(1)';
    });

    btnClose.addEventListener('click', () => {
        modal.style.transform = 'translate(-50%, -50%) scale(0)';
    });
}

async function carregarMeusEmprestimos() {
    if(!usuarioLogadoId) return;
    const token = localStorage.getItem('jwtToken');
    const lista = document.getElementById('listaMeusEmprestimos');
    lista.innerHTML = '<p>Carregando...</p>';

    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/emprestimos/usuario/${usuarioLogadoId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if(response.ok) {
            const historico = await response.json();
            const emprestimos = historico.filter(emp => emp.status === 'ATIVO' || emp.status === 'ATRASADO');

            if(emprestimos.length === 0) {
                lista.innerHTML = '<p style="color: var(--text-muted);">N nenhum empréstimo ativo no momento.</p>';
                return;
            }
            
            lista.innerHTML = '';
            emprestimos.forEach(emp => {
                const isAtrasado = emp.status === 'ATRASADO';
                const div = document.createElement('div');
                div.style = 'border: 1px solid var(--border-color); padding: 1rem; border-radius: 0.5rem; display: flex; justify-content: space-between; align-items: center;';
                div.innerHTML = `
                    <div>
                        <strong style="color: var(--text-color);">${emp.livro.titulo}</strong>
                        <p style="font-size: 0.8rem; color: ${isAtrasado ? '#EF4444' : 'var(--text-muted)'}; margin-top: 0.25rem;">Devolver até: ${new Date(emp.dataDevolucaoPrevista).toLocaleDateString()} ${isAtrasado ? '(ATRASADO!)' : ''}</p>
                    </div>
                    ${emp.renovacoesFeitas < 1 && !isAtrasado ? `<button class="btn-primary" onclick="renovarEmprestimo(${emp.id})" style="width: auto; padding: 0.5rem 1rem; font-size: 0.75rem; flex-shrink: 0;">Renovar</button>` : '<span style="font-size: 0.75rem; color: var(--text-muted); flex-shrink: 0;">Não renovável</span>'}
                `;
                lista.appendChild(div);
            });
        }
    } catch(e) {
        lista.innerHTML = '<p style="color: red;">Erro ao carregar empréstimos.</p>';
    }
}

async function renovarEmprestimo(emprestimoId) {
    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/emprestimos/${emprestimoId}/renovar`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if(response.ok) {
            showToast('Renovado com sucesso!', 'success');
            carregarMeusEmprestimos(); // Recarrega
        } else {
            await handleApiError(response, 'Falha ao renovar. Verifique se o livro já foi renovado ou há atrasos.');
        }
    } catch(e) {
        showToast('Erro ao comunicar com servidor.', 'error');
    }
}

// Notificações
async function carregarNotificacoes() {
    if(!usuarioLogadoId) return;
    const token = localStorage.getItem('jwtToken');
    const btn = document.getElementById('btnNotificacoes');
    const dropdown = document.getElementById('notificacoesDropdown');
    const lista = document.getElementById('listaNotificacoes');

    btn.onclick = () => {
        dropdown.style.transform = dropdown.style.transform.includes('scale(1)') ? 'scale(0)' : 'scale(1)';
    };

    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/notificacoes/usuario/${usuarioLogadoId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if(response.ok) {
            const notifs = await response.json();
            const nNaoLidas = notifs.filter(n => !n.lida);
            
            if(nNaoLidas.length > 0) {
                btn.innerHTML = `<i class="ph ph-bell-ringing"></i><span id="notifBadge" style="background: red; border-radius: 50%; width: 10px; height: 10px; position: absolute; top: 0; right: 0;"></span>`;
            } else {
                btn.innerHTML = `<i class="ph ph-bell"></i>`;
            }

            lista.innerHTML = '';
            if(notifs.length === 0) {
                lista.innerHTML = '<span style="color: var(--text-muted);">Nenhuma notificação.</span>';
                return;
            }

            notifs.forEach(n => {
                const div = document.createElement('div');
                div.style = `padding: 0.75rem; border-radius: 0.5rem; background: ${n.lida ? 'var(--bg-color)' : 'rgba(182, 255, 46, 0.1)'}; border: 1px solid var(--border-color); display: flex; flex-direction: column; gap: 0.5rem;`;
                if(!n.lida) {
                    div.style.borderLeft = '3px solid var(--primary-color)';
                }

                let icone = '<i class="ph ph-info" style="color: var(--primary-color); font-size: 1.2rem;"></i>';
                if (n.mensagem.includes("Bem-vindo")) icone = '<i class="ph ph-hand-waving" style="color: #F59E0B; font-size: 1.2rem;"></i>';
                else if (n.mensagem.includes("Reserva") || n.mensagem.includes("reservado")) icone = '<i class="ph ph-books" style="color: var(--primary-color); font-size: 1.2rem;"></i>';
                else if (n.mensagem.includes("Atraso") || n.mensagem.includes("vence")) icone = '<i class="ph ph-warning" style="color: #EF4444; font-size: 1.2rem;"></i>';

                div.innerHTML = `
                    <div style="display: flex; gap: 0.75rem; align-items: flex-start;">
                        <div style="margin-top: 2px;">${icone}</div>
                        <div>
                            <strong>${n.titulo || 'Aviso'}</strong>
                            <p style="font-size:0.8rem; margin-top:0.25rem;">${n.mensagem}</p>
                        </div>
                    </div>
                    <button id="btnNotif-${n.id}" class="btn-primary" style="width: auto; align-self: flex-end; padding: 0.3rem 0.8rem; font-size: 0.7rem;" ${n.lida ? 'disabled' : ''}>
                        ${n.lida ? 'Visto' : 'Marcar como lida'}
                    </button>
                `;
                
                lista.appendChild(div);

                const btnNotif = div.querySelector(`#btnNotif-${n.id}`);
                btnNotif.onclick = async (e) => {
                    e.stopPropagation();
                    if(!n.lida) {
                        // Desativa instantaneamente para evitar cliques duplos
                        btnNotif.disabled = true;
                        btnNotif.innerText = 'Visto';
                        div.style.background = 'var(--bg-color)';
                        div.style.borderLeft = '1px solid var(--border-color)';
                        n.lida = true; // Atualiza o estado local

                        await fetch(`https://bibliotech-api-e9wg.onrender.com/notificacoes/${n.id}/ler`, {
                            method: 'PUT',
                            headers: { 'Authorization': `Bearer ${token}` }
                        });

                        // Reavalia a bolinha vermelha
                        const aindaTemNaoLida = notifs.some(notif => !notif.lida);
                        if (!aindaTemNaoLida) {
                            btn.innerHTML = `<i class="ph ph-bell"></i>`;
                        }
                    }
                };
            });
        }
    } catch(e) {
        console.error('Erro ao carregar notificações', e);
    }
}
