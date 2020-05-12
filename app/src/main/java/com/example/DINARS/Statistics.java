package com.example.DINARS;

public class Statistics {

    private double[] globalDistraction = {0,0,0,0,0};
    private double[] phoneDistraction = {0,0,0,0,0};
    private double[] coffeeDistraction  = {0,0,0,0,0};
    private double[] drowsinessLevel = {0,0,0,0,0};

    public void newStatistic(double gd, double pd, double cd, double dl, int index){
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

    public double getPhoneDistraction(int index){
        return phoneDistraction[index];
    }

    public double getCoffeeDistraction(int index){
        return coffeeDistraction[index];
    }

    public double getDrowsinessLevel(int index){
        return drowsinessLevel[index];
    }

}
