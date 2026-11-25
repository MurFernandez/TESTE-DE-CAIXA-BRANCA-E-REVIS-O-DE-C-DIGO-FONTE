# Testes de Caixa-Branca e Revisão de Código — `EstoqueArquivo.java`

Este repositório reúne a atividade prática de **Análise Estática, Grafos de Fluxo de Controle (CFG), Complexidade Ciclomática, Testes de Caixa-Branca e Refatoração** aplicada à classe Java `EstoqueArquivo.java`.

---

## 1. Introdução e Identificação do Cenário

O sistema analisado faz o controle básico de entrada e saída de produtos usando dois arquivos de texto simples:
* **`estoque.csv`:** guarda o código do produto e a quantidade disponível, separados por ponto e vírgula (exemplo: `P01;15`).
* **`movimentacoes.txt`:** guarda o histórico das saídas registradas (exemplo: `P01;5`).

A classe **`EstoqueArquivo`** possui três métodos principais:
1. `consultarQuantidade(String codigo)`: lê o arquivo `estoque.csv` e devolve quanto existe em estoque daquele produto.
2. `registrarSaida(String codigo, int quantidade)`: verifica se existe estoque suficiente e registra a retirada no arquivo `movimentacoes.txt`.
3. `getUltimaMovimentacao()`: retorna o código do último produto que teve saída registrada.

O objetivo deste trabalho foi encontrar os erros do código original (`original/`), desenhar seus fluxos de execução, provar os defeitos com testes automatizados (`testes/`), criar uma versão corrigida (`revisado/`) e salvar todas as provas de execução (`evidencias/`).

---

## 2. Instruções de Execução

### O que você precisa ter instalado
* **Java JDK 11 ou superior** (`javac` e `java` funcionando no terminal).

### Organização das Pastas

```text
TESTE-DE-CAIXA-BRANCA-E-REVIS-O-DE-C-DIGO-FONTE/
├── original/                   # Código-base original (mantido intacto)
│   └── EstoqueArquivo.java
├── revisado/                   # Código corrigido e comentado
│   └── EstoqueArquivo.java
├── testes/                     # Programas de teste repetíveis (Original e Revisado)
│   ├── TesteOriginal.java
│   └── TesteRevisado.java
├── diagramas/                  # Grafos de Fluxo de Controle (.drawio e .png)
│   ├── CFG_consultarQuantidade.drawio
│   ├── CFG_consultarQuantidade.png
│   ├── CFG_registrarSaida.drawio
│   └── CFG_registrarSaida.png
├── evidencias/                 # Logs e prints da execução dos testes
│   ├── execucao_original.txt
│   ├── execucao_revisado.txt
│   └── execucao_testes.png
├── Plano de Teste.xlsx         # Planilha da análise estática preenchida
└── README.md                   # Este relatório técnico
```

### Como compilar e rodar os testes

Abra o terminal na pasta raiz do projeto e execute os comandos abaixo:

**No Windows (PowerShell):**
```powershell
# 1. Ajustar acentuação do terminal e compilar todas as classes na pasta bin/
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
javac -encoding UTF-8 -d bin original/*.java revisado/*.java testes/*.java

# 2. Rodar os testes no código ORIGINAL (mostra os defeitos do código legado)
java "-Dfile.encoding=UTF-8" -cp bin testes.TesteOriginal

# 3. Rodar os testes no código REVISADO (valida que todas as correções funcionam)
java "-Dfile.encoding=UTF-8" -cp bin testes.TesteRevisado
```

**No Linux / macOS / Git Bash:**
```bash
# 1. Compilar todas as classes na pasta bin/
javac -encoding UTF-8 -d bin original/*.java revisado/*.java testes/*.java

# 2. Rodar os testes no código ORIGINAL
java -cp bin testes.TesteOriginal

# 3. Rodar os testes no código REVISADO
java -cp bin testes.TesteRevisado
```

*(Nota: caso `TesteOriginal.java` esteja sem declaração `package testes;` no topo do arquivo, execute `java -cp bin TesteOriginal` no passo 2.)*

---

## 3. Análise Estática

A revisão manual completa do arquivo `original/EstoqueArquivo.java` está preenchida na aba **`CAIXA BRANCA (ESTÁTICO)`** da planilha [`Plano de Teste.xlsx`](Plano%20de%20Teste.xlsx).

