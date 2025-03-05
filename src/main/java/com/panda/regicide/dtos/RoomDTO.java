package com.panda.regicide.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomDTO {
    private String roomName;
    private String playerNumber;
    private String maxPlayerNumber;
}
