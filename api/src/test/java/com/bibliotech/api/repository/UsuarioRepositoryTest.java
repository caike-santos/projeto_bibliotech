package com.bibliotech.api.repository;

import com.bibliotech.api.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // Essa é a mágica: tudo que o teste salvar no banco, o Spring apaga logo em seguida para não sujar os seus dados reais!
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Deveria retornar o usuário quando o e-mail existir no banco de dados")
    void findByEmailCenario01() {
        // Arrange (Cenário: Usamos o próprio repository para cadastrar um usuário temporário)
        String email = "caike.teste@email.com";
        
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha("123456");
        usuario.setNome("Caike");
        // Ajuste os campos abaixo caso sua entidade exija mais dados obrigatórios no banco:
        usuario.setTipo("CLIENTE"); 
        usuario.setStatus("ATIVO");
        usuario.setPontosGamificacao(0);
        
        usuarioRepository.save(usuario);

        // Act (Ação: Tentamos buscar o usuário recém-salvo)
        var usuarioEncontrado = usuarioRepository.findByEmail(email);

        // Assert (Validação: Garantimos que ele foi achado com sucesso)
        assertThat(usuarioEncontrado).isNotNull();
        assertThat(usuarioEncontrado.getUsername()).isEqualTo(email);
    }
}