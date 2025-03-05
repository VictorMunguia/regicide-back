package com.panda.regicide.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Room {
    private String roomName;
    private List<Player> players = new ArrayList<>();
    private int maxPlayerNumber;
    private boolean gameStarted = false;
    private GameBoard gameBoard = new GameBoard();
    private int turnIndex = 0;
    private String roomOwner;
    private String lives;
    private String handCards;
    private boolean randomBosses;

    public Room(String roomName) {
        this.roomName = roomName;
    }

    // getters y setters...

    public void addPlayer(Player p) {
        players.add(p);
    }

    public void removePlayerById(String id) {
        players.removeIf(p -> p.getId().equals(id));
    }
}
