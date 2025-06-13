package com.panda.regicide.models;
import lombok.Data;

@Data
public class CreateRoomRequest extends PaginationRequest {
    private String roomName;
}
