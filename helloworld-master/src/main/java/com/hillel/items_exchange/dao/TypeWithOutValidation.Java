package com.hillel.items_exchange.dao;

import javax.validation.constraints.NotNull;

public class TypeWithOutValidation {
    public long field1;
    @NotNull
    public long field2;
}
