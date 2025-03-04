package com.panda.regicide.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Card {

    public Card(String value, String suit){
        this.value = value;
        this.suit = suit;
    }

    private String value;
    private String suit;
}
