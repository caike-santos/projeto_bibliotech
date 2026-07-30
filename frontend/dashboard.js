function escapeHTML(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

document.addEventListener('DOMContentLoaded', () => {
    verificarAcesso();
    carregarDados();
    setupAutocomplete();
    
    document.getElementById('btnSair').addEventListener('click', () => {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('userRole');
        localStorage.removeItem('userTipo');
        localStorage.removeItem('userId');
        localStorage.removeItem('userName');
        window.location.href = 'index.html';
    });
});

function verificarAcesso() {
    const token = localStorage.getItem('jwtToken');
    const role = localStorage.getItem('userRole');
    if (!token) {
        showToast('Acesso negado. Faça login.', 'error');
        setTimeout(() => { window.location.href = 'index.html'; }, 2000);
        return;
    }
    
    // Se não for Admin/Bibliotecário, escondemos as abas de gestão por segurança (fallback seguro)
    if (role !== 'ADMIN' && role !== 'BIBLIOTECARIO') {
        const navBalcao = document.getElementById('navBalcao');
        const navReservas = document.getElementById('navReservas');
        const navAcervo = document.getElementById('navAcervo');
        const navUsuarios = document.getElementById('navUsuarios');
        const navEmprestimos = document.getElementById('navEmprestimos');
        
        if(navBalcao) navBalcao.style.display = 'none';
        if(navReservas) navReservas.style.display = 'none';
        if(navAcervo) navAcervo.style.display = 'none';
        if(navUsuarios) navUsuarios.style.display = 'none';
        if(navEmprestimos) navEmprestimos.style.display = 'none';
    }
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    
    document.getElementById(sectionId).classList.add('active');
    event.currentTarget.classList.add('active');
}

// Lógica de Modais
function fecharModais() {
    document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
    document.getElementById('modalOverlay').classList.remove('active');
}

function abrirModalCadastroLivro() {
    fecharModais();
    // Limpa os campos do modal de IA
    document.getElementById('inputIsbn').value = '';
    document.getElementById('inputQuantidade').value = '1';
    const tituloHint = document.getElementById('inputTituloHint');
    if (tituloHint) tituloHint.value = '';
    const autorHint = document.getElementById('inputAutorHint');
    if (autorHint) autorHint.value = '';

    document.getElementById('modalOverlay').classList.add('active');
    document.getElementById('modalLivroISBN').classList.add('active');
}

function abrirModalCadastroUsuario() {
    fecharModais();
    // Limpa o form
    document.getElementById('cadUserNome').value = '';
    document.getElementById('cadUserEmail').value = '';
    document.getElementById('cadUserSenha').value = '';
    document.getElementById('cadUserTipo').value = 'LEITOR';
    
    document.getElementById('modalOverlay').classList.add('active');
    document.getElementById('modalCadastroUsuario').classList.add('active');
}

// Variáveis Globais de Dados
let livros = [];
let usuarios = [];
let emprestimos = [];
let reservas = [];

async function carregarDados() {
    const token = localStorage.getItem('jwtToken');
    const headers = { 'Authorization': `Bearer ${token}` };

    const trLoaderAcervo = '<tr><td colspan="6"><div style="display:flex; justify-content:center; padding:1rem;"><div class="loader-spinner"></div></div></td></tr>';
    const trLoaderUsuarios = '<tr><td colspan="5"><div style="display:flex; justify-content:center; padding:1rem;"><div class="loader-spinner"></div></div></td></tr>';
    const trLoaderEmprestimos = '<tr><td colspan="6"><div style="display:flex; justify-content:center; padding:1rem;"><div class="loader-spinner"></div></div></td></tr>';
    const trLoaderReservas = '<tr><td colspan="6"><div style="display:flex; justify-content:center; padding:1rem;"><div class="loader-spinner"></div></div></td></tr>';

    const tabAcervo = document.getElementById('tabelaAcervo');
    const tabUsuarios = document.getElementById('tabelaUsuarios');
    const tabEmprestimos = document.getElementById('tabelaEmprestimos');
    const tabReservas = document.getElementById('tabelaReservas');

    if(tabAcervo) tabAcervo.innerHTML = trLoaderAcervo;
    if(tabUsuarios) tabUsuarios.innerHTML = trLoaderUsuarios;
    if(tabEmprestimos) tabEmprestimos.innerHTML = trLoaderEmprestimos;
    if(tabReservas) tabReservas.innerHTML = trLoaderReservas;

    try {
        const role = localStorage.getItem('userRole');
        
        if (role === 'ADMIN' || role === 'BIBLIOTECARIO') {
            // FLUXO GESTÃO (Admin/Bibliotecário)
            const resLivros = await fetch(API_BASE_URL + '/livros?todos=true&size=500', { headers, skipLoader: true });
            if (resLivros.ok) {
                const data = await resLivros.json();
                livros = data.content || data; // Trata Pageable ou List
                renderizarAcervo();
            }

            const resUsuarios = await fetch(API_BASE_URL + '/usuarios', { headers, skipLoader: true });
            if (resUsuarios.ok) {
                usuarios = await resUsuarios.json();
                renderizarUsuarios();
            }

            const resEmprestimos = await fetch(API_BASE_URL + '/emprestimos', { headers, skipLoader: true });
            if (resEmprestimos.ok) {
                emprestimos = await resEmprestimos.json();
                renderizarEmprestimos();
            }

            const resReservas = await fetch(API_BASE_URL + '/reservas', { headers, skipLoader: true });
            if (resReservas.ok) {
                reservas = await resReservas.json();
                renderizarReservas();
            }
        } else {
            // FLUXO DO LEITOR (Só busca o seu próprio histórico) - FALLBACK SEGURO
            const resUser = await fetch(API_BASE_URL + '/usuarios/me', { headers, skipLoader: true });
            if (resUser.ok) {
                const user = await resUser.json();
                const resEmp = await fetch(API_BASE_URL + `/emprestimos/usuario/${user.id}`, { headers, skipLoader: true });
                if (resEmp.ok) {
                    emprestimos = await resEmp.json();
                }
            }
        }

        atualizarDashboard();

    } catch (e) {
        console.error('Erro ao carregar dados:', e);
    }
}

let chartInstancia = null;
function atualizarDashboard() {
    const role = localStorage.getItem('userRole');
    const contagemGeneros = {};

    if (role === 'ADMIN' || role === 'BIBLIOTECARIO') {
        // MODO ADMIN GESTÃO
        document.getElementById('statLivros').innerText = livros.length;
        
        const leitores = usuarios.filter(u => u.tipo === 'LEITOR');
        const equipe = usuarios.filter(u => u.tipo === 'ADMIN' || u.tipo === 'BIBLIOTECARIO');
        
        document.getElementById('statUsuarios').innerText = leitores.length;
        
        const statEquipe = document.getElementById('statEquipe');
        if (statEquipe) statEquipe.innerText = equipe.length;
        
        document.getElementById('statEmprestimos').innerText = emprestimos.length;

        livros.forEach(l => {
            const genero = l.generoPrincipal || 'Outros';
            contagemGeneros[genero] = (contagemGeneros[genero] || 0) + 1;
        });
    } else {
        // MODO LEITOR (Analytics Pessoal)
        document.getElementById('labelStat1').innerText = "Livros Lidos";
        document.getElementById('labelStat2').innerText = "Empréstimos Ativos";
        document.getElementById('labelStat3').innerText = "Atrasos no Histórico";
        document.getElementById('labelStat4').innerText = "Total em Multas (R$)";
        document.getElementById('labelChart').innerText = "Seus Gêneros Favoritos";

        const lidos = emprestimos.filter(e => e.status === 'DEVOLVIDO');
        const ativos = emprestimos.filter(e => e.status !== 'DEVOLVIDO' && e.status !== 'CANCELADO');
        const atrasados = emprestimos.filter(e => e.status === 'ATRASADO' || (e.dataDevolucaoReal && new Date(e.dataDevolucaoReal) > new Date(e.dataDevolucaoPrevista)));
        const multas = emprestimos.reduce((acc, curr) => acc + (curr.valorMulta || 0), 0);

        document.getElementById('statLivros').innerText = lidos.length;
        document.getElementById('statUsuarios').innerText = ativos.length;
        
        const statEquipe = document.getElementById('statEquipe');
        if(statEquipe) statEquipe.innerText = atrasados.length;
        
        document.getElementById('statEmprestimos').innerText = multas.toFixed(2).replace('.', ',');

        emprestimos.forEach(e => {
            if (e.livro && e.livro.generoPrincipal) {
                const genero = e.livro.generoPrincipal;
                contagemGeneros[genero] = (contagemGeneros[genero] || 0) + 1;
            }
        });
    }

    const ctx = document.getElementById('chartGeneros').getContext('2d');
    if(chartInstancia) chartInstancia.destroy();
    
    document.getElementById('loaderChart').style.display = 'none';
    document.getElementById('chartGeneros').style.display = 'block';

    chartInstancia = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(contagemGeneros),
            datasets: [{
                data: Object.values(contagemGeneros),
                backgroundColor: ['#B6FF2E', '#064E3B', '#374151', '#F8E7C9', '#9CA3AF', '#3B82F6', '#EF4444', '#F59E0B']
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'right', labels: { color: 'var(--text-color)' } } }
        }
    });
}

