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
            localStorage.setItem('jwtToken', dados.token);
            
            // O backend agora jÃ¡ devolve o role na resposta do login (dados.role)
            let role = dados.role || 'LEITOR';

            localStorage.setItem('userRole', role);

            if (role === 'BIBLIOTECARIO' || role === 'ADMIN') {
                window.location.href = 'dashboard.html';
            } else {
                window.location.href = 'catalogo.html';
            }
        } else {
            showToast('E-mail ou senha incorretos.', 'error');
            restaurarBotao(btnSubmit, textoOriginal);
        }
    } catch (error) {
        console.error('Erro na requisiÃ§Ã£o:', error);
        showToast('Erro ao conectar com o servidor.', 'error');
        restaurarBotao(btnSubmit, textoOriginal);
    }
});

// Callback chamado pelo Google apÃ³s o usuÃ¡rio selecionar a conta
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
            localStorage.setItem('jwtToken', data.token);
            
            // Decodifica o token JWT para pegar os dados do usuÃ¡rio (se precisar) e salva no localStorage
            try {
                const base64Url = data.token.split('.')[1];
                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
                const tokenData = JSON.parse(jsonPayload);
                
                // O token da API Bibliotech atual guarda "id", "nome", "tipo", etc.
                if(tokenData.id) localStorage.setItem('userId', tokenData.id);
                if(tokenData.nome) localStorage.setItem('userName', tokenData.nome);
                if(tokenData.tipo) localStorage.setItem('userTipo', tokenData.tipo);
            } catch(e) {
                console.log("Erro ao decodificar token", e);
            }

            showToast('Login com Google efetuado com sucesso!', 'success');
            
            setTimeout(() => {
                const tipo = localStorage.getItem('userTipo');
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

// FunÃ§Ã£o auxiliar para voltar o botÃ£o ao normal em caso de erro
function restaurarBotao(botao, texto) {
    botao.innerText = texto;
    botao.disabled = false;
    botao.style.opacity = '1';
}

