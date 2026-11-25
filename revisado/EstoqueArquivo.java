package revisado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia consultas e baixas de estoque persistidas em arquivos texto (CSV/TXT).
 * Versão revisada com fechamento automático de recursos, validação de entradas,
 * atualização consistente de saldo e tratamento explícito de falhas de I/O.
 */
public class EstoqueArquivo {

    private final Path arquivoEstoque;
    private final Path arquivoMovimentacoes;
    private String ultimaMovimentacao = "";

    /**
     * Construtor padrão que utiliza os arquivos "estoque.csv" e "movimentacoes.txt"
     * no diretório de execução atual.
     */
    public EstoqueArquivo() {
        this(Paths.get("estoque.csv"), Paths.get("movimentacoes.txt"));
    }

    /**
     * Construtor parametrizado que permite injetar caminhos customizados (útil para testes isolados).
     *
     * @param arquivoEstoque       caminho do arquivo CSV de saldos
     * @param arquivoMovimentacoes caminho do arquivo TXT de histórico de saídas
     */
    public EstoqueArquivo(Path arquivoEstoque, Path arquivoMovimentacoes) {
        if (arquivoEstoque == null || arquivoMovimentacoes == null) {
            throw new IllegalArgumentException("Os caminhos dos arquivos não podem ser nulos.");
        }
        this.arquivoEstoque = arquivoEstoque;
        this.arquivoMovimentacoes = arquivoMovimentacoes;
    }

    /**
     * Consulta a quantidade disponível de um produto pelo seu código.
     *
     * @param codigo identificador do produto (não pode ser nulo ou em branco)
     * @return quantidade disponível em estoque, ou 0 caso o código não seja encontrado
     * @throws IllegalArgumentException se o código for nulo ou vazio
     * @throws UncheckedIOException     se o arquivo de estoque não existir ou falhar na leitura
     */
    public synchronized int consultarQuantidade(String codigo) {
        String codigoNormalizado = validarCodigo(codigo);

        if (!Files.exists(arquivoEstoque)) {
            throw new UncheckedIOException(
                    new IOException("Arquivo de estoque não encontrado: " + arquivoEstoque));
        }

        // try-with-resources garante o fechamento automático do BufferedReader
        try (BufferedReader leitor = Files.newBufferedReader(arquivoEstoque, StandardCharsets.UTF_8)) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.isBlank()) {
                    continue;
                }
                String[] colunas = linha.split(";");
                if (colunas.length < 2) {
                    continue;
                }
                if (colunas[0].trim().equals(codigoNormalizado)) {
                    try {
                        return Integer.parseInt(colunas[1].trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException(
                                "Quantidade inválida no CSV para o produto " + codigoNormalizado + ": " + colunas[1], e);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao ler o arquivo de estoque: " + arquivoEstoque, e);
        }

        return 0;
    }

    /**
     * Registra a saída (baixa) de uma quantidade de produto, abatendo o saldo no CSV
     * e gravando o histórico no arquivo de movimentações com flush/close garantidos.
     *
     * @param codigo     identificador do produto (não pode ser nulo ou em branco)
     * @param quantidade quantidade a retirar (deve ser estritamente maior que zero)
     * @return {@code true} se houver saldo suficiente e a baixa for persistida; {@code false} se o saldo for insuficiente
     * @throws IllegalArgumentException se o código for inválido ou se {@code quantidade <= 0}
     * @throws UncheckedIOException     se ocorrer falha de leitura ou escrita nos arquivos
     */
    public synchronized boolean registrarSaida(String codigo, int quantidade) {
        String codigoNormalizado = validarCodigo(codigo);
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade de saída deve ser maior que zero.");
        }
        if (!Files.exists(arquivoEstoque)) {
            throw new UncheckedIOException(
                    new IOException("Arquivo de estoque não encontrado: " + arquivoEstoque));
        }

        try {
            List<String> linhas = Files.readAllLines(arquivoEstoque, StandardCharsets.UTF_8);
            List<String> linhasAtualizadas = new ArrayList<>(linhas.size());
            boolean produtoEncontrado = false;
            boolean saldoSuficiente = false;

            for (String linha : linhas) {
                if (linha.isBlank()) {
                    linhasAtualizadas.add(linha);
                    continue;
                }
                String[] colunas = linha.split(";");
                if (colunas.length >= 2 && colunas[0].trim().equals(codigoNormalizado) && !produtoEncontrado) {
                    produtoEncontrado = true;
                    int disponivel;
                    try {
                        disponivel = Integer.parseInt(colunas[1].trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException(
                                "Quantidade inválida no CSV para o produto " + codigoNormalizado + ": " + colunas[1], e);
                    }

                    if (quantidade <= disponivel) {
                        saldoSuficiente = true;
                        int novoSaldo = disponivel - quantidade;
                        linhasAtualizadas.add(codigoNormalizado + ";" + novoSaldo);
                    } else {
                        linhasAtualizadas.add(linha);
                    }
                } else {
                    linhasAtualizadas.add(linha);
                }
            }

            if (!produtoEncontrado || !saldoSuficiente) {
                return false;
            }

            // Atualiza o saldo no estoque.csv e grava o log em movimentacoes.txt com fechamento automático
            Files.write(arquivoEstoque, linhasAtualizadas, StandardCharsets.UTF_8);

            try (BufferedWriter escrita = Files.newBufferedWriter(
                    arquivoMovimentacoes,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                escrita.write(codigoNormalizado + ";" + quantidade);
                escrita.newLine();
            }

            ultimaMovimentacao = codigoNormalizado;
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Erro de I/O ao registrar saída do produto " + codigoNormalizado, e);
        }
    }

    /**
     * Retorna o código do último produto que teve saída registrada com sucesso.
     *
     * @return código do último produto movimentado, ou string vazia se nenhuma saída ocorreu
     */
    public synchronized String getUltimaMovimentacao() {
        return ultimaMovimentacao;
    }

    private String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código do produto não pode ser nulo ou vazio.");
        }
        return codigo.trim();
    }
}