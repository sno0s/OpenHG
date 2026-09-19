# Geração de mapas HG

O mapa usa planícies, florestas de carvalho, bétulas e florestas escuras, com
colinas largas e suaves. É um preset inspirado no HG clássico; não reproduz
exatamente o gerador do Minecraft 1.8.

A cada inicialização, o plugin recria somente `hg_world` com uma seed aleatória
nova, registrada no console. Terreno, biomas e muralha usam essa mesma seed.
O centro tem relevo mais plano, e a arena continua com 500 × 500 blocos.

As árvores nativas recebem um reforço por bioma; planícies continuam abertas.
Árvores e cogumelos são registrados antes da geração dos chunks iniciais.
Cavernas, minérios e decorações dos biomas continuam habilitados. Estruturas
vanilla, como vilas, ficam desabilitadas neste mapa de arena.

A muralha tem três blocos de espessura, topo nivelado, ameias, pilares,
torres circulares nos cantos e torres intermediárias simétricas. Uma camada
subterrânea fecha o perímetro. A construção das torres também é dividida
entre ticks, e a WorldBorder é ativada antes da construção física.

## Configuração

Os valores abaixo pertencem a `plugins/HardcoreGamesPlugin/config.yml`:

```yaml
HGconfigs:
  tree-density: 1
  mushroom-density: 40
  terrain:
    generator-version: 2
    base-height: 68
    height-variation: 12
    hill-frequency: 0.006
    biome-frequency: 0.008
    plains-weight: -0.15
    dark-forest-weight: -0.3
  wall:
    height: 10
```

- `height-variation`: diminuir deixa o terreno mais plano (0 a 24).
- `hill-frequency`: diminuir alarga as colinas (0.001 a 0.02).
- `biome-frequency`: diminuir aumenta as regiões de cada bioma (0.001 a 0.05).
- `plains-weight`: aumentar favorece planícies (-1 a 1).
- `dark-forest-weight`: aumentar favorece florestas escuras (-1 a 1).
- `tree-density`: reforço de árvores; 0 mantém só as árvores vanilla,
  1 é o padrão e 2 aumenta o reforço (máximo 8).
- `wall.height`: altura sobre o ponto mais alto do perímetro (6 a 24).

Na primeira execução desta versão, os antigos valores padrão
`biome-frequency: 0.003` e `plains-weight: 0.2` são migrados para o novo
preset. Outros valores personalizados são preservados. A marca
`generator-version: 2` impede que a migração se repita.

## Compilar e instalar

Use JDK 21 e execute `gradlew.bat test shadowJar` dentro de `hardcoregames`.
Instale `build/libs/hardcoregames-1.0.0-all.jar`, que inclui SQLite.
O servidor de desenvolvimento (`runServer`) usa Paper 1.21.11, como a API
do projeto. Reinicie o servidor para gerar um mapa com as novas configurações.

Os testes verificam continuidade e limites do relevo em sete seeds,
reprodutibilidade, presença dos quatro biomas, migração do preset e fechamento
da muralha. Aparência das árvores e decorações deve ser conferida dentro do jogo.
