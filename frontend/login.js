document.getElementById('formLogin').addEventListener('submit', async function(event) {
    event.preventDefault();

    const email = document.getElementById('email').value;
    const senha = document.getElementById('senha').value;
    const btnSubmit = document.querySelector('#formLogin .btn-primary');
    
    const textoOriginal = btnSubmit.innerText;
    btnSubmit.innerText = 'Autenticando...';
    btnSubmit.disabled = true;

    try {
        const resposta = await fetch('https://bibliotech-api-e9wg.onrender.com/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, senha })
        });

        if (resposta.ok) {
            const dados = await resposta.json();
            localStorage.setItem('jwtToken', dados.token);
            
            // Decodifica o JWT para ver o email (sub)
            const base64Payload = dados.token.split('.')[1];
            const payload = JSON.parse(atob(base64Payload));
            const email = payload.sub;

            let role = 'LEITOR'; // fallback padrão

            try {
                // Busca no banco de dados para confirmar o role
                const resUser = await fetch('https://bibliotech-api-e9wg.onrender.com/usuarios', {
                    headers: { 'Authorization': `Bearer ${dados.token}` }
                });
                
                if (resUser.ok) {
                    const usuarios = await resUser.json();
                    const userLogado = usuarios.find(u => u.email === email);
                    if (userLogado && userLogado.tipo) {
                        role = userLogado.tipo;
                    }
                }
            } catch (err) {
                console.error("Erro ao buscar role no banco, usando fallback.", err);
            }

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
        const res = await fetch('https://bibliotech-api-e9wg.onrender.com/login/google', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ token: response.credential })
        });

        if (res.ok) {
            const data = await res.json();
            localStorage.setItem('jwtToken', data.token);
            
            // Decodifica o token JWT para pegar os dados do usuário (se precisar) e salva no localStorage
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

// Função auxiliar para voltar o botão ao normal em caso de erro
function restaurarBotao(botao, texto) {
    botao.innerText = texto;
    botao.disabled = false;
    botao.style.opacity = '1';
}
