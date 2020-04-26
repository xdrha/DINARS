package com.example.clickme;

public class Statistics {

    private double[] globalDistraction = {0,0,0,0,0};
    private int[] phoneDistraction = {0,0,0,0,0};
    private int[] coffeeDistraction  = {0,0,0,0,0};
    private int[] drowsinessLevel = {0,0,0,0,0};

    public void newStatistic(double gd, int pd, int cd, int dl, int index){
        globalDistraction[index] = gd;
        phoneDistraction[index] = pd;
        coffeeDistraction[index] = cd;
        drowsinessLevel[index] = dl;
    }

    public void init(){
        for(int i = 0; i < 5; i++){
            globalDistraction[i] = 0;
            phoneDistraction[i] = 0;
            coffeeDistraction[i] = 0;
            drowsinessLevel[i] = 0;
        }
    }

    public double getGlobalDistraction(int index){
        return globalDistraction[index];
    }

    public int getPhoneDistraction(int index){
        return phoneDistraction[index];
    }

    public int getCoffeeDistraction(int index){
        return coffeeDistraction[index];
    }

    public int getDrowsinessLevel(int index){
        return drowsinessLevel[index];
    }

}
