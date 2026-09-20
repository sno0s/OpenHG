# Geração de mapas HG

O mapa usa planícies, florestas comuns, florestas escuras, jungle e deserto.
O bioma de bétulas foi retirado; árvores individuais de bétula ainda podem fazer
parte das florestas comuns. Há colinas suaves e quatro montanhas localizadas.
É um preset inspirado no HG clássico; não reproduz
exatamente o gerador do Minecraft 1.8.

A cada inicialização, o plugin recria somente `hg_world` com uma seed aleatória
nova, registrada no console. Terreno, biomas e muralha usam essa mesma seed.
O centro tem relevo plano, e a arena tem 750 × 750 blocos por padrão.
`HGconfigs.world-size` define o lado da arena: borda, muralha, sorteio do feast,
regiões obrigatórias dos biomas, montanhas e ravinas usam esse tamanho.
São aceitos valores pares entre 256 e 10000; valores inválidos usam 750 com aviso.

Cada seed tem uma região garantida de cada um dos cinco biomas, com bordas
irregulares. Fora dessas regiões, a distribuição segue o ruído de temperatura e
umidade. As quatro montanhas acrescentam até 24 blocos ao relevo padrão e ocupam
uma parte pequena da arena; `height-variation: 0` também desliga as montanhas.
Três ravinas de superfície são garantidas por seed, longe do centro e da muralha.
No tamanho padrão, têm cerca de 90–130 blocos de comprimento, 6–10 de largura
central e 24–34 de profundidade. Posição, curvatura e dimensões variam com a seed.

O reset usa a pasta retornada por `World.getWorldFolder()`. Isso cobre tanto
`hg_world/` nas versões antigas quanto
`world/dimensions/minecraft/hg_world/` no Paper 26. O plugin primeiro
carrega/localiza a arena com seu gerador, descarrega sem salvar, remove
somente seus dados e recria o mundo. Se o descarregamento falhar, nada é
apagado. O caminho real e as seeds anterior/nova aparecem no console.

As árvores nativas recebem um reforço por bioma; planícies continuam abertas.
Os listeners são registrados antes da geração dos chunks iniciais. Depois da
geração, no próximo tick, a limpeza remove grama, flores e folhas secas
(`LEAF_LITTER`), inclusive sob copas. A altura é medida com `WORLD_SURFACE`,
que inclui as plantas baixas ignoradas pelo heightmap de movimento.

Os cogumelos voltaram à colocação no chunk pronto com `setType(..., false)`,
como no populator do commit `1ce5691`. O valor `mushroom-density` agora é o alvo
de quantidade, e não um número de tentativas que podem falhar: 40 tenta atingir
40–50 cogumelos por chunk, limitado ao solo livre disponível. As posições não
se repetem, e cogumelos existentes contam para o alvo. Plains também recebe o
populator. A jungle não recebe cogumelos pequenos, e os nativos são removidos
na limpeza; o solo de areia do deserto não é elegível.

O cacau é reforçado nos troncos da jungle, entre um e cinco blocos acima do solo,
com frutos maduros. `cocoa-density: 12` adiciona até 12 frutos por chunk quando
há troncos e espaço; 0 mantém somente o cacau vanilla. Um marcador persistente
no chunk impede a reposição de cogumelos/cacau coletados ao recarregar a área.
A limpeza continua sendo aplicada a cada carregamento.

Cavernas, minérios, cactos e demais decorações dos biomas seguem habilitados.
Vilas e templos do deserto podem aparecer segundo as regras e o espaçamento
vanilla de cada seed; sua presença dentro da arena não é garantida.
As demais estruturas são bloqueadas apenas em `hg_world`.

Animais e monstros usam 25% dos limites de população normais apenas em `hg_world`.
Os limites inteiros são arredondados para baixo; os animais criados durante a
geração dos chunks também recebem 25% da probabilidade normal. A quantidade
observada varia conforme a seed, jogadores, categorias e regras naturais de spawn.

A muralha tem três blocos de espessura, topo nivelado, ameias, pilares,
torres circulares nos cantos e torres intermediárias simétricas. Uma camada
subterrânea fecha o perímetro. A construção das torres também é dividida
entre ticks, e a WorldBorder é ativada antes da construção física.

## Configuração

Os valores abaixo pertencem a `plugins/HardcoreGamesPlugin/config.yml`:

```yaml
HGconfigs:
  world-size: 750
  mob-spawn-multiplier: 0.25
  tree-density: 1
  mushroom-density: 40
  cocoa-density: 12
  terrain:
    generator-version: 3
    base-height: 68
    height-variation: 12
    hill-frequency: 0.006
    biome-frequency: 0.008
    plains-weight: -0.15
    dark-forest-weight: -0.3
  wall:
    height: 10
```

- `world-size`: lado da arena quadrada; aplique a mudança reiniciando o servidor.
- `mob-spawn-multiplier`: 0 desliga spawn natural/geração, 0.25 reduz 75%, 1 normal.
- `height-variation`: diminuir deixa o terreno mais plano (0 a 24); montanhas somam até 2× esse valor.
- `hill-frequency`: diminuir alarga as colinas (0.001 a 0.02).
- `biome-frequency`: diminuir aumenta as regiões de cada bioma (0.001 a 0.05).
- `plains-weight`: aumentar favorece planícies (-1 a 1).
- `dark-forest-weight`: aumentar favorece florestas escuras (-1 a 1).
- `tree-density`: reforço de árvores; 0 mantém só as árvores vanilla,
  1 é o padrão e 2 aumenta o reforço (máximo 8).
