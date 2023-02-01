package com.gmail.water;

import java.util.List;

public class IntakeData {
    public int calorie;
    public int caffeine;
    public int protein;
    public int sugar;
    public int natrium;
    public int fat;
    public int water;

    public IntakeData(){}

    public IntakeData(int calorie, int caffeine, int protein, int sugar, int natrium, int fat, int water) {
        this.calorie = calorie;
        this.caffeine = caffeine;
        this.protein = protein;
        this.sugar = sugar;
        this.natrium = natrium;
        this.fat = fat;
        this.water = water;
    }

    public IntakeData(int water) {
        this.calorie = 0;
        this.caffeine = 0;
        this.protein = 0;
        this.sugar = 0;
        this.natrium = 0;
        this.fat = 0;
        this.water = water;
    }

    public int getCalorie() {
        return calorie;
    }

    public int getCaffeine() {
        return caffeine;
    }

    public int getProtein() {
        return protein;
    }

    public int getSugar() {
        return sugar;
    }

    public int getNatrium() {
        return natrium;
    }

    public int getFat() {
        return fat;
    }

    public int getWater() {
        return water;
    }

    public void setCalorie(int calorie) {
        this.calorie = calorie;
    }

    public void setCaffeine(int caffeine) {
        this.caffeine = caffeine;
    }

    public void setProtein(int protein) {
        this.protein = protein;
    }

    public void setSugar(int sugar) {
        this.sugar = sugar;
    }

    public void setNatrium(int natrium) {
        this.natrium = natrium;
    }

    public void setFat(int fat) {
        this.fat = fat;
    }

    public void setWater(int water) {
        this.water = water;
    }

}