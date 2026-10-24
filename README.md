# Herobrine 2.0 (plugin Paper 1.21.9+)

Herobrine no estilo do *The Herobrine Mod*, mas 100% plugin. Reescrito do zero.

## Como funciona

- **Corpo de verdade**: usa a entidade `Mannequin` (Paper 1.21.9+) com skin de jogador. Cada aparição é **visível só para o jogador-alvo**.
- **Presença oculta (0-100)** por jogador, salva no próprio jogador. Sobe com o tempo jogado e com o comportamento; o jogador nunca vê o número.
  - 0-15: só sons (cave sounds, respiração, passos atrás de você)
  - 15-35: avistamentos ao longe, janelas, tochas apagadas, portas, animais encarando
  - 35-55: árvores sem folhas, túneis 1x2, placas, cruzes, netherrack em chamas, tempestade
  - 55-75: perseguições e stalking (some quando você olha, reaparece mais perto)
  - 75+: encontros diretos (opcional: combate)
- **Aprendizado**: minera muito no subsolo → túneis misteriosos; passa a noite em casa → ele aparece na janela; dorme → acorda com placa, tocha apagada ou porta aberta.
- **Altar**: 3x3 de blocos de ouro, netherrack no centro, 4 tochas de redstone em volta, acenda o fogo.
- Nada destrutivo: não apaga inventário, e as alterações de bloco usam fila (blocos por tick configurável).

## Comandos (`/hb`, permissão `herobrine.admin`)

`/hb spawn <stare|window|stalk|chase|hostile> [jogador]` · `/hb event <id> [jogador]` · `/hb presence <jogador> [set N]` · `/hb banish [jogador|all]` · `/hb reload` · `/hb info`

Para testar rápido: `/hb presence <você> set 80` e `/hb event sighting`. Em criativo o diretor automático não age (use os comandos).

## Skin

Por padrão usa a skin da conta `Herobrine`. Para olhos brancos, gere uma skin assinada em mineskin.org e cole `texture-value` / `texture-signature` em `config.yml`.

## Compilar

```bash
./maven/apache-maven-3.9.6/bin/mvn clean package
```
Saída: `target/Herobrine-2.0.0.jar`. Requer Java 21.
