document.getElementById('formLogin').addEventListener('submit', async function(event) {
    event.preventDefault();

    const email = document.getElementById('email').value;
    const senha = document.getElementById('senha').value;
    const btnSubmit = document.querySelector('#formLogin .btn-primary');
    
    const textoOriginal = btnSubmit.innerText;
    btnSubmit.innerText = 'Autenticando...';
    btnSubmit.disabled = true;

    try {
        const resposta = await fetch(API_BASE_URL + '/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, senha })
        });

        if (resposta.ok) {
            const dados = await resposta.json();
            // O cookie jwtToken será recebido automaticamente via cabeçalho Set-Cookie
            
            let role = dados.role || 'LEITOR';
            localStorage.setItem('userRole', role);
            if(dados.userId) localStorage.setItem('userId', dados.userId);
            if(dados.userName) localStorage.setItem('userName', dados.userName);
            if(dados.email) localStorage.setItem('userEmail', dados.email);

            if (role === 'BIBLIOTECARIO' || role === 'ADMIN') {
                window.location.href = 'dashboard.html';
            } else {
                window.location.href = 'catalogo.html';
            }
        } else {
            if (resposta.status === 403) {
                const msg = await resposta.text();
                showToast(msg, 'error');
            } else {
                showToast('E-mail ou senha incorretos.', 'error');
            }
            restaurarBotao(btnSubmit, textoOriginal);
        }
    } catch (error) {
        console.error('Erro na requisição:', error);
        showToast('Erro ao conectar com o servidor.', 'error');
        restaurarBotao(btnSubmit, textoOriginal);
    }
});

// Callback chamado pelo Google após o usuário selecionar a conta
async function handleGoogleLogin(response) {
    if (!response.credential) {
        showToast('Erro ao obter credenciais do Google', 'error');
        return;
    }

    try {
        const res = await fetch(API_BASE_URL + '/login/google', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ token: response.credential })
        });

        if (res.ok) {
            const data = await res.json();
            // O cookie jwtToken será recebido automaticamente via cabeçalho Set-Cookie
            localStorage.setItem('userRole', data.role || 'LEITOR');
            if(data.userId) localStorage.setItem('userId', data.userId);
            if(data.userName) localStorage.setItem('userName', data.userName);
            if(data.email) localStorage.setItem('userEmail', data.email);

            showToast('Login com Google efetuado com sucesso!', 'success');
            
            setTimeout(() => {
                const tipo = data.role || 'LEITOR';
                if (tipo === 'ADMIN' || tipo === 'BIBLIOTECARIO') {
                    window.location.href = 'dashboard.html';
                } else {
                    window.location.href = 'catalogo.html';
                }
            }, 1500);

        } else {
            const err = await res.text();
            showToast('Falha no login com Google: ' + err, 'error');
        }
    } catch (error) {
        console.error('Erro no Google Login:', error);
        showToast('Erro ao conectar com o servidor.', 'error');
    }
}

// Função auxiliar para voltar o botão ao normal em caso de erro
function restaurarBotao(botao, texto) {
    botao.innerText = texto;
    botao.disabled = false;
    botao.style.opacity = '1';
}