// ---------------- ACERVO (IA) ---------------- //
function renderizarAcervo() {
    const tbody = document.getElementById('tabelaAcervo');
    tbody.innerHTML = '';
    livros.forEach(l => {
        tbody.innerHTML += `
            <tr>
                <td data-label="ID">${l.id}</td>
                <td data-label="ISBN">${l.isbn || 'N/A'}</td>
                <td data-label="Título">
                    ${l.titulo} 
                    ${!l.ativo ? '<span style="color: var(--error-color, #ef4444); font-size: 0.75rem; font-weight: bold; margin-left: 0.5rem;">(Inativo)</span>' : ''}
                </td>
                <td data-label="Autor">${l.autor}</td>
                <td data-label="Estoque">${l.quantidadeDisponivel}/${l.quantidadeTotal}</td>
                <td data-label="Ações">
                    <div style="display: flex; gap: 0.5rem; justify-content: flex-end; flex-wrap: wrap;">
                        <button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: var(--secondary-color, #10B981);" onclick="abrirVisualizacaoLivro(${l.id})">Ver</button>
                        <button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: #3B82F6;" onclick="abrirEdicaoLivro(${l.id})">Editar</button>
                        <button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: var(--warning-color);" onclick="inativarLivro(${l.id})">Inativar</button>
                        <button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: var(--error-color, #ef4444);" onclick="excluirLivroDefinitivo(${l.id})">Excluir</button>
                    </div>
                </td>
            </tr>
        `;
    });
}