Dos 10 itens avaliados na planilha:
* **4 itens estavam corretos (linhas em azul):** nomenclatura de classes/arquivos (ID 2), nomenclatura de variáveis (ID 3), condição de parada do laço `while` (ID 4) e inicialização das variáveis (ID 6).
* **6 itens apresentavam problemas (linhas em vermelho):**
  1. **Falta de documentação (ID 1 — Baixa):** a classe e os métodos não possuem comentários Javadoc explicando parâmetros, retornos e erros.
  2. **Falta de validação para `null` (ID 5 — Média):** passar `codigo = null` em `consultarQuantidade` (linha 17) não gera erro de validação e retorna `0` silenciosamente.
  3. **Saldo nunca é descontado no arquivo (ID 7 — Crítica):** o método `registrarSaida` (linhas 28–35) grava a saída, mas não subtrai a quantidade do arquivo `estoque.csv`, permitindo retirar o mesmo produto infinitas vezes.
  4. **Erros ignorados e falta de proteção no CSV (ID 8 — Alta):** os blocos `catch (IOException e)` (linhas 22 e 36) estão vazios e escondem falhas de disco; além disso, linhas vazias ou letras no lugar de números quebram o programa com `ArrayIndexOutOfBoundsException` e `NumberFormatException`.
  5. **Arquivos abertos sem fechar (ID 9 — Crítica):** `BufferedReader` (linha 13) e `FileWriter` (linha 32) nunca chamam `.close()`. No Windows, isso trava o arquivo `estoque.csv` em memória e deixa `movimentacoes.txt` vazio (`0 bytes`) porque os dados ficam presos no buffer.
  6. **Aceitação de quantidades zero ou negativas (ID 10 — Crítica):** na linha 30 (`if (quantidade <= disponivel)`), informar `-5` passa na verificação (`-5 <= 15`) e registra uma saída negativa.

---

## 4. Grafos de Fluxo de Controle (CFG) e Convenções

Os dois grafos foram desenhados a partir do código original e estão salvos na pasta `diagramas/`.

### Regras adotadas nos diagramas
* **Nós (`N`):** representam o início/fim do método, blocos de comandos sequenciais, decisões (`while` e `if`), desvios (`break` e `return`) e blocos `catch`.
* **Arestas (`E`):** representam o caminho normal entre comandos, decisões verdadeiro/falso, retorno do laço `while`, saída pelo `break`/`return` e desvios para o `catch` quando ocorre erro de arquivo (`IOException`).
* **Exceções modeladas:** apenas `IOException` possui `try/catch` no código original (na abertura/leitura de `estoque.csv` e na abertura/escrita de `movimentacoes.txt`). Erros não tratados como `NumberFormatException` encerram o método sem caminho de recuperação.
* **`switch`:** não existe comando `switch` no código analisado.

### 4.1. Grafo de `consultarQuantidade(String codigo)`

![CFG consultarQuantidade](diagramas/CFG_consultarQuantidade.png)

* **Nós:** `N1` (Início) → `N2` (`quantidade = 0` / `try`) → `N3` (abre `FileReader`) → `N4` (`while readLine != null`) → `N5` (`split(";")`) → `N6` (`if colunas[0].equals(codigo)`) → `N7` (`parseInt`) → `N8` (`break`) → `N9` (`catch IOException`) → `N10` (`return quantidade`) → `N11` (Fim).

### 4.2. Grafo de `registrarSaida(String codigo, int quantidade)`

![CFG registrarSaida](diagramas/CFG_registrarSaida.png)

* **Nós:** `N1` (Início) → `N2` (`disponivel = consultarQuantidade`) → `N3` (`if quantidade <= disponivel`) → `N4` (`try` / abre `FileWriter`) → `N5` (`write` / `ultimaMovimentacao`) → `N6` (`return true`) → `N7` (`catch IOException`) → `N8` (`return false`) → `N9` (Fim).

---

## 5. Cálculo da Complexidade Ciclomática $V(G)$

Usando a fórmula de McCabe $V(G) = E - N + 2P$ (com $P = 1$ componente conectado por método):

| Método | Nós ($N$) | Arestas ($E$) | Fórmula $V(G) = E - N + 2P$ | Conferência por Regiões ($R$) | Conferência por Decisões ($D + 1$) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`consultarQuantidade`** | $11$ | $14$ | $14 - 11 + 2(1) = \mathbf{5}$ | $4\text{ internas} + 1\text{ externa} = \mathbf{5}$ | $2\text{ decisões} + 2\text{ desvios I/O} + 1 = \mathbf{5}$ |
| **`registrarSaida`** | $9$ | $11$ | $11 - 9 + 2(1) = \mathbf{4}$ | $3\text{ internas} + 1\text{ externa} = \mathbf{4}$ | $1\text{ decisão} + 2\text{ desvios I/O} + 1 = \mathbf{4}$ |

---

