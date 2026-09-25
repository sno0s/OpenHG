# Kit Fisherman

Selecione pelo menu de kits ou por `/kit Fisherman` antes da partida. Ao começar,
o jogador recebe uma vara de pesca no primeiro slot, sem desgaste e protegida
contra descarte, seguindo o padrão dos itens de habilidade dos outros kits.

Lance a linha com o botão direito, fisgue um jogador e recolha a vara com outro
clique direito. O alvo é teleportado diretamente para a localização do Fisherman.
O alcance é o da pesca normal; não há cooldown adicional, bônus de dano,
redução de dano ou passiva.

A habilidade exige partida em andamento, ambos vivos e em sobrevivência, sem
invulnerabilidade, dentro da borda de `hg_world`. Não funciona com espectadores,
eliminados, jogadores de outros mundos nem durante a invencibilidade inicial.
Um uso negado da habilidade também não aplica o puxão vanilla. Eventos de pesca
e teleporte cancelados por outros plugins são respeitados.

Apenas a vara identificada do kit ativa o teleporte; uma vara comum mantém seu
comportamento normal. Pescar itens ou entidades que não sejam jogadores também
mantém o comportamento normal. Não há timers ou estado de habilidade persistente
entre capturas, mortes e partidas. As regras gerais do servidor sobre uso da mão
secundária continuam valendo.

Nome e lore vêm de `items.fisherman` no `items.yml` e são compartilhados pelo
item e pelo ícone do menu. Reiniciar acrescenta as chaves novas sem substituir
textos personalizados. Alterar o nome visível não muda a identidade da vara.

## Verificação no servidor

Os testes automatizados cobrem teleporte, identidade, catálogo, descarte,
fases da partida, invulnerabilidade, espectadores, eliminação, borda e
cancelamentos. No Paper 26.2 com dois clientes, conferir também:

- Selecionar o kit e receber a vara ao iniciar a partida.
- Fisgar e recolher jogadores após a invencibilidade; testar capturas repetidas.
- Conferir a localização final, animação da linha e ausência de puxão extra.
- Morrer/desconectar enquanto a linha está lançada e confirmar que a habilidade
  não afeta quem ficou espectador ou deixou a partida.
- Conferir interação com plugins de proteção/teleporte usados no servidor real.

Referência da API: [PlayerFishEvent](https://jd.papermc.io/paper/26.2/org/bukkit/event/player/PlayerFishEvent.html).