- `mushroom-density`: alvo de cogumelos por chunk, mais 0–10; 0 desliga o reforço.
- `cocoa-density`: limite de frutos extras por chunk de jungle, de 0 a 256.
- `wall.height`: altura sobre o ponto mais alto do perímetro (6 a 24).

Na primeira execução desta versão, os antigos valores padrão
`biome-frequency: 0.003` e `plains-weight: 0.2` são migrados para o novo
preset. Outros valores personalizados são preservados. A marca
`generator-version: 3` impede que a migração se repita.
As novas chaves `world-size` e `mob-spawn-multiplier` são adicionadas também às
configurações existentes, sem substituir valores já definidos. `cocoa-density`
também é adicionada às configurações existentes com valor 12.

## Receitas de sopa

As duas receitas não exigem ordem na grade e funcionam na grade 2×2 do jogador:

- Uma tigela + um cacau (`COCOA_BEANS`) → uma sopa de cogumelos.
- Uma tigela + um cacto (`CACTUS`) → uma sopa de cogumelos.

O resultado é `MUSHROOM_STEW` normal, portanto usa a mesma cura, alimentação e
devolução da tigela do `SoupListener`. A receita vanilla de cogumelos continua.

## Compilar e instalar

Execute `gradlew.bat test shadowJar` dentro de `hardcoregames`. O Gradle pode ser
iniciado com o Java 21 existente e obtém automaticamente o JDK 25 para compilar
e testar. O primeiro build precisa de acesso à internet para baixar a toolchain.
Instale `build/libs/hardcoregames-1.0.0-all.jar`, que inclui SQLite.
O servidor de desenvolvimento (`runServer`) usa Paper 26.2, como a API
do projeto. Reinicie o servidor para gerar um mapa com as novas configurações.

Os testes verificam continuidade e limites do relevo, reprodutibilidade,
presença dos cinco biomas por seed em diferentes tamanhos, ravinas, migração do
preset, fechamento da muralha, limpeza sob árvores, cogumelos e redução de mobs.
Aparência das árvores e decorações deve ser conferida dentro do jogo.

## Jenkins e Crafty

A pipeline compila, executa os testes e publica os XMLs JUnit e o relatório HTML.
Depois empacota e valida `hardcoregames-1.0.0-all.jar`, incluindo SQLite, antes
de copiar esse mesmo arquivo para `/hg-server`. Falhas impedem o deploy.
O parâmetro `JAVA_HOME_PATH` mantém `/opt/jdk-21` para iniciar o Gradle no agente
existente. O JDK 25 usado pela compilação e pelos testes é provisionado pelo
Gradle. A pipeline precisa dos plugins JUnit e Email Extension.

`/restarthg check` consulta o servidor no Crafty sem reiniciá-lo. A consulta
confirma acesso, mas não garante a permissão de comandos exigida pelo reinício.
`/restarthg` envia o POST de reinício após a contagem configurada. Requisições
simultâneas são bloqueadas; falhas liberam uma nova tentativa manual.

O cliente aceita URL HTTP ou HTTPS, normaliza barra final e sufixo `/api/v2`,
aceita a chave com ou sem `Bearer` e usa timeout de 10 segundos para conexão
e leitura. Mantém o suporte existente ao certificado autoassinado local do Crafty.
O console distingue HTTP 401 (token), 403 (permissão), 404 (endereço/servidor),
redirecionamentos, timeout, DNS, falta de rota, `ConnectException` e erros TLS.
O check informa o endereço e a porta realmente usados. As mensagens ao jogador
passam pelo utilitário `Messages`, com o prefixo e as cores do projeto.
Tokens e corpos completos não são registrados.

O endereço configurado é `https://10.170.184.252:8111`; o IP final `.252` foi
confirmado. Não inclua `/#/` na URL da API. Na verificação local, a porta aceitou
TCP, mas o handshake HTTPS expirou. Existe rota para `10.170.184.0/24` pela
ZeroTier. Não houve chamada de reinício ao servidor real; execute o diagnóstico
no ambiente do servidor para confirmar a causa operacional.

O endpoint documentado de reinício é
`POST /api/v2/servers/{serverID}/action/restart_server`, com
`Authorization: Bearer <api-key>` e permissão de comandos no servidor.
O check usa `GET /api/v2/servers/{serverID}`. `ConnectException` acontece antes
de obter uma resposta HTTP: trocar token ou caminho da API não resolve uma
porta inacessível. O endereço precisa funcionar a partir do processo Minecraft,
não apenas do navegador do administrador.

O padrão do Crafty é HTTPS na porta 8443. A porta 8111 pode ser um mapeamento
externo, por exemplo `8111:8443`. Se Minecraft e Crafty estiverem no mesmo
container, o endereço interno costuma ser `https://127.0.0.1:8443`; se estiverem
em containers separados, use o nome/IP e a porta acessíveis pela rede deles.
Não foi feita troca automática do endereço, pois depende dessa instalação.

Referências: [Paper 26.2 e Java](https://docs.papermc.io/paper/dev/project-setup/),
[API v2 do Crafty](https://docs.craftycontrol.com/pages/developer-guide/api-reference/v2/).
Veja também [portas e mapeamentos Docker](https://docs.craftycontrol.com/pages/getting-started/installation/docker/).
