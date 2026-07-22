package com.euandrelucas.herobrine.ai;

import org.bukkit.entity.Player;

/**
 * Gerencia as transições e níveis de raiva/ameaça da IA do Herobrine.
 */
public class HerobrineStateMachine {

    private HerobrineState currentState;
    private int angerLevel;
    private final Player targetPlayer;
    private long lastStateChangeTime;

    public HerobrineStateMachine(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
        this.currentState = HerobrineState.WATCHING;
        this.angerLevel = 0;
        this.lastStateChangeTime = System.currentTimeMillis();
    }

    /**
     * Atualiza o estado da IA com base na raiva e na interação do jogador.
     */
    public void update() {
        long elapsedSeconds = (System.currentTimeMillis() - lastStateChangeTime) / 1000;

        // Decaimento natural de raiva se inativo
        if (angerLevel > 0 && elapsedSeconds % 10 == 0) {
            angerLevel = Math.max(0, angerLevel - 1);
        }

        // Avalia transição de estado baseada em raiva
        if (angerLevel >= 80 && currentState != HerobrineState.HOSTILE) {
            setState(HerobrineState.HOSTILE);
        } else if (angerLevel >= 50 && currentState == HerobrineState.WATCHING) {
            setState(HerobrineState.CREEPING);
        }
    }

    public void addAnger(int amount) {
        this.angerLevel = Math.min(100, Math.max(0, this.angerLevel + amount));
        update();
    }

    public void setState(HerobrineState newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            this.lastStateChangeTime = System.currentTimeMillis();
        }
    }

    public HerobrineState getCurrentState() {
        return currentState;
    }

    public int getAngerLevel() {
        return angerLevel;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public long getLastStateChangeTime() {
        return lastStateChangeTime;
    }
}
