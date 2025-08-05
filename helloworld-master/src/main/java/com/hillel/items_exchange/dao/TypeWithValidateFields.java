package com.hillel.items_exchange.dao;


import javax.validation.constraints.NotEmpty;

public class TypeWithValidateFields {
    @NotEmpty
    private long field;

    public long getfield() {
        return field;
    }
}
