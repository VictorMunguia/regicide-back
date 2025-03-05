package com.panda.regicide.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginationResponse {
    private List<RoomDTO> rooms;
    private String totalRooms;
    private String totalPages;
    private String currentPage;
}