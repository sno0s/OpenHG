# Mine-agent — OpenHG

Este arquivo orienta novos chats e agentes que trabalham no repositório OpenHG.
Ele complementa `CLAUDE.md` e `.openhg-context/context.md`; o código e os
comentários mais recentes do Vikunja têm precedência quando houver conflito.

## Objetivo do agente

Manter e evoluir o plugin HardcoreGames para o servidor Paper do projeto,
entregando alterações pequenas, testadas e documentadas. Antes de implementar,
ler a tarefa e os comentários atuais no Vikunja. Depois de implementar, registrar
o que foi feito, os testes e qualquer limitação no card correspondente.

## Projeto e ambiente

- Repositório: `/DATA/Documents/OpenHG`.
- Módulo Gradle: `/DATA/Documents/OpenHG/hardcoregames`.
- Plugin: `hardcoregames-1.0.0-all.jar`.
- Servidor de teste: Crafty, servidor chamado `HG`.
- Diretório de plugins no servidor: `/hg-server` dentro do Jenkins e a pasta
  persistida do servidor Crafty.
- Jenkins job: `OpenHG`; a pipeline valida, testa, empacota e copia o JAR.
- Projeto Vikunja OpenHG: id `3`; Feast: task id `3`.

Não colocar tokens, chaves do Crafty ou credenciais no Git, nos comentários do
Vikunja, em logs ou neste arquivo. Usar a configuração já existente no ambiente
quando for necessário testar integrações.

## Fluxo de trabalho

1. Ler `CLAUDE.md`, `.openhg-context/context.md`, a documentação relacionada e
   o card do Vikunja.
2. Separar observações verificadas, inferências, dúvidas e critérios de aceite.
3. Para revisão ou propostas, usar o Claude Code Anthropic instalado no servidor
   por `/DATA/AppData/openhg-mcp/claude-review.sh`.
4. Para implementação delegada, usar
   `/DATA/AppData/openhg-mcp/claude-implement.sh` em worktree isolada. O Claude
   propõe ou implementa; o Mine-agent revisa o diff antes de integrar em `main`.
5. Fazer a menor alteração coerente com o card. Não mudar status, descrição,
   prioridade ou escopo de cards sem pedido explícito.
6. Rodar `git diff --check` e deixar a pipeline Jenkins executar a validação
   completa. Não compilar repetidamente localmente se o Jenkins estiver
   disponível; o comando de referência é `./gradlew --no-daemon test shadowJar`
   dentro de `hardcoregames`.
7. Só considerar deploy concluído quando o build Jenkins passar. Registrar no
   Vikunja o commit, build, testes e deploy.

## Convenções de código

- Usar Java simples, nomes claros e métodos pequenos.
- Preservar compatibilidade com Paper/Bukkit 26.2 e Java 21 do agente Jenkins.
- Preferir validação defensiva e mensagens pelo utilitário `Messages`.
- Não esconder falhas de configuração: registrar o problema no log e preservar
  valores personalizados.
- Adicionar testes significativos para regras de distribuição, limites,
  migrações e comportamento de itens; evitar testes que apenas repetem a linha
  implementada.

## Feast

- O Feast tem 12 baús.
- `HGconfigs.feast-loot` usa totais globais exatos antes da distribuição.
- `amount: 64` significa 64 itens no Feast inteiro, não em cada baú.
- Itens não empilháveis podem ocupar vários slots; a capacidade total nunca pode
  ser excedida.
- Livros encantados usam:

  ```yaml
  - material: ENCHANTED_BOOK
    amount: 1
    enchantments:
      SHARPNESS: 1
  ```

- Itens normais também podem receber encantamentos. Chaves `minecraft:...` são
  aceitas; nomes ou níveis inválidos devem gerar aviso e ser ignorados.
- YAML aceita somente espaços na indentação. Nunca inserir TAB em `config.yml`;
  um TAB faz o Paper rejeitar o arquivo e pode fazer o plugin voltar aos defaults.
- `config.yml` é editável no servidor. O `ConfigManager` deve mesclar apenas
  chaves reais ausentes do template, preservar configurações existentes e não
  chamar `saveConfig()` em todo boot.

## Outros escopos conhecidos

- Lumberjack: machado de madeira sem encantamento, durabilidade infinita,
  quebra somente logs conectados da árvore, não pode ser dropado ou colocado em
  baú.
- Barreira: altura configurável sobre o maior relevo; contato causa 6 pontos de
  dano por segundo respeitando invulnerabilidade.
- Geração de mundo: preservar as regras documentadas em `docs/world-generation.md`
  e não reintroduzir valores antigos sem decisão no Vikunja.

## Comunicação

Comentários no Vikunja devem ser curtos e factuais. Usar `[CODEX]` ou `[CLAUDE]`
com `REVIEW`, `IMPLEMENTAÇÃO` ou `TESTE` quando ajudar a distinguir autoria.
Não duplicar comentários anteriores e não atribuir uma opinião a outro agente
sem referência direta.
