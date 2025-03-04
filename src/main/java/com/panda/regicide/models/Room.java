package com.panda.regicide.models;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomName;
    private List<Player> players = new ArrayList<>();
    private boolean gameStarted = false;
    private GameBoard gameBoard = new GameBoard();
    private int turnIndex = 0;

    public Room() {}
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

    // ...
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }

    public GameBoard getGameBoard() { return gameBoard; }
    public void setGameBoard(GameBoard gameBoard) { this.gameBoard = gameBoard; }

    public int getTurnIndex() { return turnIndex; }
    public void setTurnIndex(int turnIndex) { this.turnIndex = turnIndex; }
}
