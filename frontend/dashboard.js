document.addEventListener('DOMContentLoaded', () => {
    verificarAcesso();
    carregarDados();
    setupAutocomplete();
    
    document.getElementById('btnSair').addEventListener('click', () => {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('userRole');
        window.location.href = 'index.html';
    });
});

function verificarAcesso() {
    const token = localStorage.getItem('jwtToken');
    const role = localStorage.getItem('userRole');
    if (!token || (role !== 'BIBLIOTECARIO' && role !== 'ADMIN')) {
        showToast('Acesso negado. Apenas administradores e bibliotecários podem acessar este painel.', 'error');
        setTimeout(() => { window.location.href = 'index.html'; }, 2000);
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

    try {
        // Fetch Livros
        const resLivros = await fetch('https://bibliotech-api-e9wg.onrender.com/livros', { headers });
        if (resLivros.ok) {
            const data = await resLivros.json();
            livros = data.content || data; // Trata Pageable ou List
            renderizarAcervo();
        }

        // Fetch Usuários
        const resUsuarios = await fetch('https://bibliotech-api-e9wg.onrender.com/usuarios', { headers });
        if (resUsuarios.ok) {
            usuarios = await resUsuarios.json();
            renderizarUsuarios();
        }

        // Fetch Empréstimos
        const resEmprestimos = await fetch('https://bibliotech-api-e9wg.onrender.com/emprestimos', { headers });
        if (resEmprestimos.ok) {
            emprestimos = await resEmprestimos.json();
            renderizarEmprestimos();
        }

        // Fetch Reservas
        const resReservas = await fetch('https://bibliotech-api-e9wg.onrender.com/reservas', { headers });
        if (resReservas.ok) {
            reservas = await resReservas.json();
            renderizarReservas();
        }

        // Atualizar Dashboard
        atualizarDashboard();

    } catch (e) {
        console.error('Erro ao carregar dados:', e);
    }
}

// ---------------- ANALYTICS ---------------- //
let chartInstancia = null;
function atualizarDashboard() {
    // Atualizar Contadores
    document.getElementById('statLivros').innerText = livros.length;
    
    const leitores = usuarios.filter(u => u.tipo === 'LEITOR');
    const equipe = usuarios.filter(u => u.tipo === 'ADMIN' || u.tipo === 'BIBLIOTECARIO');
    
    document.getElementById('statUsuarios').innerText = leitores.length;
    
    const statEquipe = document.getElementById('statEquipe');
    if (statEquipe) statEquipe.innerText = equipe.length;
    document.getElementById('statEmprestimos').innerText = emprestimos.length;

    // Gráfico de Gêneros
    const contagemGeneros = {};
    livros.forEach(l => {
        const genero = l.generoPrincipal || 'Outros';
        contagemGeneros[genero] = (contagemGeneros[genero] || 0) + 1;
    });

    const ctx = document.getElementById('chartGeneros').getContext('2d');
    if(chartInstancia) chartInstancia.destroy();

    chartInstancia = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(contagemGeneros),
            datasets: [{
                data: Object.values(contagemGeneros),
                backgroundColor: ['#B6FF2E', '#064E3B', '#374151', '#F8E7C9', '#9CA3AF']
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'right', labels: { color: '#F8E7C9' } } }
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
                <td data-label="Título">${l.titulo}</td>
                <td data-label="Autor">${l.autor}</td>
                <td data-label="Estoque">${l.quantidadeDisponivel}/${l.quantidadeTotal}</td>
                <td data-label="Ações">
                    <div style="display: flex; gap: 0.5rem; justify-content: flex-end;">
                        <button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto; background: #3B82F6;" onclick="abrirEdicaoLivro(${l.id})">Editar</button>
                        <button class="btn-primary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto;" onclick="inativarLivro(${l.id})">Inativar</button>
                    </div>
                </td>
            </tr>
        `;
    });
}

let livroSendoRevisado = null;

async function buscarEcadastrarLivro() {
    const isbn = document.getElementById('inputIsbn').value;
    const qtd = document.getElementById('inputQuantidade').value || 1;
    const btn = document.getElementById('btnBuscarIsbn');
    const token = localStorage.getItem('jwtToken');
    
    if(!isbn) {
        showToast('Digite um ISBN.', 'warning');
        return;
    }

    btn.innerText = 'Processando (IA)...';
    btn.disabled = true;

    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/livros/cadastrar-por-isbn/${isbn}?quantidade=${qtd}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            livroSendoRevisado = await response.json();
            
            // Abre o modal de revisão
            fecharModais();
            document.getElementById('modalOverlay').classList.add('active');
            document.getElementById('modalRevisaoIA').classList.add('active');
            
            // Preenche os dados sugeridos pela IA
            document.getElementById('revLivroId').value = livroSendoRevisado.id;
            document.getElementById('revGenero').value = livroSendoRevisado.generoPrincipal || '';
            document.getElementById('revTags').value = (livroSendoRevisado.tagsSecundarias || []).map(t => t.nome).join(', ');

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

async function confirmarRevisaoIA() {
    const id = document.getElementById('revLivroId').value;
    const genero = document.getElementById('revGenero').value;
    const tagsArray = document.getElementById('revTags').value.split(',').map(t => ({ nome: t.trim() }));
    
    livroSendoRevisado.generoPrincipal = genero;
    livroSendoRevisado.tagsSecundarias = tagsArray;

    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/livros/${id}`, {
            method: 'PUT',
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(livroSendoRevisado)
        });

        if (response.ok) {
            showToast('Classificação atualizada e livro salvo com sucesso!', 'success');
            fecharModais();
            carregarDados();
        } else {
            await handleApiError(response, 'Erro ao atualizar livro.');
        }
    } catch (e) {
        showToast('Erro de rede.', 'error');
    }
}

