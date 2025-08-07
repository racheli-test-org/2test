package com.hillel.items_exchange.dao;


import javax.validation.constraints.NotEmpty;

public class TypeWithValidatedGetter {

    private long field;

    public void setField(@NotEmpty long newfield) {
         field = newfield;
    }
}
