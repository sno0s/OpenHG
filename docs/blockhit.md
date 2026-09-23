# Bloqueio clássico com espada

Minecraft Java/Paper 26.2. Segure o botão direito com uma espada na mão principal.
O bloqueio começa imediatamente e termina ao soltar o botão ou trocar de item.
O cooldown de ataque já era removido pelo OpenHG e continua assim.

## Mecânica

Em config.yml, HGconfigs.combat.sword-blocking.enabled liga/desliga a função;
damage-reduction vale 1.0: um ponto de dano base (meio coração) antes de armadura,
encantamentos e absorção. É um desconto fixo, não 50% ou invulnerabilidade.
O Minecraft limita a redução ao dano recebido. São contemplados golpes, flechas,
projéteis comuns e explosões; quedas, fogo, fome e dano ambiental ficam inalterados.
O bloqueio cobre todas as direções, não gasta durabilidade adicional e não recebe
o cooldown de desativação de escudo por machado, nem o recuo adicional de escudo. Nomes, lores e encantamentos ficam
preservados. Componentes de bloqueio de outros plugins não são substituídos.

Crafting e coleta são tratados diretamente; alterações de inventário (incluindo
Feast, baús, comandos e plugins) são consolidadas no tick seguinte. Não há varredura
periódica global. Ao selecionar uma espada, ela também é atualizada imediatamente.

## Visual

O ZIP openhg-blockhit-26.2.zip é produzido junto com shadowJar.
A pose BLOCK em primeira pessoa usa o cliente 26.2. O pack ajusta a pose em terceira
pessoa para a referência 1.7, inclusive mão esquerda e espada de cobre.
Fora do bloqueio, os modelos normais são preservados. Nenhuma textura é distribuída.
Licença/atribuição: resourcepacks/blockhit/CREDITS.txt.

Publique o ZIP por HTTPS e configure resource-pack, resource-pack-sha1 e
resource-pack-prompt em server.properties, com backup. O pack é opcional;
sem aceitá-lo, a defesa funciona, mas o ajuste visual de terceira pessoa não aparece.

O cliente moderno não reproduz a continuação da animação de ataque durante o bloqueio
da 1.7: é preciso alternar ataque e bloqueio. Um plugin/resource pack não altera esse
comportamento do cliente; a reprodução exata exige mod. Esta entrega aproxima a
mecânica e a pose, sem modificar todo o combate.

## Validação

Testar espada comum/encantada, troca de slot, crafting, coleta e retirada de baú;
comparar golpes com/sem bloqueio (sem armadura: diferença máxima de 1 ponto).
Checar fogo/queda sem redução e ausência de desgaste adicional.
No cliente 26.2, aceitar o pack e conferir primeira e terceira pessoa.
