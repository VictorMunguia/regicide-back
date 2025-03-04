package com.panda.regicide.models;

import lombok.Data;

import java.util.*;

@Data
public class GameBoard {
    private Deque<Card> deck = new ArrayDeque<>(); // Pila principal
    private List<Card> grave = new ArrayList<>();  // Cartas descartadas
    private List<Card> table = new ArrayList<>();  // Cartas jugadas en la mesa
    private Deque<Card> bosses = new ArrayDeque<>(); // Lista de jefes
    private Boss currentBoss = new Boss();

    private String playerTurn = "";
    private String playerPhase = "";
    private boolean endGame = false;
    private boolean winGame = false;

    /**
     * Método auxiliar para sacar (pop) N cartas del grave (o las que queden).
     */
    public List<Card> popFromGrave(int count) {
        if (count > grave.size()) {
            count = grave.size();
        }
        List<Card> sub = grave.subList(0, count);
        List<Card> result = new ArrayList<>(sub);
        sub.clear(); // las removemos del grave
        return result;
    }

    /**
     * Añade esas cartas al deck y baraja de nuevo.
     */
    public void addToDeck(List<Card> cards) {
        List<Card> combined = new ArrayList<>(deck);
        combined.addAll(cards);
        Collections.shuffle(combined);
        deck = new ArrayDeque<>(combined);
    }
}
