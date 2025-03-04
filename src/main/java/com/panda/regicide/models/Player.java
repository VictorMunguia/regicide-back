package com.panda.regicide.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Player {
    private String id;               // El ID del socket/session
    private List<Card> hand; // Cartas en mano

    public Player() {
        this.hand = new ArrayList<>();
    }

    public Player(String id) {
        this.id = id;
        this.hand = new ArrayList<>();
    }

    // getters, setters...
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public List<Card> getHand() {
        return hand;
    }
    public void setHand(List<Card> hand) {
        this.hand = hand;
    }
}