let livroSendoRevisado = null;

async function buscarEcadastrarLivro() {
    const isbn = document.getElementById('inputIsbn').value.trim();
    const qtd = document.getElementById('inputQuantidade').value || 1;
    const tituloHint = document.getElementById('inputTituloHint') ? document.getElementById('inputTituloHint').value.trim() : '';
    const autorHint = document.getElementById('inputAutorHint') ? document.getElementById('inputAutorHint').value.trim() : '';

    const btn = document.getElementById('btnBuscarIsbn');
    const token = localStorage.getItem('jwtToken');
    
    if(!isbn) {
        showToast('Digite um ISBN.', 'warning');
        return;
    }

    btn.innerText = 'Processando (IA)...';
    btn.disabled = true;

    try {
        let url = `/livros/cadastrar-por-isbn/${isbn}?quantidade=${qtd}`;
        if (tituloHint) url += `&tituloHint=${encodeURIComponent(tituloHint)}`;
        if (autorHint) url += `&autorHint=${encodeURIComponent(autorHint)}`;

        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const livroCadastrado = await response.json();
            
            // Abre o modal universal de edição para a revisão final, 
            // repassando os dados que a IA encontrou (mas que AINDA NÒO estão no banco)
            abrirEdicaoLivro(null, livroCadastrado);
            document.getElementById('tituloModalLivro').innerText = 'IA: Revise e Salve';
            
            showToast('Livro importado! Revise os dados e clique em Salvar.', 'info');

        } else {
            await handleApiError(response, 'Falha ao catalogar o livro. Verifique o ISBN.');
        }
    } catch (e) {
        console.error(e);
        showToast('Erro de comunicação.', 'error');
    } finally {
        btn.innerText = 'Buscar e Validar via IA';
        btn.disabled = false;
    }
}