## 6. Caminhos Básicos Independentes

### Método `consultarQuantidade` ($V(G) = 5$ caminhos)
1. **C1 (Erro ao abrir arquivo):** `N1 → N2 → N3 → N9 → N10 → N11` — `estoque.csv` não existe ou está bloqueado (`IOException`).
2. **C2 (Produto não encontrado / arquivo vazio):** `N1 → N2 → N3 → N4 → N10 → N11` — chega ao fim do arquivo (`readLine() == null`) sem achar o código.
3. **C3 (Encontrado na 1ª linha):** `N1 → N2 → N3 → N4 → N5 → N6 → N7 → N8 → N10 → N11` — acha o código logo na primeira leitura e sai pelo `break`.
4. **C4 (Repetição do laço):** `N1 → N2 → N3 → N4 → N5 → N6 → N4 → N5 → N6 → N7 → N8 → N10 → N11` — a 1ª linha não bate, volta para o `while` e encontra na 2ª linha.
5. **C5 (Erro durante a leitura):** `N1 → N2 → N3 → N4 → N9 → N10 → N11` — abre o arquivo, mas falha no meio do `readLine()` (`IOException`).

### Método `registrarSaida` ($V(G) = 4$ caminhos)
1. **C1 (Estoque insuficiente):** `N1 → N2 → N3 → N8 → N9` — `quantidade <= disponivel` é falso e retorna `false`.
2. **C2 (Saída concluída):** `N1 → N2 → N3 → N4 → N5 → N6 → N9` — tem saldo, grava no arquivo e encerra no `return true`.
3. **C3 (Erro ao abrir arquivo de saída):** `N1 → N2 → N3 → N4 → N7 → N8 → N9` — `FileWriter` lança `IOException`.
4. **C4 (Erro ao escrever no arquivo):** `N1 → N2 → N3 → N4 → N5 → N7 → N8 → N9` — `write()` lança `IOException`.

---

## 7. Matriz de Casos de Teste, Testes Validados e Simulação

### 7.1. Resumo dos Testes Validados (Defeitos Comprovados vs. Correções Aprovadas)

A tabela abaixo mostra diretamente quais testes comprovaram cada defeito no código original e quais testes validaram que o problema foi resolvido no código revisado:

| Problema Encontrado | Teste no Código Original (Comprovou a Falha) | Teste no Código Revisado (Validou a Correção) |
| :--- | :--- | :--- |
| **Arquivo preso em memória e log vazio (`0 bytes`)** | **CT-04** (Windows acusou *"arquivo já está sendo usado por outro processo"*) e **CT-07** (`movimentacoes.txt` ficou com `0 bytes`). | **✅ CT-R01** e **✅ CT-R05** (arquivo fecha automaticamente e grava `P01;5` no disco). |
| **Saldo nunca era descontado de `estoque.csv`** | **CT-07** e **CT-08** (após retirar `5` ou `15` de `15`, o saldo continuava `15`). | **✅ CT-R05** (saldo caiu de `15` para `10`) e **✅ CT-R06** (saldo zerou para `0`). |
| **Aceitação de saída negativa ou zero** | **CT-10** (`registrarSaida("P01", -5)` retornou `true` indevidamente). | **✅ CT-R08** (bloqueou `0` e `-5` com `IllegalArgumentException`). |
| **Aceitação de código nulo ou vazio** | **CT-05** (`consultarQuantidade(null)` não validou a entrada e retornou `0`). | **✅ CT-R03** (rejeitou `null` e `"   "` com `IllegalArgumentException`). |
| **Quebra por linha vazia ou texto no CSV** | **CT-06** (quebrou com `NumberFormatException` ao ler `P01;ABC`). | **✅ CT-R04** (ignorou linhas inválidas e leu `P01=20` normalmente). |
| **Erro de arquivo ausente/bloqueado escondido** | **CT-04** e **CT-11** (`IOException` ignorada no `catch` vazio). | **✅ CT-R09** (propagou erro claro `UncheckedIOException` quando o arquivo não existe). |

### 7.2. Execução no Código Original (`testes/TesteOriginal.java`)

Nos testes do código legado, os cenários que **falharam** servem justamente como prova técnica de que os defeitos existem no código original:

