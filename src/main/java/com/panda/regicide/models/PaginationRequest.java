package com.panda.regicide.models;

import lombok.Data;

@Data
public class PaginationRequest {
    private String page;
    private String size;
}