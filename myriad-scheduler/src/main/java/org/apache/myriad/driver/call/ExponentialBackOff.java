package org.apache.myriad.driver.call;

import java.util.Random;

public class ExponentialBackOff {

    private int maxSteps;
    private int step;
    private int maxIterations;
    private int iteration;

    public ExponentialBackOff(int maxSteps, int maxIterations){
        this.maxSteps = maxSteps;
        step = 1;
        this.maxIterations = maxIterations;
        iteration = 1;
    }

    public Boolean lockUntilNextStep(){
        if (iteration == maxIterations){
            return false;
        }
        Random r = new Random();
        int sleepLow = (int) Math.round(Math.pow(2, step)) * 1000;
        int sleepHigh = (int) Math.round(Math.pow(2, step + 1)) * 1000;
        try {
            Thread.sleep(sleepHigh - r.nextInt(sleepHigh - sleepLow));
            step++;
            if (step == maxSteps){
                step = 1;
                if (! (maxIterations == -1)){
                    iteration++;
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void reStart(){
        step = 1;
        iteration = 1;
    }
}
