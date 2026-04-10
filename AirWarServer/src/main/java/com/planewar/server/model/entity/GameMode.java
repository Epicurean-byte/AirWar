package com.planewar.server.model.entity;

/**
 * Game mode enumeration for multiplayer battles.
 */
public enum GameMode {
    /**
     * PVP mode: Two players face each other, restricted to their own half of the screen.
     * Players compete for higher score.
     */
    PVP,
    
    /**
     * Cooperative mode: Two players fight together against enemies.
     * No friendly fire, shared victory condition.
     */
    COOP
}
