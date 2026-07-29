document.getElementById('formLogin').addEventListener('submit', async function(event) {
    event.preventDefault();

    const email = document.getElementById('email').value;
    const senha = document.getElementById('senha').value;
    const btnSubmit = document.querySelector('#formLogin .btn-primary');
    
    const textoOriginal = btnSubmit.innerText;
    btnSubmit.innerText = 'Autenticando...';
    btnSubmit.disabled = true;

    try {
        const resposta = await fetch('http://localhost:8080/login', {
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
                const resUser = await fetch('http://localhost:8080/usuarios', {
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
    } catch (erro) {
        console.error('Erro:', erro);
        showToast('Falha ao conectar.', 'error');
        restaurarBotao(btnSubmit, textoOriginal);
    }
});


// Função auxiliar para voltar o botão ao normal em caso de erro
function restaurarBotao(botao, texto) {
    botao.innerText = texto;
    botao.disabled = false;
    botao.style.opacity = '1';
}