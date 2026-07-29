let dadosUsuarioGlobal = null;

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        window.location.href = 'index.html';
        return;
    }

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const email = payload.sub;
        
        // Exibe nome/email básicos enquanto carrega
        document.getElementById('perfilEmail').innerText = email;
        const nomeSalvo = localStorage.getItem('userName');
        if (nomeSalvo) document.getElementById('perfilNome').innerText = nomeSalvo;

        // Buscar ID do usuário
        const resUser = await fetch('https://bibliotech-api-e9wg.onrender.com/usuarios', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (!resUser.ok) throw new Error('Falha ao buscar usuários');
        const usuarios = await resUser.json();
        const user = usuarios.find(u => u.email === email);
        
        if (user) {
            dadosUsuarioGlobal = user;
            document.getElementById('perfilNome').innerText = user.nome;
            localStorage.setItem('userName', user.nome);
            await carregarGamificacao(user.id, token);
            await carregarHistorico(user.id, token);
            await carregarReservas(user.id, token);
        }

    } catch (e) {
        console.error(e);
        showToast('Erro ao carregar os dados do perfil.', 'error');
    }
    
    configurarEdicaoPerfil();
});

function configurarEdicaoPerfil() {
    const modal = document.getElementById('modalEditarPerfil');
    const btnAbrir = document.getElementById('btnAbrirEdicao');
    const btnClose = document.getElementById('btnCloseEdicao');
    const btnSalvar = document.getElementById('btnSalvarEdicao');
    
    if (!modal || !btnAbrir || !btnClose || !btnSalvar) return;

    btnAbrir.addEventListener('click', () => {
        if(dadosUsuarioGlobal) {
            document.getElementById('editNome').value = dadosUsuarioGlobal.nome;
            document.getElementById('editEmail').value = dadosUsuarioGlobal.email;
            document.getElementById('editSenha').value = '';
            modal.style.transform = 'translate(-50%, -50%) scale(1)';
        }
    });

    btnClose.addEventListener('click', () => {
        modal.style.transform = 'translate(-50%, -50%) scale(0)';
    });

    btnSalvar.addEventListener('click', async () => {
        const novoNome = document.getElementById('editNome').value.trim();
        const novoEmail = document.getElementById('editEmail').value.trim();
        const novaSenha = document.getElementById('editSenha').value.trim();

        if(!novoNome || !novoEmail) {
            showToast('Nome e e-mail são obrigatórios!', 'warning');
            return;
        }

        const payload = {
            nome: novoNome,
            email: novoEmail,
            tipo: dadosUsuarioGlobal.tipo,
            status: dadosUsuarioGlobal.status
        };

        if (novaSenha) {
            payload.senha = novaSenha;
        }

        btnSalvar.disabled = true;
        btnSalvar.innerText = "Salvando...";

        try {
            const token = localStorage.getItem('jwtToken');
            const res = await fetch(`https://bibliotech-api-e9wg.onrender.com/usuarios/${dadosUsuarioGlobal.id}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                showToast('Perfil atualizado com sucesso!', 'success');
                const credenciaisMudaram = (novoEmail !== dadosUsuarioGlobal.email) || novaSenha;
                
                document.getElementById('perfilNome').innerText = novoNome;
                document.getElementById('perfilEmail').innerText = novoEmail;
                localStorage.setItem('userName', novoNome);
                dadosUsuarioGlobal.nome = novoNome;
                dadosUsuarioGlobal.email = novoEmail;
                modal.style.transform = 'translate(-50%, -50%) scale(0)';
                
                if (credenciaisMudaram) {
                    showToast('Credenciais alteradas. Faça login novamente.', 'info');
                    setTimeout(() => {
                        localStorage.removeItem('jwtToken');
                        window.location.href = 'index.html';
                    }, 2500);
                }
            } else {
                showToast('Erro ao atualizar perfil.', 'error');
            }
        } catch(e) {
            console.error(e);
            showToast('Falha na comunicação com o servidor', 'error');
        } finally {
            btnSalvar.disabled = false;
            btnSalvar.innerText = "Salvar Alterações";
        }
    });
}

async function carregarGamificacao(usuarioId, token) {
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/usuarios/${usuarioId}/gamificacao`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            const data = await response.json();
            document.getElementById('perfilSelo').innerText = `${data.selo} ${data.nivel}`;
            document.getElementById('perfilPontos').innerText = data.totalEmprestimos;
        }
    } catch(e) {
        console.error('Erro na gamificação', e);
    }
}

async function carregarHistorico(usuarioId, token) {
    const corpoHistorico = document.getElementById('tabelaCorpo');
    const corpoAtivos = document.getElementById('tabelaMeusEmprestimos');
    
    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/emprestimos/usuario/${usuarioId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            const historico = await response.json();
            if(corpoHistorico) corpoHistorico.innerHTML = '';
            if(corpoAtivos) corpoAtivos.innerHTML = '';

            const ativos = historico.filter(e => e.status !== 'DEVOLVIDO');
            const inativos = historico.filter(e => e.status === 'DEVOLVIDO');

            if (ativos.length === 0 && corpoAtivos) {
                corpoAtivos.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">Nenhum empréstimo ativo no momento.</td></tr>';
            }

            if (inativos.length === 0 && corpoHistorico) {
                corpoHistorico.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">Nenhum empréstimo encontrado no seu histórico.</td></tr>';
            }

            ativos.forEach(emp => {
                if(!corpoAtivos) return;
                const isAtrasado = emp.status === 'ATRASADO';
                const isAguardando = emp.status === 'AGUARDANDO_RETIRADA';
                let badgeClass = 'status-ativo';
                if (isAtrasado) badgeClass = 'status-atrasado';
                else if (isAguardando) badgeClass = 'status-atrasado'; // Vamos usar a cor laranja/atrasado por enquanto, ou criar uma nova classe. O status-atrasado é vermelho, melhor colocar um style inline ou classe nova

                let devolucaoTexto = new Date(emp.dataDevolucaoPrevista).toLocaleDateString();
                if (isAguardando) {
                    devolucaoTexto = `Buscar até: ${devolucaoTexto}`;
                }

                let acoes = '-';
                if (emp.status === 'ATIVO') {
                    if (emp.renovacoesFeitas === 0) {
                        acoes = `<button class="btn-primary" style="padding: 0.3rem 0.6rem; font-size: 0.75rem;" onclick="renovarEmprestimo(${emp.id})">Renovar</button>`;
                    } else {
                        acoes = '<span style="font-size: 0.75rem; color: var(--text-muted);">Já renovado</span>';
                    }
                } else if (isAtrasado) {
                    acoes = '<span style="font-size: 0.75rem; color: #EF4444;">Bloqueado</span>';
                } else if (isAguardando) {
                    acoes = '<span style="font-size: 0.75rem; color: var(--warning-color);">Retirada Pendente</span>';
                    badgeClass = ''; // Remover classe padrão pra usar style custom
                }

                const statusHtml = isAguardando ? 
                    `<span class="status-badge" style="background: rgba(234, 179, 8, 0.1); color: var(--warning-color); border-color: var(--warning-color);">AGUARDANDO RETIRADA</span>` : 
                    `<span class="status-badge ${badgeClass}">${emp.status}</span>`;

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td data-label="Livro"><strong>${emp.livro.titulo}</strong></td>
                    <td data-label="Data Retirada">${isAguardando ? '-' : new Date(emp.dataRetirada).toLocaleDateString()}</td>
                    <td data-label="Devolução Prevista" style="${isAtrasado ? 'color: #EF4444; font-weight: bold;' : (isAguardando ? 'color: var(--warning-color); font-weight: bold;' : '')}">${devolucaoTexto}</td>
                    <td data-label="Status">${statusHtml}</td>
                    <td data-label="Ações">${acoes}</td>
                `;
                corpoAtivos.appendChild(tr);
            });

            inativos.forEach(emp => {
                if(!corpoHistorico) return;
                const badgeClass = 'status-devolvido';
                const devolucaoTexto = emp.dataDevolucaoReal ? `Devolvido em: ${new Date(emp.dataDevolucaoReal).toLocaleDateString()}` : new Date(emp.dataDevolucaoPrevista).toLocaleDateString();
                const multaTexto = emp.valorMulta > 0 ? `R$ ${emp.valorMulta.toFixed(2).replace('.', ',')}` : '-';

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td data-label="Livro"><strong>${emp.livro.titulo}</strong><br><span style="font-size:0.75rem; color:var(--text-muted);">${emp.livro.autor}</span></td>
                    <td data-label="Data Retirada">${new Date(emp.dataRetirada).toLocaleDateString()}</td>
                    <td data-label="Devolução">${devolucaoTexto}</td>
                    <td data-label="Multa" style="${emp.valorMulta > 0 ? 'color: #EF4444; font-weight: bold;' : ''}">${multaTexto}</td>
                    <td data-label="Status"><span class="status-badge ${badgeClass}">${emp.status}</span></td>
                `;
                corpoHistorico.appendChild(tr);
            });
        }
    } catch(e) {
        console.error('Erro no histórico', e);
        if(corpoHistorico) corpoHistorico.innerHTML = '<tr><td colspan="5" style="text-align: center; color: red;">Erro ao carregar o histórico de empréstimos.</td></tr>';
    }
}

async function carregarReservas(usuarioId, token) {
    const tbody = document.getElementById('tabelaMinhasReservas');
    if (!tbody) return;

    try {
        const response = await fetch(`https://bibliotech-api-e9wg.onrender.com/reservas/usuario/${usuarioId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const reservas = await response.json();
            tbody.innerHTML = '';

            if (reservas.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted);">Você não possui reservas no momento.</td></tr>';
                return;
            }

            for (const r of reservas) {
                let posicaoNaFila = '-';
                let badgeClass = 'status-devolvido';
                
                if (r.status === 'NOTIFICADO') {
                    badgeClass = 'status-ativo'; // Verde
                    posicaoNaFila = '<strong style="color: var(--primary-color);">LIVRO DISPONÍVEL NO BALCÃO!</strong>';
                } else if (r.status === 'AGUARDANDO') {
                    badgeClass = 'status-atrasado'; // Laranja/Atrasado style
                    
                    // Descobrir a posição real batendo na fila do livro
                    try {
                        const filaRes = await fetch(`https://bibliotech-api-e9wg.onrender.com/reservas/livro/${r.livro.id}`, { headers: { 'Authorization': `Bearer ${token}` }});
                        if (filaRes.ok) {
                            const fila = await filaRes.json();
                            const index = fila.findIndex(f => f.usuario.id === usuarioId);
                            if(index !== -1) {
                                posicaoNaFila = `<strong>${index + 1}º da fila</strong>`;
                            }
                        }
                    } catch(e) {}
                }

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td data-label="Livro"><strong>${r.livro.titulo}</strong></td>
                    <td data-label="Data Solicitação">${new Date(r.dataSolicitacao).toLocaleDateString()}</td>
                    <td data-label="Status"><span class="status-badge ${badgeClass}">${r.status}</span></td>
                    <td data-label="Posição na Fila">${posicaoNaFila}</td>
                `;
                tbody.appendChild(tr);
            }
        }
    } catch (e) {
        console.error('Erro ao carregar reservas', e);
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: red;">Erro ao carregar histórico de reservas.</td></tr>';
    }
}

window.renovarEmprestimo = async function(emprestimoId) {
    if(!confirm("Deseja renovar este empréstimo por mais 14 dias? (Regra: Apenas 1 renovação permitida por livro)")) return;
    
    const token = localStorage.getItem('jwtToken');
    try {
        const res = await fetch(`https://bibliotech-api-e9wg.onrender.com/emprestimos/${emprestimoId}/renovar`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (res.ok) {
            showToast('Empréstimo renovado com sucesso!', 'success');
            if (dadosUsuarioGlobal) {
                carregarHistorico(dadosUsuarioGlobal.id, token);
            }
        } else {
            const err = await res.text();
            let msgErro = 'Não foi possível renovar.';
            try {
                const parsed = JSON.parse(err);
                msgErro = parsed.message || parsed.error || err;
            } catch(e) {
                msgErro = err;
            }
            showToast(msgErro, 'error');
        }
    } catch(e) {
        showToast('Falha na comunicação com o servidor.', 'error');
    }
}
