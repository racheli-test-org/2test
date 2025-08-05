package com.hillel.items_exchange.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"advertisements", "phones", "deals", "children"})
public class User extends BaseEntity {

    @Column(unique = true)
    private String username;
    private String password;
    @Column(unique = true)
    private String email;
    private Boolean online;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_image")
    private String avatarImage;

    @Column(name = "last_online_time")
    private LocalDate lastOnlineTime;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Advertisement> advertisements;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "user_deal",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "deal_id", referencedColumnName = "id")})
    private List<Deal> deals;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Phone> phones;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Child> children;
}
