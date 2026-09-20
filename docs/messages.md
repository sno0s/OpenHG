# Mensagens, nome e cores

O plugin cria `plugins/HardcoreGamesPlugin/messages.yml` ao iniciar. Esse arquivo
concentra os textos de chat, os menus, nomes e descrições dos itens, ajuda dos
comandos e diagnósticos do console/Crafty. O `config.yml` continua responsável
pelas regras da partida, geração do mundo e conexão com serviços.

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

## Instalações existentes

Na primeira criação de `messages.yml`, o nome/tag e as cores antigos de
`HGconfigs.server-name` e `HGconfigs.colors` são importados. A tag antiga inteira
é preservada em `server-name`, com `prefix: "{server-name} "` para não duplicar
os colchetes. A partir daí, altere esses valores somente em `messages.yml`.

Atualizações acrescentam chaves ausentes sem substituir os textos personalizados.
Um arquivo com erro de sintaxe não é sobrescrito; o carregamento falha indicando
o arquivo, para permitir a correção. Mensagens nativas do Minecraft, de outros
plugins e o cabeçalho técnico que o Paper acrescenta aos logs não fazem parte
deste catálogo.
