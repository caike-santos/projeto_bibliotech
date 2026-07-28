document.getElementById('formLogin').addEventListener('submit', async function(event) {
    // Evita que a página recarregue ao enviar o formulário (comportamento padrão do HTML)
    event.preventDefault();

    // Captura os valores digitados pelo usuário
    const email = document.getElementById('email').value;
    const senha = document.getElementById('senha').value;
    const btnSubmit = document.querySelector('.btn-primary');

    // Dá um feedback visual para o usuário enquanto o servidor processa
    const textoOriginal = btnSubmit.innerText;
    btnSubmit.innerText = 'Autenticando Lumina...';
    btnSubmit.disabled = true;
    btnSubmit.style.opacity = '0.7';

    try {
        // Faz a requisição POST para a sua API Spring Boot rodando no Docker
        const resposta = await fetch('http://localhost:8080/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            // Empacota o email e senha no formato JSON esperado pelo record DadosAutenticacao
            body: JSON.stringify({ email, senha })
        });

        if (resposta.ok) {
            // Se o Spring Security aprovar, extraímos o JSON retornado (DadosTokenJWT)
            const dados = await resposta.json();
            
            // Salva o Token JWT no "cofre" do navegador (Local Storage)
            // A partir de agora, o front-end vai mandar esse token no cabeçalho das próximas requisições
            localStorage.setItem('jwtToken', dados.token);
            
            // Redireciona o usuário para a página principal (que criaremos a seguir)
            window.location.href = 'catalogo.html';
        } else {
            // Se o status for 403 (Forbidden) ou 401 (Unauthorized)
            alert('E-mail ou senha incorretos. Verifique suas credenciais.');
            restaurarBotao(btnSubmit, textoOriginal);
        }
    } catch (erro) {
        console.error('Erro de conexão:', erro);
        alert('Falha ao conectar com os servidores da BiblioTech. Verifique se a API está no ar.');
        restaurarBotao(btnSubmit, textoOriginal);
    }
});

// Função auxiliar para voltar o botão ao normal em caso de erro
function restaurarBotao(botao, texto) {
    botao.innerText = texto;
    botao.disabled = false;
    botao.style.opacity = '1';
}