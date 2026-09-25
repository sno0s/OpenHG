# Mensagens, nome e cores

O plugin cria arquivos separados em `plugins/HardcoreGamesPlugin/` ao iniciar:

| Arquivo | Conteúdo |
| --- | --- |
| `items.yml` | Nomes, lores e descrições dos itens e dos kits (Kangaroo, Lumberjack, Fisherman, Stomper) |
| `menus.yml` | Títulos e ícones do menu de kits e do menu de estatísticas |
| `feast.yml` | Lista `loot` do Feast, com totais globais para os 12 baús |
| `messages.yml` | Chat, mensagens do Feast, ajuda dos comandos e diagnósticos do console/Crafty |

O `config.yml` continua responsável pelas regras da partida, geração do mundo e
conexão com serviços.

Edite o arquivo gerado e reinicie o servidor para aplicar. Exemplo:

```yaml
server-name: "&bMeu servidor"
prefix: "&8[{server-name}&8] &r"
colors:
  msg: "&f"
  highlight: "&e"
  error: "&c"
  success: "&a"
  broadcast: "&6"
```

O prefixo pode conter qualquer texto, colchetes ou outro formato. Use `prefix: ""`
para ocultá-lo. São aceitos códigos de cor `&0` a `&f`, estilos como `&l` e `&o`,
e `&r` para limpar a formatação. Os códigos antigos com `§` também funcionam.

## Traduções e parâmetros

Cada mensagem tem uma chave estável, independente do idioma. Traduza o valor e
preserve os parâmetros entre chaves; é possível mudar a posição deles:

```yaml
kits:
  selected: "Kit {highlight}{kit}{success} selecionado!"
match:
  countdown: "Partida começando em {highlight}{seconds}s{msg}!"
```

`{player}`, `{seconds}`, `{kit}`, coordenadas e outros parâmetros recebem os
valores indicados pelo contexto. `{msg}`, `{highlight}`, `{error}`, `{success}`
e `{broadcast}` recebem as cores da paleta. Uma mensagem de chat vazia (`""`)
é silenciada. Descrições de itens usam listas YAML, com uma linha por entrada.

A configuração vale para todos os jogadores; a estrutura permite manter versões
do arquivo em outros idiomas. Seleção automática de idioma por jogador ainda não
está implementada. A identificação dos itens e menus independe dos textos visíveis.
O identificador interno do kit continua válido no comando `/kit`, que também aceita
seu nome traduzido, inclusive com espaços.

## Kits

Cada kit tem um bloco `items.<kit>` com `name` e `lore`. Esse bloco
é a fonte única: o ícone no menu de kits, o item entregue pelo kit e as mensagens
de chat usam o mesmo texto. Ao implementar um kit novo, adicione seu bloco no `items.yml`.

```yaml
items:
  kangaroo:
    name: "&6Kangaroo"
    lore:
      - "&7Clique para saltar!"
```

Descrições legadas são preservadas como campo opcional `description`. Se não havia lore, a descrição vira sua primeira linha.

O `menus.yml` monta o ícone com `{kit}` (o `name`) e uma linha `{lore}`, expandida para todas as linhas da lore do item. `{description}` continua disponível para templates personalizados antigos. Um kit novo também requer sua implementação e registro no código.

## Instalações existentes

Na primeira criação de `messages.yml`, o nome/tag e as cores antigos de
`HGconfigs.server-name` e `HGconfigs.colors` são importados. A tag antiga inteira
é preservada em `server-name`, com `prefix: "{server-name} "` para não duplicar
os colchetes. A partir daí, altere esses valores somente em `messages.yml`.

Quem já tinha um `messages.yml` único recebe a separação automaticamente: os
textos de `items` e `menus` são movidos para os novos arquivos com as
personalizações intactas, e o loot sai de `HGconfigs.feast-loot` para
`feast.yml`. Antes de reescrever qualquer arquivo, a migração guarda uma cópia
`<arquivo>.<data>-<hora>-<identificador>.bak` na mesma pasta. A migração roda uma única vez;
reinícios seguintes não movem nada. Conflitos mantêm o valor do arquivo novo e geram aviso; o valor antigo permanece no backup.

Algumas chaves mudaram de lugar e são migradas junto, mantendo o valor
personalizado: `items.rocket.*` e `kits.kangaroo.*` viraram `items.kangaroo.*`,
`kits.lumberjack.*` virou `items.lumberjack.*` e `items.cannot-drop` virou
`common.cannot-drop`. Um valor já presente no destino nunca é sobrescrito.

Atualizações acrescentam chaves ausentes sem substituir os textos personalizados.
Um arquivo com erro de sintaxe não é sobrescrito; o carregamento falha indicando
o arquivo, para permitir a correção. Uma lista `loot` malformada é reportada no
console e os baús ficam vazios, em vez de voltar em silêncio para os padrões.
Mensagens nativas do Minecraft, de outros plugins e o cabeçalho técnico que o
Paper acrescenta aos logs não fazem parte deste catálogo.
