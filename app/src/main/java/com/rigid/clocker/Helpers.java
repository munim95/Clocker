package com.rigid.clocker;

import android.graphics.Path;

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
    //create Polygon path
    public static Path createPath(Path path, int sides, float radius, float cx, float cy){
        float angle = (float) (2.0 * Math.PI / sides); //since all angles are equal in a polygon
        path.moveTo(
                (float)(cx + (radius * Math.cos(0.0))),
                (float)(cy + (radius * Math.sin(0.0))));
        for (int i=1; i<=sides;i++) {
            path.lineTo(
                    cx + (float) (radius * Math.cos(angle * i)),
                    cy + (float) (radius * Math.sin(angle * i)));
        }
        path.close();

        return path;
    }
}
