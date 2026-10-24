# Herobrine 2.0 (plugin Paper 1.21.9+)

Herobrine no estilo dos mods *The Legend of Herobrine*, *Herobrine's Omen*, *Herobrine Awakens* e *The End of Herobrine*,
mas 100% plugin. Reescrito do zero.

## Como funciona

- **Corpo de verdade**: entidade `Mannequin` (Paper 1.21.9+) com skin de jogador. Cada aparição é **visível só para o jogador-alvo** (o chefe é visível para todos).
- **Presença oculta (0-100)** por jogador, salva no jogador. Sobe com tempo jogado e comportamento; o jogador nunca vê o número. Ela define a **fase**:
  | Fase | Presença | O que acontece |
  |---|---|---|
  | 0 | < 15 | sons, passos, mensagens falsas de chat/log, placas, tocha de redstone aos seus pés, pirâmides de pedra musgosa, cruzes de bedrock falso |
  | 1 | < 35 | avistamentos (Espião), janelas, tochas apagadas, portas, animais encarando, chest com "HI", "Who are you?", Herobrine falso que entra no chat (responde se você falar em 30 s), túneis com tochas de redstone, árvores sem folhas, mobs infectados |
  | 2 | < 55 | Weeping (só anda quando você não olha), Stalker, Sky Stalker, Flash (aparece e some), diamante falso que explode, casas armadilhadas, estátuas, letras "HEROBRINE", tempestade, bedrock flutuante, santuários em ruínas, bases de sobreviventes |
  | 3 | < 75 | Círculo de Herobrines, Herobrine Construtor (visível, constrói de verdade), perseguição com susto, Knockbacker (graveto), "You can't hide", falso shutdown / erros de OpenGL |
  | 4 | 75+ | Mago (ilusões, raios, teleporte), "LEAVE", Guerreiro/Caçador em combate (opcional) |
- **Aprendizado**: minera muito no subsolo → túneis; passa a noite em casa → janela; dorme → acorda com placa, tocha apagada ou porta aberta.
- **Chefe**: acenda fogo no altar (3x3 de ouro, netherrack no centro, 4 tochas de redstone). 300 de vida, 3 fases (raios, ilusões, servos), Espada de Bedrock, teleporte ao ser acertado, barra de vida. Derrotá-lo dá loot e zera a presença de quem estava por perto.

## Itens (sem resource pack, identificados por tag)

`/hb give <item>`: Pó Amaldiçoado, Diamante Amaldiçoado, Diamante Purificado, Água Benta, Água Profana, Espada de Bedrock, Coração do Herobrine e o conjunto de **ferramentas/armadura de Diamante Amaldiçoado** (espada com raio, picareta que funde, pá com toque suave, enxada fertilizante, machado de fúria, armadura com efeitos e bônus de conjunto). Todos têm receita de crafting (menos Bedrock Sword e Coração):

- Diamante Amaldiçoado: diamante + 2 pó amaldiçoado (pó cai de mobs infectados/possuídos, do chefe e de sobreviventes).
- Água Benta: garrafa + ouro + glowstone + lágrima de ghast (arremessável; cura infectados, fere Herobrine e remove ilusões).
- Diamante Purificado: diamante amaldiçoado + 2 água benta + estrela do Nether.

**Altar**: clique direito no netherrack do altar com Diamante Amaldiçoado = desperta o mundo; com Diamante Purificado = purifica e expulsa o Herobrine do mundo.

## Mobs

- **Infectados**: porco (investe), vaca (encara e depois investe com knockback forte), aldeão (corre), ovelha/galinha (encaram). Curados com Água Benta.
- **Monstros possuídos**: zumbi/esqueleto/aranha mais fortes, brilhando.
- **Sobreviventes**: NPCs com troca (ouro → pó amaldiçoado / água benta etc.) nas bases.

## Comandos (`/hb`, permissão `herobrine.admin`)

`spawn <modo> [jogador]` · `event <id> [jogador]` · `presence <jogador> [set N]` · `phase <jogador> <0-4|add|reset>` · `give <item> [jogador] [qtd]` · `world <status|summon|purify|reset> [mundo]` · `banish [jogador|all]` · `reload` · `info`

Teste rápido: `/hb phase <você> 4`, depois `/hb spawn weeping` ou `/hb event statue`. Em criativo/espectador o diretor automático não age (use os comandos).

## Configuração

Tudo em `config.yml`: liga/desliga e peso de cada evento, `jumpscares`, `summon.required` (estilo "Herobrine Always Spawns" ao contrário), `builder`, `boss`, `hostile.break-blocks`, `fake.kick-on-ignore` etc.

## Skin

Por padrão usa a skin da conta `Herobrine`. Para olhos brancos, gere uma skin assinada em mineskin.org e cole `texture-value`/`texture-signature`.

## Limitações (só plugin, sem resource pack)

Sem bioma "Cursed Forest" real (emulado tirando folhas de várias árvores), sem disco "Dog", sem blocos/itens com textura própria e sem animações customizadas. Sem proteção de região (WorldGuard etc.): as construções respeitam apenas os mundos desativados e a permissão `herobrine.bypass`.

## Compilar

```bash
./maven/apache-maven-3.9.6/bin/mvn clean package
```
Saída: `target/Herobrine-2.0.0.jar`. Requer Java 21 e Paper 1.21.9+.
