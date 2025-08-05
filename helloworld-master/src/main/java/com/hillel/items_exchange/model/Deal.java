package com.hillel.items_exchange.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = {"users"})
public class Deal extends BaseEntity {

    @ManyToMany(mappedBy = "deals")
    private List<User> users;
}
