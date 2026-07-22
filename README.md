# Herobrine - Plugin Minecraft Spigot/Paper

Um plugin completo, imersivo e de alta fidelidade para servidores Minecraft Spigot e Paper (1.20.x / 1.21.x em Java 17+ / Java 21), trazendo a lendária entidade **Herobrine** para o seu servidor.

Desenvolvido sob o pacote `com.euandrelucas.herobrine`.

---

## 🌟 Funcionalidades

### 1. Invocação & Spawn Inteligente
- **Spawn Ambiente Automático**: Manifestação em intervalos configuráveis com variação aleatória perto de jogadores fora da visão direta inicial.
- **Ritual do Altar**: Construa a estrutura (base 3x3 de blocos de ouro/cobblestone musgoso + netherrack central + 4 tochas de redstone) e acenda fogo sobre a netherrack para invocar o Herobrine com um efeito de raio visual.
- **Regra de Unicidade**: Máximo de apenas **1** Herobrine ativo por mundo por vez.

### 2. Máquina de Estados Finita da IA
O mob transiciona dinamicamente entre 8 estados guiados por um nível de ameaça/raiva:
1. `WATCHING`: Observa à distância e teletransporta ao ser olhado diretamente.
2. `DWELLING`: Permanece parado em locais escuros perto do jogador.
3. `CREEPING`: Aproxima-se furtivamente.
4. `LURKER`: Muda de posição ativamente para continuar espionando.
5. `SNEAKY_STRIKE`: Desfere um golpe surpresa se ignorado por muito tempo.
6. `HOSTILE`: Ataca abertamente o jogador.
7. `FLEEING`: Teletransporta para longe quando a vida fica baixa e aprende com a derrota.
8. `POSSESSING`: Possui animais passivos próximos.

### 3. Edição de Mundo Paginada (Sem Lag)
Fila de alteração de blocos assíncrona (`max-blocks-per-tick`) respeitando plugins de proteção (WorldGuard, GriefPrevention, Towny):
- Túneis 2x2 em rocha.
- Pirâmides perfeitas de areia em oceanos.
- Remoção de folhas de florestas inteiras.
- Masmorras subterrâneas com armadilhas.
- Tochas de redstone em telhados.
- Cruzes de madeira em florestas.
- Túneis-isca com minério de diamante.
- Placas com mensagens assustadoras.
- Portas fantasmas ("Ghost Doors" abrindo e fechando).
- Explosões controladas e fogo.
- Pinturas assombradas que mudam de arte quando o jogador não olha diretamente.

### 4. Modo de Corrupção do Nether
- Elevação gradual do nível de lava na área do Nether quando a fúria do Herobrine atinge o limiar.
- Comando `/hb restorenether` para reverter a corrupção restaurando o snapshot dos blocos.

### 5. Mobs Possuídos & Ritual de Banimento
- **Animais Possuídos**: Vacas com empurrão, ovelhas emitindo fumaça e galinhas invocando morcegos.
- **Monstros Possuídos**: Zumbis e esqueletos com atributos aumentados.
- **Relíquia de Banimento**: Receita de crafting personalizada (Maçã dourada encantada + Estrela do Nether + Cabeça de Dragão + Blocos de Diamante).

### 6. Terror Psicológico & Sistema de Medo
- Jumpscares visuais/sonoros e tela de erro opcional (client-safe).
- Mensagens de chat escritas ao contrário.
- Pesadelos ao acordar da cama com efeitos leves de cegueira/náusea.
- **Sistema de Medo (0-100)**: Medo individual por jogador armazenado em arquivo YAML (`fear_data.yml`).

---

## 🛠️ Comandos & Permissões

### Comandos (`/herobrine` ou `/hb`)
- `/hb spawn [jogador]` - Força a manifestação do Herobrine.
- `/hb banish [mundo]` - Remove o Herobrine do mundo.
- `/hb jumpscare <jogador>` - Dispara um susto imediato.
- `/hb fear <jogador>` - Mostra o nível de medo do jogador.
- `/hb setfear <jogador> <0-100>` - Define o nível de medo de um jogador.
- `/hb frequency <minutos>` - Ajusta o intervalo de spawn ambiente.
- `/hb restorenether` - Reverte alterações da corrupção do Nether.
- `/hb state` - Exibe o estado e o nível de raiva atual do Herobrine.
- `/hb reload` - Recarrega as configurações (`config.yml`) e mensagens (`messages.yml`).

### Permissões
- `herobrine.admin` - Acesso a todos os comandos administrativos (Padrão: `op`).
- `herobrine.usealtar` - Permissão para invocar o Herobrine usando o Altar Ritual (Padrão: `true`).
- `herobrine.bypass` - Impede que o jogador seja escolhido como alvo de assombrações.
- `herobrine.notify` - Recebe avisos administrativos do plugin.

---

## 📦 Como Compilar

Requisitos: Java 17+ (ou Java 21) instalado.

```bash
# No Windows
mvnw.cmd clean package

# O arquivo JAR compilado será gerado em:
# target/herobrine-1.0.0-SNAPSHOT.jar
```

---

## 📜 Créditos e Isenção de Responsabilidade
Conceito inspirado na clássica creepypasta comunitária de Herobrine (2010-2011) e em mods/plugins clássicos da comunidade Minecraft. Este plugin utiliza exclusivamente recursos e APIs padrão do Minecraft/Bukkit/Paper e não contém assets protegidos por direitos autorais de terceiros.
