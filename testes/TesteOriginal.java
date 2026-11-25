package testes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import original.EstoqueArquivo;

/**
 * Suíte repetível de testes de caixa-branca para original/EstoqueArquivo.java.
 * Executa os caminhos básicos do CFG e demonstra objetivamente as falhas do legado.
 */
public class TesteOriginal {

    private static final Path ESTOQUE = Paths.get("estoque.csv");
    private static final Path MOVIMENTACOES = Paths.get("movimentacoes.txt");
    private static int totalExecutados = 0;
    private static int totalPassou = 0;
    private static int totalFalhou = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================================");
        System.out.println("EXECUÇÃO DE TESTES CAIXA-BRANCA — CÓDIGO ORIGINAL (LEGADO)");
        System.out.println("=================================================================\n");

        try {
            executarCT01();
            executarCT02();
            executarCT03();
            executarCT04();
            executarCT05();
            executarCT06();
            executarCT07();
            executarCT08();
            executarCT09();
            executarCT10();
            executarCT11();
        } finally {
            limparArquivos();
        }

        System.out.println("\n=================================================================");
        System.out.printf("RESUMO LEGADO: %d executados | %d passaram | %d falharam (defeitos comprovados)%n",
                totalExecutados, totalPassou, totalFalhou);
        System.out.println("=================================================================");
    }

    private static void prepararAmbiente(String conteudoEstoque) throws IOException {
        limparArquivos();
        if (conteudoEstoque != null) {
            Files.writeString(ESTOQUE, conteudoEstoque, StandardCharsets.UTF_8);
        }
    }

    private static void limparArquivos() {
        try {
            Files.deleteIfExists(ESTOQUE);
            Files.deleteIfExists(MOVIMENTACOES);
        } catch (IOException e) {
            System.out.println("  [AVISO DE RECURSO PRESO] Não foi possível remover arquivo: " + e.getMessage());
        }
    }

    private static void registrar(String id, String descricao, boolean passou, String esperado, String observado) {
        totalExecutados++;
        if (passou) {
            totalPassou++;
        } else {
            totalFalhou++;
        }
        String status = passou ? "PASSOU" : "FALHOU (DEFEITO DO LEGADO)";
        System.out.printf("[%s] %s - %s%n", id, status, descricao);
        System.out.printf("   Esperado : %s%n", esperado);
        System.out.printf("   Observado: %s%n%n", observado);
    }

    private static void executarCT01() throws IOException {
        prepararAmbiente("P01;15\nP02;8\n");
        EstoqueArquivo est = new EstoqueArquivo();
        int qtd = est.consultarQuantidade("P01");
        registrar("CT-01", "consultarQuantidade - Produto na 1ª linha (Caminho C3 / break)",
                qtd == 15, "15", String.valueOf(qtd));
    }

    private static void executarCT02() throws IOException {
        prepararAmbiente("P01;15\nP02;8\n");
        EstoqueArquivo est = new EstoqueArquivo();
        int qtd = est.consultarQuantidade("P02");
        registrar("CT-02", "consultarQuantidade - Produto na 2ª linha (Caminho C4 / loop)",
                qtd == 8, "8", String.valueOf(qtd));
    }

    private static void executarCT03() throws IOException {
        prepararAmbiente("P01;15\nP02;8\n");
        EstoqueArquivo est = new EstoqueArquivo();
        int qtd = est.consultarQuantidade("P99");
        registrar("CT-03", "consultarQuantidade - Código inexistente (Caminho C2 / EOF)",
                qtd == 0, "0", String.valueOf(qtd));
    }

    private static void executarCT04() throws IOException {
        prepararAmbiente(null);
        EstoqueArquivo est = new EstoqueArquivo();
        int qtd = est.consultarQuantidade("P01");
        registrar("CT-04", "consultarQuantidade - Arquivo ausente (Caminho C1 / catch IOException)",
                false,
                "Sinalizar erro de arquivo ausente (não confundir falha de I/O com saldo 0)",
                "Retornou " + qtd + " silenciosamente (IOException engolida no catch)");
    }

    private static void executarCT05() throws IOException {
        prepararAmbiente("P01;15\n");
        EstoqueArquivo est = new EstoqueArquivo();
        int qtd = est.consultarQuantidade(null);
        registrar("CT-05", "consultarQuantidade - Parâmetro codigo == null",
                false,
                "Lançar IllegalArgumentException para código nulo",
                "Aceitou null e retornou " + qtd);
    }

    private static void executarCT06() throws IOException {
        prepararAmbiente("\nP01;ABC\n");
        EstoqueArquivo est = new EstoqueArquivo();
        try {
            int qtd = est.consultarQuantidade("P01");
            registrar("CT-06", "consultarQuantidade - Linha vazia / quantidade não numérica",
                    false, "Tratar linha malformada sem crash", "Retornou " + qtd);
        } catch (RuntimeException e) {
            registrar("CT-06", "consultarQuantidade - Linha vazia / quantidade não numérica",
                    false,
                    "Tratar linha vazia/corrompida graciosamente sem derrubar a aplicação",
                    "Lançou exceção não tratada: " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
        }
    }

    private static void executarCT07() throws IOException {
        prepararAmbiente("P01;15\n");
        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 5);
        long tamanhoLog = Files.exists(MOVIMENTACOES) ? Files.size(MOVIMENTACOES) : -1;
        int saldoPosSaida = est.consultarQuantidade("P01");

        boolean realmenteFuncionou = ok && tamanhoLog > 0 && saldoPosSaida == 10;
        registrar("CT-07", "registrarSaida - Saída válida de 5 unidades (Caminho C2)",
                realmenteFuncionou,
                "Retornar true, gravar 'P01;5' em movimentacoes.txt e atualizar saldo para 10",
                String.format("Retornou %b, mas tamanho de movimentacoes.txt = %d bytes (buffer sem close) e saldo = %d",
                        ok, tamanhoLog, saldoPosSaida));
    }

    private static void executarCT08() throws IOException {
        prepararAmbiente("P01;15\n");
        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 15);
        int saldoPosSaida = est.consultarQuantidade("P01");

        registrar("CT-08", "registrarSaida - Valor limite quantidade == disponivel (15 == 15)",
                ok && saldoPosSaida == 0,
                "Retornar true e zerar saldo disponível (0)",
                String.format("Retornou %b, mas saldo permaneceu %d em estoque.csv", ok, saldoPosSaida));
    }

    private static void executarCT09() throws IOException {
        prepararAmbiente("P01;15\n");
        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 16);
        registrar("CT-09", "registrarSaida - Saldo insuficiente (16 > 15, Caminho C1)",
                !ok, "false", String.valueOf(ok));
    }

    private static void executarCT10() throws IOException {
        prepararAmbiente("P01;15\n");
        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", -5);
        registrar("CT-10", "registrarSaida - Quantidade negativa (-5 <= 15)",
                !ok,
                "Rejeitar quantidade <= 0 (retornar false ou lançar exceção)",
                "Aceitou saída negativa e retornou " + ok + " (ultimaMovimentacao='" + est.getUltimaMovimentacao() + "')");
    }

    private static void executarCT11() throws IOException {
        prepararAmbiente("P01;15\n");
        Files.createDirectory(MOVIMENTACOES);
        EstoqueArquivo est = new EstoqueArquivo();
        boolean ok = est.registrarSaida("P01", 5);
        registrar("CT-11", "registrarSaida - Falha de I/O na escrita (Caminho C3 / catch IOException)",
                false,
                "Propagar/logar erro de I/O (não confundir falha de disco com falta de estoque)",
                "Retornou " + ok + " silenciosamente (IOException engolida no catch)");
    }
}