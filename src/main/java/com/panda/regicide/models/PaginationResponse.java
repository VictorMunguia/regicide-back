package com.panda.regicide.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginationResponse {
    private List<String> rooms;
    private String totalRooms;
    private String totalPages;
    private String currentPage;
}