async function inativarLivro(id) {
    if(!confirm('Tem certeza que deseja inativar este livro?')) return;
    const token = localStorage.getItem('jwtToken');
    try {
        await fetch(`https://bibliotech-api-e9wg.onrender.com/livros/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
        showToast('Livro inativado com sucesso.', 'info');
        carregarDados();
    } catch(e) {
        showToast('Erro ao inativar livro.', 'error');
    }
}

let livroParaEdicao = null;

function abrirEdicaoLivro(id) {
    livroParaEdicao = livros.find(l => l.id === id);
    if(!livroParaEdicao) return;

    document.getElementById('editLivroId').value = livroParaEdicao.id;
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

    // O PUT no backend atualiza apenas os campos cadastrais
    const payload = {
        ...livroParaEdicao, // Mantemos os dados originais como base
        titulo: document.getElementById('editLivroTitulo').value.trim(),
        autor: document.getElementById('editLivroAutor').value.trim(),
        editora: document.getElementById('editLivroEditora').value.trim(),
        ano: document.getElementById('editLivroAno').value ? parseInt(document.getElementById('editLivroAno').value) : null,
        capaUrl: document.getElementById('editLivroCapaUrl').value.trim(),
        generoPrincipal: document.getElementById('editLivroGenero').value.trim(),
        tagsSecundarias: tagsArray,
        sinopse: document.getElementById('editLivroSinopse').value.trim(),
        quantidadeTotal: qtdTotal,
        quantidadeDisponivel: qtdDisp
    };

    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/livros/${id}`, {
            method: 'PUT',
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showToast('Livro atualizado com sucesso!', 'success');
            fecharModais();
            carregarDados();
        } else {
            await handleApiError(response, 'Erro ao atualizar livro.');
        }
    } catch (e) {
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
        const response = await fetch('https://bibliotech-api-e9wg.onrender.com/usuarios', {
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
    if(!confirm('Tem certeza que deseja inativar/bloquear este usuário?')) return;
    const token = localStorage.getItem('jwtToken');
    try {
        await fetch(`https://bibliotech-api-e9wg.onrender.com/usuarios/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
        showToast('Usuário inativado com sucesso.', 'info');
        carregarDados();
    } catch(e) {
        showToast('Erro ao inativar usuário.', 'error');
    }
}

// ---------------- EMPRÉSTIMOS ---------------- //
function renderizarEmprestimos() {
    const tbody = document.getElementById('tabelaEmprestimos');
    tbody.innerHTML = '';
    emprestimos.forEach(e => {
        const dataDev = new Date(e.dataDevolucaoPrevista).toLocaleDateString();
        tbody.innerHTML += `
            <tr>
                <td data-label="ID">${e.id}</td>
                <td data-label="Livro">${e.livro.titulo}</td>
                <td data-label="Leitor">${e.usuario.nome}</td>
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
    if(!confirm('Confirmar devolução deste empréstimo?')) return;
    const token = localStorage.getItem('jwtToken');
    try {
        await fetch(`https://bibliotech-api-e9wg.onrender.com/emprestimos/${id}/devolver`, { method: 'PUT', headers: { 'Authorization': `Bearer ${token}` } });
        showToast('Devolução confirmada com sucesso.', 'success');
        carregarDados();
    } catch(e) {
        showToast('Erro ao confirmar devolução.', 'error');
    }
}

async function confirmarRetirada(id) {
    if(!confirm('O leitor está no balcão e você entregará o livro agora? (Isso iniciará o prazo de 14 dias)')) return;
    const token = localStorage.getItem('jwtToken');
    try {
        const res = await fetch(`https://bibliotech-api-e9wg.onrender.com/emprestimos/${id}/confirmar-retirada`, { method: 'PUT', headers: { 'Authorization': `Bearer ${token}` } });
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
        const dataSol = new Date(r.dataSolicitacao).toLocaleDateString();
        const podeEfetivar = r.status === 'NOTIFICADO';
        
        tbody.innerHTML += `
            <tr>
                <td data-label="ID">${r.id}</td>
                <td data-label="Livro">${r.livro.titulo}</td>
                <td data-label="Leitor">${r.usuario.nome}</td>
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
    if(!confirm('Deseja registrar o empréstimo para este usuário que estava na fila?')) return;
    
    const token = localStorage.getItem('jwtToken');
    try {
        const response = await fetch('https://bibliotech-api-e9wg.onrender.com/emprestimos', {
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

// ---------------- BALCÃO (AUTOCOMPLETE & EMPRÉSTIMO) ---------------- //
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
        const response = await fetch('https://bibliotech-api-e9wg.onrender.com/emprestimos', {
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
