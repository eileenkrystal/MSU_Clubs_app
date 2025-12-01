package com.example.cse476;

public class Profile {
    public String id;      // uuid, same as USER_ID in prefs
    public String email;   // read-only from auth/users
    public String name;
    public String major;
    public String year;

    public Profile() {
        // required for Retrofit/Gson
    }

    public Profile(String id, String email, String name, String major, String year) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.major = major;
        this.year = year;
    }
}
