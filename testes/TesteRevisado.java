package testes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import revisado.EstoqueArquivo;

/**
 * Suíte repetível de testes para revisado/EstoqueArquivo.java.
 * Isola o ambiente recriando e removendo estoque.csv e movimentacoes.txt a cada caso de teste.
 */
public class TesteRevisado {

    private static final Path ESTOQUE = Paths.get("estoque.csv");
    private static final Path MOVIMENTACOES = Paths.get("movimentacoes.txt");

    private static int totalExecutados = 0;
    private static int totalPassou = 0;
    private static int totalFalhou = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================================");
        System.out.println("EXECUÇÃO DE TESTES CAIXA-BRANCA — CÓDIGO REVISADO (CORRIGIDO)");
        System.out.println("=================================================================\n");

        try {
            testarConsultaFluxoNormal();
            testarConsultaCodigoInexistente();
            testarConsultaCodigoInvalido();
            testarConsultaLinhaMalformada();
            testarSaidaFluxoNormalEAbatimentoSaldo();
            testarSaidaLimiteExato();
            testarSaidaSaldoInsuficiente();
            testarSaidaQuantidadeZeroOuNegativa();
            testarArquivoInexistente();
        } finally {
            limparArquivos();
        }

        System.out.println("\n=================================================================");
        System.out.printf("RESUMO REVISADO: %d executados | %d passaram | %d falharam%n",
                totalExecutados, totalPassou, totalFalhou);
        System.out.println("=================================================================");

        if (totalFalhou > 0) {
            System.exit(1);
        }
    }

    private static void prepararEstoque(String conteudo) throws IOException {
        limparArquivos();
        if (conteudo != null) {
            Files.writeString(ESTOQUE, conteudo, StandardCharsets.UTF_8);
        }
    }

    private static void limparArquivos() throws IOException {
        Files.deleteIfExists(ESTOQUE);
        Files.deleteIfExists(MOVIMENTACOES);
    }

    private static void registrar(String id, String descricao, boolean passou, String detalhe) {
        totalExecutados++;
        if (passou) {
            totalPassou++;
        } else {
            totalFalhou++;
        }
        System.out.printf("[%s] %s - %s%n   Detalhe: %s%n%n",
                id, passou ? "PASSOU" : "FALHOU", descricao, detalhe);
    }

    private static void testarConsultaFluxoNormal() throws IOException {
        prepararEstoque("P01;15\nP02;8\n");

        EstoqueArquivo est = new EstoqueArquivo();
        int q1 = est.consultarQuantidade("P01");
        int q2 = est.consultarQuantidade("P02");

        registrar("CT-R01", "Consulta de produtos existentes (1ª e 2ª linhas)",
                q1 == 15 && q2 == 8, "P01=" + q1 + ", P02=" + q2);
    }

    private static void testarConsultaCodigoInexistente() throws IOException {
        prepararEstoque("P01;15\n");

        EstoqueArquivo est = new EstoqueArquivo();
        int q = est.consultarQuantidade("P99");

        registrar("CT-R02", "Consulta de código inexistente retorna 0",
                q == 0, "P99=" + q);
    }

    private static void testarConsultaCodigoInvalido() throws IOException {
        prepararEstoque("P01;15\n");

        EstoqueArquivo est = new EstoqueArquivo();
        boolean rejeitouNull = false;
        boolean rejeitouVazio = false;

        try {
            est.consultarQuantidade(null);
        } catch (IllegalArgumentException e) {
            rejeitouNull = true;
        }

        try {
            est.consultarQuantidade("   ");
        } catch (IllegalArgumentException e) {
            rejeitouVazio = true;
        }

        registrar("CT-R03", "Rejeição de código nulo ou em branco",
                rejeitouNull && rejeitouVazio,
                "rejeitouNull=" + rejeitouNull + ", rejeitouVazio=" + rejeitouVazio);
    }

    private static void testarConsultaLinhaMalformada() throws IOException {
        prepararEstoque("\nLINHA_INVALIDA\nP01;20\n");

        EstoqueArquivo est = new EstoqueArquivo();
        int q = est.consultarQuantidade("P01");

        registrar("CT-R04", "Resiliência a linhas vazias ou malformadas no CSV",
                q == 20, "Retornou P01=" + q + " sem lançar exceção não tratada");
    }

    private static void testarSaidaFluxoNormalEAbatimentoSaldo() throws IOException {
        prepararEstoque("P01;15\nP02;8\n");

        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 5);
        int saldoRestante = est.consultarQuantidade("P01");
        String log = Files.exists(MOVIMENTACOES)
                ? Files.readString(MOVIMENTACOES, StandardCharsets.UTF_8).trim()
                : "";

        boolean passou = ok
                && saldoRestante == 10
                && log.contains("P01;5")
                && "P01".equals(est.getUltimaMovimentacao());

        registrar("CT-R05", "Registro de saída persiste log (flush/close) e abate saldo no CSV",
                passou,
                String.format("ok=%b, saldoRestante=%d, log='%s', ultimaMov='%s'",
                        ok, saldoRestante, log, est.getUltimaMovimentacao()));
    }

    private static void testarSaidaLimiteExato() throws IOException {
        prepararEstoque("P01;15\n");

        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 15);
        int saldoRestante = est.consultarQuantidade("P01");

        registrar("CT-R06", "Saída no limite exato (quantidade == disponivel) zera estoque",
                ok && saldoRestante == 0, "ok=" + ok + ", saldoRestante=" + saldoRestante);
    }

    private static void testarSaidaSaldoInsuficiente() throws IOException {
        prepararEstoque("P01;15\n");

        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 16);
        int saldoRestante = est.consultarQuantidade("P01");

        registrar("CT-R07", "Bloqueio quando quantidade > disponivel",
                !ok && saldoRestante == 15, "ok=" + ok + ", saldoRestante=" + saldoRestante);
    }

    private static void testarSaidaQuantidadeZeroOuNegativa() throws IOException {
        prepararEstoque("P01;15\n");

        EstoqueArquivo est = new EstoqueArquivo();
        boolean rejeitouZero = false;
        boolean rejeitouNegativo = false;

        try {
            est.registrarSaida("P01", 0);
        } catch (IllegalArgumentException e) {
            rejeitouZero = true;
        }

        try {
            est.registrarSaida("P01", -5);
        } catch (IllegalArgumentException e) {
            rejeitouNegativo = true;
        }

        int saldoRestante = est.consultarQuantidade("P01");

        registrar("CT-R08", "Bloqueio de saída com quantidade zero ou negativa",
                rejeitouZero && rejeitouNegativo && saldoRestante == 15,
                String.format("rejeitouZero=%b, rejeitouNegativo=%b, saldoRestante=%d",
                        rejeitouZero, rejeitouNegativo, saldoRestante));
    }

    private static void testarArquivoInexistente() throws IOException {
        prepararEstoque(null);

        EstoqueArquivo est = new EstoqueArquivo();
        boolean lancouErro = false;
        try {
            est.consultarQuantidade("P01");
        } catch (RuntimeException e) {
            lancouErro = true;
        }

        registrar("CT-R09", "Propagação explícita de erro quando arquivo não existe (sem catch vazio)",
                lancouErro, "lancouErro=" + lancouErro);
    }
}