package com.example.cse476;

import java.io.Serializable;

public class Club implements Serializable {
    public String id;
    public String slug;
    public String name;
    public String description;
    public String website;
    public String address;
    public String email;
    public String phone;

    @Override
    public String toString() {
        return name != null ? name : (slug != null ? slug : "Club");
    }
}
