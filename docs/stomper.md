# Kit Stomper

O Stomper é passivo e não entrega item. Ao pousar de uma queda durante a partida,
recebe meio coração de dano e, a partir da altura mínima configurada, causa dano
verdadeiro aos jogadores vivos próximos. O dano escala linearmente com a altura:
`altura_da_queda / altura_mínima * 20` pontos de vida. Assim, metade da altura
mínima causa metade do dano letal. O dano não é reduzido por armadura e o raio
padrão é de 7 blocos.

As opções ficam em `HGconfigs.stomper.impact-radius` e
`HGconfigs.stomper.minimum-fall-height` no `config.yml`.