| ID | Método | Caminho | Pré-condição | Entrada | Resultado Esperado | Resultado Observado | Situação | Evidência |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **CT-01** | `consultarQuantidade` | C3 | `estoque.csv` com `P01;15` e `P02;8` | `"P01"` | `15` | Retornou `15` (mas deixou arquivo aberto). | **Passou** *(com leak)* | `evidencias/execucao_original.txt` |
| **CT-02** | `consultarQuantidade` | C4 | `estoque.csv` com `P01;15` e `P02;8` | `"P02"` | `8` | Retornou `8`. | **Passou** | `evidencias/execucao_original.txt` |
| **CT-03** | `consultarQuantidade` | C2 | `estoque.csv` sem `P99` | `"P99"` | `0` | Retornou `0`. | **Passou** | `evidencias/execucao_original.txt` |
| **CT-04** | `consultarQuantidade` | C1 | Tentar apagar `estoque.csv` | `"P01"` | Sinalizar arquivo ausente | Arquivo ficou travado pelo `CT-01` e retornou `15`. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |
| **CT-05** | `consultarQuantidade` | C2 | `estoque.csv` com `P01;15` | `null` | Lançar `IllegalArgumentException` | Aceitou `null` e retornou `0`. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |
| **CT-06** | `consultarQuantidade` | Exceção | CSV com linha vazia e `P01;ABC` | `"P01"` | Tratar erro sem derrubar | Lançou `NumberFormatException` sem tratamento. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |
| **CT-07** | `registrarSaida` | C2 | `estoque.csv` com `P01;15` | `"P01"`, `5` | `true`, log `P01;5` e saldo `10` | Retornou `true`, mas log ficou com `0 bytes` e saldo `15`. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |
| **CT-08** | `registrarSaida` | C2 (Limite) | `estoque.csv` com `P01;15` | `"P01"`, `15` | `true` e zerar saldo (`0`) | Retornou `true`, mas saldo continuou `15`. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |
| **CT-09** | `registrarSaida` | C1 (Limite) | `estoque.csv` com `P01;15` | `"P01"`, `16` | `false` | Retornou `false`. | **Passou** | `evidencias/execucao_original.txt` |
| **CT-10** | `registrarSaida` | C2 (Borda) | `estoque.csv` com `P01;15` | `"P01"`, `-5` | Rejeitar quantidade $\le 0$ | Aceitou `-5` e retornou `true`. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |
| **CT-11** | `registrarSaida` | C3 | `movimentacoes.txt` bloqueado | `"P01"`, `5` | Sinalizar erro de escrita | Engoliu `IOException` e retornou `false`. | **Falhou** *(Defeito)* | `evidencias/execucao_original.txt` |

### 7.3. Execução Validada no Código Revisado (`testes/TesteRevisado.java` — 9/9 Aprovados)

Todos os 9 testes automatizados executados contra `revisado/EstoqueArquivo.java` **passaram com sucesso (`0 falhas`)**:

| ID | Cenário Testado | Resultado Esperado | Resultado Observado | Situação | Evidência |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **CT-R01** | Consulta na 1ª e 2ª linhas (`P01` e `P02`) | `P01=15` e `P02=8` | `P01=15, P02=8` (arquivo fechado corretamente) | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R02** | Consulta de código inexistente (`P99`) | `0` | `P99=0` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R03** | Consulta com código `null` ou em branco | Lançar `IllegalArgumentException` | `rejeitouNull=true, rejeitouVazio=true` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R04** | CSV com linha vazia e texto inválido | Ignorar linhas ruins e ler `P01=20` | `Retornou P01=20 sem erro` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R05** | Saída válida (`5` un. de `15`) | `true`, log `P01;5` e saldo `10` | `ok=true, saldoRestante=10, log='P01;5'` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R06** | Saída no limite exato (`15 == 15`) | `true` e saldo zerado (`0`) | `ok=true, saldoRestante=0` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R07** | Saída maior que o saldo (`16 > 15`) | `false` e saldo `15` mantido | `ok=false, saldoRestante=15` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R08** | Saída com quantidade `0` ou `-5` | Lançar `IllegalArgumentException` | `rejeitouZero=true, rejeitouNegativo=true` | **✅ Passou** | `evidencias/execucao_revisado.txt` |
| **CT-R09** | Consulta com `estoque.csv` inexistente | Lançar `UncheckedIOException` | `lancouErro=true` | **✅ Passou** | `evidencias/execucao_revisado.txt` |

### 7.4. Estratégia de Simulação (*Test Doubles*)
* **O que é difícil simular naturalmente:** erros de disco no meio da leitura de uma linha (`C5` de `consultarQuantidade`) ou durante a gravação (`C4` de `registrarSaida`), pois dependem de falha física ou perda de rede.
* **Como foi resolvido:**
  1. **Isolamento de arquivos:** cada teste recria e apaga `estoque.csv` e `movimentacoes.txt` antes e depois da execução, e a versão revisada também aceita caminhos customizados (`Path`) via construtor para uso com pastas temporárias.
  2. **Indução de erro de escrita:** no `CT-11`, foi criada uma pasta chamada `movimentacoes.txt` para forçar o Java a lançar `IOException` ao tentar abrir o arquivo para escrita.
  3. **Evolução com Mocks/Stubs:** em projetos maiores, pode-se extrair uma interface de leitura/escrita e usar *mocks* (ex.: Mockito) para simular `IOException` exatamente na 2ª linha lida.

