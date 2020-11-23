package com.rigid.clocker;

public class Helpers {
    public static int[] timeConversion(long seconds) {
        int[] timeConversion = new int[3];
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        long hours = minutes / 60;
        minutes -= hours * 60;
        //return only min:seconds(eg. 04:05) if hours is 0
        timeConversion[0] = (int)hours;
        timeConversion[1] = (int)minutes;
        timeConversion[2] = (int)seconds;
        return timeConversion;
    }
}
