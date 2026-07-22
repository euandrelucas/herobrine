package com.euandrelucas.herobrine.ai;

/**
 * Define os estados de comportamento da máquina de estados do Herobrine.
 */
public enum HerobrineState {
    /** Observando à distância, na borda da visão do jogador */
    WATCHING,
    
    /** Parado em local fixo (atrás da porta, pé da cama, caverna) */
    DWELLING,
    
    /** Aproxima-se lentamente e furtivamente do jogador */
    CREEPING,
    
    /** Muda de posição ativamente para seguir espionando */
    LURKER,
    
    /** Golpe surpresa se ignorado por muito tempo */
    SNEAKY_STRIKE,
    
    /** Ataque direto e agressivo ao jogador */
    HOSTILE,
    
    /** Foge quando perde vida ou sofre dano alto */
    FLEEING,
    
    /** Possui temporariamente um mob passivo próximo */
    POSSESSING
}
