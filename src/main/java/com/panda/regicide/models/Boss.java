package com.panda.regicide.models;

public class Boss {
    private String value; // J, Q, K
    private String suit;  // ♥, ♦, ♣, ♠
    private int health;
    private int damage;
    private String effects = "";
    private boolean effectBloqued = false;

    // getters, setters...
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSuit() { return suit; }
    public void setSuit(String suit) { this.suit = suit; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public String getEffects() { return effects; }
    public void setEffects(String effects) { this.effects = effects; }

    public boolean isEffectBloqued() { return effectBloqued; }
    public void setEffectBloqued(boolean effectBloqued) { this.effectBloqued = effectBloqued; }
}
