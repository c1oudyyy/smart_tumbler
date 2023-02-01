package com.gmail.water;

public class FriendItem {
    String name;
    int water;

    public FriendItem(int water, String name) {
        this.water = water;
        this.name = name;
    }

    public int getWater() {
        return water;
    }

    public String getName() {
        return name;
    }

    public void setWater(int water) {
        this.water = water;
    }

    public void setName(String name) {
        this.name = name;
    }
}
