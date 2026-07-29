document.getElementById('formCadastro').addEventListener('submit', async function(event) {
    event.preventDefault();

    const nome = document.getElementById('cadNome').value;
    const email = document.getElementById('cadEmail').value;
    const senha = document.getElementById('cadSenha').value;
    const btnSubmit = document.querySelector('#formCadastro .btn-primary');
    
    const textoOriginal = btnSubmit.innerText;
    btnSubmit.innerText = 'Cadastrando...';
    btnSubmit.disabled = true;

    try {
        const resposta = await fetch('http://localhost:8080/usuarios', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nome, email, senha, tipo: 'LEITOR', status: 'ATIVO', enabled: true })
        });

        if (resposta.ok) {
            showToast('Cadastro realizado com sucesso! Redirecionando para o login...', 'success');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 2000);
        } else {
            await handleApiError(resposta, 'Erro ao cadastrar.');
            restaurarBotao(btnSubmit, textoOriginal);
        }
    } catch (erro) {
        console.error('Erro:', erro);
        showToast('Falha ao conectar.', 'error');
        restaurarBotao(btnSubmit, textoOriginal);
    }
});

function restaurarBotao(botao, texto) {
    botao.innerText = texto;
    botao.disabled = false;
    botao.style.opacity = '1';
}