---

## 8. Correções Implementadas e Comparação Antes vs. Depois

### O que foi corrigido em `revisado/EstoqueArquivo.java`
1. **Fechamento automático (`try-with-resources`):** uso de `Files.newBufferedReader` e `Files.newBufferedWriter` com `UTF-8`, garantindo que os dados sejam gravados no disco e os arquivos sejam fechados automaticamente.
2. **Desconto real no estoque:** ao registrar uma saída válida, o novo saldo é calculado e salvo de volta em `estoque.csv`.
3. **Validação das entradas:** bloqueio de código nulo/vazio (`validarCodigo`) e de quantidades menores ou iguais a zero (`quantidade <= 0`).
4. **Tratamento claro de erros:** remoção dos `catch` vazios, propagação de falhas de I/O via `UncheckedIOException` e proteção contra linhas malformadas no CSV.
5. **Documentação e organização:** adição de comentários Javadoc, construtor com `Path` configurável e `synchronized` nos métodos.

### Comparativo Técnico (Original vs. Revisado)

| Aspecto | Código Original (`original/`) | Código Revisado (`revisado/`) | Melhoria Obtida |
| :--- | :--- | :--- | :--- |
| **Confiabilidade** | Não fecha arquivos (`0 bytes` no log) e não desconta o saldo do CSV. | Fecha arquivos automaticamente e atualiza o saldo restante em `estoque.csv`. | Evita perda de dados e mantém o estoque real sincronizado. |
| **Segurança** | Aceita `null` e quantidades negativas (`-5`). | Valida código e exige `quantidade > 0` antes de processar. | Impede registros inválidos ou negativos no histórico. |
| **Tratamento de Erros** | `catch (IOException)` vazio esconde erros; CSV inválido trava o programa. | Lança `UncheckedIOException` em falhas de disco e ignora linhas corrompidas. | Facilita identificar problemas reais de arquivo sem quedas inesperadas. |
| **Legibilidade** | Sem comentários Javadoc e caminhos fixos no código. | Javadoc completo, `UTF-8` explícito e construtor flexível com `Path`. | Código mais fácil de ler, manter e testar isoladamente. |
| **Complexidade $V(G)$** | `consultarQuantidade`: $5$<br>`registrarSaida`: $4$ | `consultarQuantidade`: $8$<br>`registrarSaida`: $10$ | **Justificativa:** o aumento de $V(G)$ é esperado e positivo, pois resulta das validações de entrada, checagens de formato do CSV e tratamento de exceções que faltavam no original. |

---

## 9. Limitações

* **Dois arquivos separados sem transação única:** como os dados ficam em dois arquivos de texto (`estoque.csv` e `movimentacoes.txt`), uma queda de energia exata entre a gravação do primeiro e do segundo arquivo pode deixá-los dessincronizados (algo que só seria totalmente evitado com um banco de dados transacional).
* **Uso simultâneo por programas externos:** o modificador `synchronized` protege várias *threads* dentro do mesmo programa Java, mas não bloqueia o arquivo caso outro programa externo edite o CSV ao mesmo tempo.
* **Arquivos muito grandes:** a atualização do saldo carrega as linhas do CSV em memória, funcionando muito bem para estoques pequenos e médios, mas não sendo indicada para milhões de produtos.

---

## 10. Conclusão e Referências

A combinação da análise estática com os testes estruturais de caixa-branca permitiu localizar falhas graves que passariam despercebidas apenas olhando o retorno booleano dos métodos — como o arquivo de movimentações vazio por falta de `close()` e a ausência de abatimento do estoque. Os testes automatizados validaram todas as 9 verificações na versão revisada, garantindo segurança, integridade dos dados e rastreabilidade completa no repositório.

### Referências
* MCCABE, T. J. *A Complexity Measure*. IEEE Transactions on Software Engineering, v. SE-2, n. 4, p. 308–320, 1976.
* PRESSMAN, R. S.; MAXIM, B. R. *Engenharia de Software: Uma Abordagem Profissional*. 9. ed. Porto Alegre: AMGH, 2021.
* ORACLE. *The Java™ Tutorials — The try-with-resources Statement*. Disponível em: https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html.