async function inativarLivro(id) {
    const confirmed = await showCustomConfirm('Atenção', 'Tem certeza que deseja inativar este livro? (Ele sairá do catálogo, mas o ISBN continuará reservado)', 'warning');
    if(!confirmed) return;
    const token = localStorage.getItem('jwtToken');
    try {
        await fetch(`/livros/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
        showToast('Livro inativado com sucesso.', 'info');
        carregarDados();
    } catch(e) {
        showToast('Erro ao inativar livro.', 'error');
    }
}

async function excluirLivroDefinitivo(id) {
    const confirmed = await showCustomConfirm('Cuidado', 'Tem certeza que deseja EXCLUIR DEFINITIVAMENTE este livro? O ISBN será liberado. Esta ação não pode ser desfeita.', 'danger');
    if(!confirmed) return;
    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(`/livros/hard/${id}`, { 
            method: 'DELETE', 
            headers: { 'Authorization': `Bearer ${token}` } 
        });

        if (response.ok) {
            showToast('Livro excluído definitivamente com sucesso!', 'success');
            carregarDados();
        } else {
            const errorData = await response.json().catch(() => ({}));
            showToast(errorData.message || 'Erro: O livro não pode ser excluído pois possui histórico de empréstimos.', 'error');
        }
    } catch(e) {
        showToast('Erro ao se conectar com o servidor para exclusão.', 'error');
    }
}


// ---------------- MODAL DE VISUALIZA!ÒO DE LIVRO ---------------- //
function abrirVisualizacaoLivro(id) {
    const livro = livros.find(l => l.id === id);
    if (!livro) return;

    document.getElementById('visTitulo').innerText = livro.titulo || 'Sem Título';
    document.getElementById('visAutor').innerText = livro.autor || 'Desconhecido';
    document.getElementById('visEditora').innerText = livro.editora || 'Desconhecida';
    document.getElementById('visAno').innerText = livro.ano || 'N/A';
    document.getElementById('visIsbn').innerText = livro.isbn || 'N/A';
    document.getElementById('visGenero').innerText = livro.generoPrincipal || 'N/A';
    document.getElementById('visSinopse').innerText = livro.sinopse || 'Nenhuma sinopse disponível.';
    document.getElementById('visEstoque').innerText = `${livro.quantidadeDisponivel} / ${livro.quantidadeTotal}`;
    
    const capaDiv = document.getElementById('visCapa');
    if (livro.capaUrl) {
        capaDiv.style.backgroundImage = `url('${livro.capaUrl}')`;
    } else {
        capaDiv.style.backgroundImage = 'none';
        capaDiv.style.backgroundColor = '#ccc';
    }

    const tagsDiv = document.getElementById('visTags');
    tagsDiv.innerHTML = '';
    if (livro.tagsSecundarias && livro.tagsSecundarias.length > 0) {
        livro.tagsSecundarias.forEach(t => {
            tagsDiv.innerHTML += `<span style="font-size: 0.7rem; padding: 0.2rem 0.5rem; background: var(--border-color); border-radius: 1rem; color: var(--text-color);">${t.nome}</span>`;
        });
    } else {
        tagsDiv.innerHTML = '<span style="font-size: 0.7rem; color: var(--text-muted);">Nenhuma tag</span>';
    }

    fecharModais();
    document.getElementById('modalOverlay').classList.add('active');
    document.getElementById('modalVisualizarLivro').classList.add('active');
}

let livroParaEdicao = null;

function abrirEdicaoLivro(id, livroIA = null) {
    if (id === null) {
        if (livroIA) {
            // Modo Revisão de IA (Preenche com dados recebidos mas sem ID)
            livroParaEdicao = { tagsSecundarias: livroIA.tagsSecundarias || [] };
            document.getElementById('editLivroIsbn').value = livroIA.isbn || '';
            document.getElementById('editLivroTitulo').value = livroIA.titulo || '';
            document.getElementById('editLivroAutor').value = livroIA.autor || '';
            document.getElementById('editLivroEditora').value = livroIA.editora || '';
            document.getElementById('editLivroAno').value = livroIA.ano || '';
            document.getElementById('editLivroCapaUrl').value = livroIA.capaUrl || '';
            document.getElementById('editLivroGenero').value = livroIA.generoPrincipal || '';
            document.getElementById('editLivroTags').value = (livroIA.tagsSecundarias || []).map(t => t.nome).join(', ');
            document.getElementById('editLivroSinopse').value = livroIA.sinopse || '';
            document.getElementById('editLivroQtdTotal').value = livroIA.quantidadeTotal || 1;
            document.getElementById('editLivroQtdDisponivel').value = livroIA.quantidadeDisponivel || 1;
        } else {
            // Modo Cadastro Manual 100% Branco
            livroParaEdicao = { tagsSecundarias: [] };
            document.getElementById('tituloModalLivro').innerText = 'Cadastrar Manualmente';
            document.getElementById('editLivroIsbn').value = '';
            document.getElementById('editLivroTitulo').value = '';
            document.getElementById('editLivroAutor').value = '';
            document.getElementById('editLivroEditora').value = '';
            document.getElementById('editLivroAno').value = '';
            document.getElementById('editLivroCapaUrl').value = '';
            document.getElementById('editLivroGenero').value = '';
            document.getElementById('editLivroTags').value = '';
            document.getElementById('editLivroSinopse').value = '';
            document.getElementById('editLivroQtdTotal').value = 1;
            document.getElementById('editLivroQtdDisponivel').value = 1;
        }
        document.getElementById('editLivroId').value = '';
    } else {
        // Modo Edição (Livro já existe no banco)
        livroParaEdicao = livros.find(l => l.id === id);
        if(!livroParaEdicao) return;
        document.getElementById('tituloModalLivro').innerText = 'Editar Livro';
        document.getElementById('editLivroId').value = livroParaEdicao.id;
        document.getElementById('editLivroIsbn').value = livroParaEdicao.isbn || '';
        document.getElementById('editLivroTitulo').value = livroParaEdicao.titulo || '';
        document.getElementById('editLivroAutor').value = livroParaEdicao.autor || '';
        document.getElementById('editLivroEditora').value = livroParaEdicao.editora || '';
        document.getElementById('editLivroAno').value = livroParaEdicao.ano || '';
        document.getElementById('editLivroCapaUrl').value = livroParaEdicao.capaUrl || '';
        document.getElementById('editLivroGenero').value = livroParaEdicao.generoPrincipal || '';
        document.getElementById('editLivroTags').value = (livroParaEdicao.tagsSecundarias || []).map(t => t.nome).join(', ');
        document.getElementById('editLivroSinopse').value = livroParaEdicao.sinopse || '';
        document.getElementById('editLivroQtdTotal').value = livroParaEdicao.quantidadeTotal !== undefined ? livroParaEdicao.quantidadeTotal : '';
        document.getElementById('editLivroQtdDisponivel').value = livroParaEdicao.quantidadeDisponivel !== undefined ? livroParaEdicao.quantidadeDisponivel : '';
    }

    fecharModais();
    document.getElementById('modalOverlay').classList.add('active');
    document.getElementById('modalEditarLivro').classList.add('active');
}

async function salvarEdicaoLivro() {
    if(!livroParaEdicao) return;

    const id = document.getElementById('editLivroId').value;
    const tagsStr = document.getElementById('editLivroTags').value;
    const tagsArray = tagsStr ? tagsStr.split(',').map(t => ({ nome: t.trim() })).filter(t => t.nome !== '') : [];

    const qtdTotal = document.getElementById('editLivroQtdTotal').value ? parseInt(document.getElementById('editLivroQtdTotal').value) : 0;
    const qtdDisp = document.getElementById('editLivroQtdDisponivel').value ? parseInt(document.getElementById('editLivroQtdDisponivel').value) : 0;

    if (qtdDisp > qtdTotal) {
        showToast('A quantidade disponível não pode ser maior que a quantidade total.', 'warning');
        return;
    }

    const payload = {
        ...livroParaEdicao, // Mantemos os dados originais como base (pode estar vazio se for manual)
        isbn: document.getElementById('editLivroIsbn').value.trim(),
        titulo: document.getElementById('editLivroTitulo').value.trim(),
        autor: document.getElementById('editLivroAutor').value.trim(),
        editora: document.getElementById('editLivroEditora').value.trim(),
        ano: document.getElementById('editLivroAno').value ? parseInt(document.getElementById('editLivroAno').value) : null,
        capaUrl: document.getElementById('editLivroCapaUrl').value.trim(),
        generoPrincipal: document.getElementById('editLivroGenero').value.trim(),
        sinopse: document.getElementById('editLivroSinopse').value.trim(),
        quantidadeTotal: qtdTotal,
        quantidadeDisponivel: qtdDisp,
        tagsSecundarias: tagsArray
    };

    const token = localStorage.getItem('jwtToken');
    try {
        const isNew = !id;
        const url = isNew ? `/livros` : `/livros/${id}`;
        const method = isNew ? 'POST' : 'PUT';

        const response = await fetch(url, {
            method: method,
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showToast(isNew ? 'Livro salvo com sucesso no acervo!' : 'Livro atualizado com sucesso!', 'success');
            fecharModais();
            carregarDados();
        } else {
            // Se o backend retornar status 409 (Conflict), é pq o ISBN duplicou
            if (response.status === 409) {
                showToast('Erro: Este ISBN já está cadastrado no sistema!', 'error');
            } else {
                await handleApiError(response, isNew ? 'Erro ao cadastrar.' : 'Erro ao atualizar.');
            }
        }
    } catch(e) {
        showToast('Erro de rede ao salvar edição.', 'error');
    }
}

// ---------------- USUÁRIOS ---------------- //
function renderizarUsuarios() {
    const currentUserRole = localStorage.getItem('userRole') || 'LEITOR';
    const tbody = document.getElementById('tabelaUsuarios');
    tbody.innerHTML = '';
    
    // Bibliotecário não vê admins
    const usuariosFiltrados = usuarios.filter(u => {
        if (currentUserRole === 'BIBLIOTECARIO' && u.tipo === 'ADMIN') return false;
        return true;
    });

    // Se for bibliotecário, oculta o botão de "Novo Usuário" (ou restringe)
    // Conforme pedido: "só admin tenha a função de cadastrar funcionarios e admin"
    const btnNovoUser = document.getElementById('btnNovoUsuario');
    if (btnNovoUser) {
        btnNovoUser.style.display = currentUserRole === 'ADMIN' ? 'inline-block' : 'none';
    }

    usuariosFiltrados.forEach(u => {
        // Bibliotecário não bloqueia outros bibliotecários
        let showBlockBtn = true;
        if (currentUserRole === 'BIBLIOTECARIO' && u.tipo !== 'LEITOR') showBlockBtn = false;
        
        tbody.innerHTML += `
            <tr>
                <td data-label="ID">${u.id}</td>
                <td data-label="Nome">${u.nome} <span style="font-size:0.7rem; color:var(--text-muted);">(${u.tipo || 'LEITOR'})</span></td>
                <td data-label="E-mail">${u.email}</td>
                <td data-label="Status"><span style="color: ${u.status === 'INATIVO' || !u.enabled ? 'red' : 'var(--primary-color)'}">${u.status}</span></td>
                <td data-label="Ações">
                    <div style="display: flex; gap: 0.5rem; justify-content: flex-end;">
                        ${showBlockBtn ? `<button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: red;" onclick="inativarUsuario(${u.id})">Bloquear</button>` : '<span style="font-size:0.8rem; color:var(--text-muted);">Sem permissão</span>'}
                    </div>
                </td>
            </tr>
        `;
    });
}

async function cadastrarUsuarioInterno() {
    const nome = document.getElementById('cadUserNome').value.trim();
    const email = document.getElementById('cadUserEmail').value.trim();
    const senha = document.getElementById('cadUserSenha').value.trim();
    const tipo = document.getElementById('cadUserTipo').value;
    
    if(!nome || !email || !senha) {
        showToast('Preencha todos os campos.', 'warning');
        return;
    }

    const btn = document.getElementById('btnSalvarUsuario');
    btn.innerText = 'Processando...';
    btn.disabled = true;

    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(API_BASE_URL + '/usuarios', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` // Embora a rota talvez seja pública, enviamos mesmo assim
            },
            body: JSON.stringify({ nome, email, senha, tipo, status: 'ATIVO', enabled: true })
        });

        if (response.ok) {
            showToast('Usuário cadastrado com sucesso!', 'success');
            fecharModais();
            carregarDados();
        } else {
            await handleApiError(response, 'Falha ao cadastrar.');
        }
    } catch(e) {
        showToast('Erro de conexão.', 'error');
    } finally {
        btn.innerText = 'Criar Usuário';
        btn.disabled = false;
    }
}

