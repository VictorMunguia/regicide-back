package com.panda.regicide.models;

import lombok.Data;

import java.util.List;

@Data
public class PlayTurnData {
    private String roomName;
    private String playerId;
    private String action;
    private List<Card> cards;
}
