package com.bibliotech.api.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;

@Service // Indica que aqui ficam as Regras de Negócio
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final UsuarioRepository usuarioRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository, UsuarioRepository usuarioRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Emprestimo realizarEmprestimo(Emprestimo novoEmprestimo) {
        // RN01 - Verifica se o usuário já tem 3 empréstimos ATIVOS
        int quantidadeAtivos = emprestimoRepository.countByUsuarioIdAndStatus(novoEmprestimo.getUsuario().getId(), "ATIVO");
        
        if (quantidadeAtivos >= 3) {
            // Se tiver 3 ou mais, o Java aborta e lança um erro
            throw new RuntimeException("Limite excedido: O leitor já possui 3 livros emprestados.");
        }

        // RN02 - Prazo de Devolução Automático (14 dias)
        // Setamos os valores no backend para garantir que ninguém burle as regras
        novoEmprestimo.setDataRetirada(LocalDate.now());
        novoEmprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(14));
        novoEmprestimo.setStatus("ATIVO");
        novoEmprestimo.setRenovacoesFeitas(0);

        Long usuarioId = novoEmprestimo.getUsuario().getId();

        // RN03 - Bloqueio por Atraso
        if (emprestimoRepository.existsByUsuarioIdAndStatus(usuarioId, "ATRASADO")) {
            throw new RuntimeException("Bloqueado: O leitor possui livros em atraso e precisa regularizar sua situação.");
        }

        // Se passar pela regra, salva no banco
        return emprestimoRepository.save(novoEmprestimo);
    }

    // RN04 - Limite de Renovação
    public Emprestimo renovarEmprestimo(Long emprestimoId) {
        // Busca o empréstimo no banco ou devolve erro se não existir
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        // RN03 aplicada à renovação (Não pode renovar se estiver bloqueado por atraso)
        if (emprestimoRepository.existsByUsuarioIdAndStatus(emprestimo.getUsuario().getId(), "ATRASADO")) {
            throw new RuntimeException("Bloqueado: O leitor não pode fazer renovações pois possui livros em atraso.");
        }

        // RN04 - Só pode renovar 1 vez
        if (emprestimo.getRenovacoesFeitas() >= 1) {
            throw new RuntimeException("Limite de renovação: Este livro já foi renovado o limite máximo de vezes permitidas.");
        }

        // Aplica a renovação: Adiciona +1 à contagem e joga o prazo para mais 14 dias
        emprestimo.setRenovacoesFeitas(emprestimo.getRenovacoesFeitas() + 1);
        emprestimo.setDataDevolucaoPrevista(emprestimo.getDataDevolucaoPrevista().plusDays(14));

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo devolverLivro(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado com o ID: " + emprestimoId));

        if ("DEVOLVIDO".equals(emprestimo.getStatus())) {
            throw new RuntimeException("Este livro já consta como devolvido no sistema.");
        }

        emprestimo.setDataDevolucaoReal(LocalDate.now());
        emprestimo.setStatus("DEVOLVIDO");

        // Regra de Negócio: Verificar Atrasos vs Gamificação
        if (!emprestimo.getDataDevolucaoReal().isAfter(emprestimo.getDataDevolucaoPrevista())) {
            
            // ENTREGOU NO PRAZO: Ganha Gamificação e Zera Multa
            Usuario usuario = emprestimo.getUsuario();
            int pontosAtuais = usuario.getPontosGamificacao();
            usuario.setPontosGamificacao(pontosAtuais + 10);
            usuarioRepository.save(usuario);
            
            emprestimo.setValorMulta(0.0); // Zera a multa por segurança
            
        } else {
            // ENTREGOU ATRASADO: Perde o direito aos pontos e paga multa
            // Calcula a diferença exata de dias entre o dia que era pra devolver e o dia que devolveu
            long diasAtraso = ChronoUnit.DAYS.between(emprestimo.getDataDevolucaoPrevista(), emprestimo.getDataDevolucaoReal());
            
            // R$ 2.00 cobrados por cada dia
            double valorDaMulta = diasAtraso * 2.00;
            
            emprestimo.setValorMulta(valorDaMulta);
            
            System.out.println("Alerta de Atraso: " + diasAtraso + " dias. Multa gerada: R$ " + valorDaMulta);
        }

        return emprestimoRepository.save(emprestimo);
    }
}