async function inativarUsuario(id) {
    const confirmed = await showCustomConfirm('Bloquear Usuário', 'Tem certeza que deseja inativar/bloquear este usuário?', 'warning');
    if(!confirmed) return;
    const token = localStorage.getItem('jwtToken');
    try {
        await fetch(`/usuarios/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
        showToast('Usuário inativado com sucesso.', 'info');
        carregarDados();
    } catch(e) {
        showToast('Erro ao inativar usuário.', 'error');
    }
}

// ---------------- EMPR0STIMOS ---------------- //
function renderizarEmprestimos() {
    const tbody = document.getElementById('tabelaEmprestimos');
    tbody.innerHTML = '';
    emprestimos.forEach(e => {
        const dataDev = formatarDataLocal(e.dataDevolucaoPrevista);
        tbody.innerHTML += `
            <tr>
                <td data-label="ID">${e.id}</td>
                <td data-label="Livro">${escapeHTML(e.livro.titulo)}</td>
                <td data-label="Leitor">${escapeHTML(e.usuario.nome)}</td>
                <td data-label="Devolução">${e.status === 'AGUARDANDO_RETIRADA' ? 'Buscar até ' + dataDev : dataDev}</td>
                <td data-label="Status" style="color: ${e.status === 'ATRASADO' ? 'red' : (e.status === 'AGUARDANDO_RETIRADA' ? 'var(--warning-color)' : 'inherit')}">${e.status}</td>
                <td data-label="Ações">
                    <div style="display: flex; gap: 0.5rem; justify-content: flex-end;">
                        ${e.status === 'AGUARDANDO_RETIRADA' ? 
                            `<button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: var(--success-color, #10B981);" onclick="confirmarRetirada(${e.id})">Entregar Livro</button>` : 
                            (e.status !== 'DEVOLVIDO' && e.status !== 'CANCELADO' ? 
                                `<button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto;" onclick="forcarDevolucao(${e.id})">Devolver</button>` : 
                                'Resolvido'
                            )
                        }
                    </div>
                </td>
            </tr>
        `;
    });
}

async function forcarDevolucao(id) {
    const confirmed = await showCustomConfirm('Devolução', 'Confirmar devolução deste empréstimo?', 'info');
    if(!confirmed) return;
    const token = localStorage.getItem('jwtToken');
    try {
        await fetch(`/emprestimos/${id}/devolver`, { method: 'PUT', headers: { 'Authorization': `Bearer ${token}` } });
        showToast('Devolução confirmada com sucesso.', 'success');
        carregarDados();
    } catch(e) {
        showToast('Erro ao confirmar devolução.', 'error');
    }
}

async function confirmarRetirada(id) {
    const confirmed = await showCustomConfirm('Confirmar Retirada', 'O leitor está no balcão e você entregará o livro agora? (Isso iniciará o prazo de 14 dias)', 'info');
    if(!confirmed) return;
    const token = localStorage.getItem('jwtToken');
    try {
        const res = await fetch(`/emprestimos/${id}/confirmar-retirada`, { method: 'PUT', headers: { 'Authorization': `Bearer ${token}` } });
        if(res.ok) {
            showToast('Retirada confirmada! Prazo iniciado.', 'success');
            carregarDados();
        } else {
            showToast('Erro ao confirmar retirada.', 'error');
        }
    } catch(e) {
        showToast('Erro de conexão.', 'error');
    }
}

// ---------------- RESERVAS ---------------- //
function renderizarReservas() {
    const tbody = document.getElementById('tabelaReservas');
    if (!tbody) return;
    tbody.innerHTML = '';
    
    reservas.forEach(r => {
        const dataSol = formatarDataLocal(r.dataSolicitacao);
        const podeEfetivar = r.status === 'NOTIFICADO';
        
        tbody.innerHTML += `
            <tr>
                <td data-label="ID">${r.id}</td>
                <td data-label="Livro">${escapeHTML(r.livro.titulo)}</td>
                <td data-label="Leitor">${escapeHTML(r.usuario.nome)}</td>
                <td data-label="Solicitado">${dataSol}</td>
                <td data-label="Status"><span style="color: ${podeEfetivar ? 'var(--primary-color)' : 'var(--warning-color)'}">${r.status}</span></td>
                <td data-label="Ações">
                    <div style="display: flex; gap: 0.5rem; justify-content: flex-end;">
                        ${podeEfetivar 
                            ? `<button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto;" onclick="efetivarEmprestimoDaReserva(${r.livro.id}, ${r.usuario.id})">Efetivar Empréstimo</button>` 
                            : '<span style="font-size:0.8rem; color:var(--text-muted);">Aguardando</span>'}
                    </div>
                </td>
            </tr>
        `;
    });
}

async function efetivarEmprestimoDaReserva(livroId, usuarioId) {
    const confirmed = await showCustomConfirm('Efetivar Empréstimo', 'Deseja registrar o empréstimo para este usuário que estava na fila?', 'info');
    if(!confirmed) return;
    
    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(API_BASE_URL + '/emprestimos?balcao=true', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ 
                livro: { id: parseInt(livroId) }, 
                usuario: { id: parseInt(usuarioId) } 
            })
        });

        if (response.ok) {
            showToast('Empréstimo efetivado a partir da reserva!', 'success');
            carregarDados();
        } else {
            await handleApiError(response, 'Falha ao efetivar empréstimo da reserva.');
        }
    } catch(e) {
        showToast('Erro de conexão.', 'error');
    }
}

// ---------------- BALCÒO (AUTOCOMPLETE & EMPR0STIMO) ---------------- //
function setupAutocomplete() {
    const userSearch = document.getElementById('balcaoUserSearch');
    const userDropdown = document.getElementById('balcaoUserDropdown');
    const userIdInput = document.getElementById('balcaoUserId');
    const userSelected = document.getElementById('balcaoUserSelected');

    const bookSearch = document.getElementById('balcaoBookSearch');
    const bookDropdown = document.getElementById('balcaoBookDropdown');
    const bookIdInput = document.getElementById('balcaoBookId');
    const bookSelected = document.getElementById('balcaoBookSelected');

    // Fechar dropdowns ao clicar fora
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.autocomplete-container')) {
            if(userDropdown) userDropdown.style.display = 'none';
            if(bookDropdown) bookDropdown.style.display = 'none';
        }
    });

    if(!userSearch) return;

    // Autocomplete Leitor
    userSearch.addEventListener('input', (e) => {
        const termo = e.target.value.toLowerCase();
        userIdInput.value = ''; // reseta seleção
        userSelected.style.display = 'none';
        
        if (termo.length < 2) {
            userDropdown.style.display = 'none';
            return;
        }

        const filtrados = usuarios.filter(u => u.enabled && (u.tipo === 'LEITOR' || !u.tipo) && (u.email.toLowerCase().includes(termo) || u.nome.toLowerCase().includes(termo)));
        
        userDropdown.innerHTML = '';
        if (filtrados.length === 0) {
            userDropdown.innerHTML = '<div class="autocomplete-item" style="color:var(--text-muted)">Nenhum leitor encontrado</div>';
        } else {
            filtrados.forEach(u => {
                const item = document.createElement('div');
                item.className = 'autocomplete-item';
                item.innerHTML = `<strong>${u.nome}</strong><br><small style="color:var(--text-muted)">${u.email}</small>`;
                item.onclick = () => {
                    userSearch.value = u.nome;
                    userIdInput.value = u.id;
                    userDropdown.style.display = 'none';
                    userSelected.style.display = 'block';
                };
                userDropdown.appendChild(item);
            });
        }
        userDropdown.style.display = 'block';
    });

    // Autocomplete Livro
    bookSearch.addEventListener('input', (e) => {
        const termo = e.target.value.toLowerCase();
        bookIdInput.value = ''; // reseta seleção
        bookSelected.style.display = 'none';
        
        if (termo.length < 2) {
            bookDropdown.style.display = 'none';
            return;
        }

        const filtrados = livros.filter(l => l.quantidadeDisponivel > 0 && (l.isbn.toLowerCase().includes(termo) || l.titulo.toLowerCase().includes(termo)));
        
        bookDropdown.innerHTML = '';
        if (filtrados.length === 0) {
            bookDropdown.innerHTML = '<div class="autocomplete-item" style="color:var(--text-muted)">Nenhum livro disponível encontrado</div>';
        } else {
            filtrados.forEach(l => {
                const item = document.createElement('div');
                item.className = 'autocomplete-item';
                item.innerHTML = `<strong>${l.titulo}</strong><br><small style="color:var(--text-muted)">ISBN: ${l.isbn} | Disp: ${l.quantidadeDisponivel}</small>`;
                item.onclick = () => {
                    bookSearch.value = l.titulo;
                    bookIdInput.value = l.id;
                    bookDropdown.style.display = 'none';
                    bookSelected.style.display = 'block';
                };
                bookDropdown.appendChild(item);
            });
        }
        bookDropdown.style.display = 'block';
    });
}

async function realizarEmprestimoBalcao() {
    const usuarioId = document.getElementById('balcaoUserId').value;
    const livroId = document.getElementById('balcaoBookId').value;
    const btn = document.getElementById('btnRealizarEmprestimoBalcao');

    if (!usuarioId || !livroId) {
        showToast('Selecione um Leitor e um Livro usando as sugestões.', 'warning');
        return;
    }

    btn.innerText = 'Processando...';
    btn.disabled = true;

    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(API_BASE_URL + '/emprestimos?balcao=true', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ 
                livro: { id: parseInt(livroId) }, 
                usuario: { id: parseInt(usuarioId) } 
            })
        });

        if (response.ok) {
            showToast('Empréstimo registrado com sucesso!', 'success');
            
            // Limpa o formulário
            document.getElementById('balcaoUserSearch').value = '';
            document.getElementById('balcaoUserId').value = '';
            document.getElementById('balcaoUserSelected').style.display = 'none';
            
            document.getElementById('balcaoBookSearch').value = '';
            document.getElementById('balcaoBookId').value = '';
            document.getElementById('balcaoBookSelected').style.display = 'none';

            carregarDados(); // Atualiza listas globais
        } else {
            await handleApiError(response, 'Falha ao registrar empréstimo no balcão.');
        }
    } catch(e) {
        showToast('Erro de conexão.', 'error');
    } finally {
        btn.innerText = 'Confirmar Empréstimo';
        btn.disabled = false;
    }
}


