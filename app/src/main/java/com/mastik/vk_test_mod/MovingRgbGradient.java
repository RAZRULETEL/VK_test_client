package com.mastik.vk_test_mod;

import android.animation.TimeAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;

import java.util.HashMap;


public class MovingRgbGradient extends TimeAnimator {
    private static int speed = 1, frequency = 5, scale = 2, darkening = 100;
    private static int[][] colors;
    private static final TimeAnimator def = new TimeAnimator();
    private static HashMap<String,Integer> properties = new HashMap<>();
    public static boolean settingsChanged = false;

    public static void animate(LinearLayout back){
        def.start();
        if(colors != null)
            for(int i = 1; i < colors.length; i++)
                colors[i] = generateContinue(colors[i-1], scale);
        def.setTimeListener(new TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator timeAnimator, long total, long change) {
                if(colors == null || settingsChanged) {
                    colors = new int[back.getChildCount()][];
                    settingsChanged = false;
                }
                if(colors.length < back.getChildCount()){
                    int[][] buff = colors;
                    colors = new int[back.getChildCount()][];
                    System.arraycopy(buff, 0, colors, 0, buff.length);
                }
                for(int l = 0; l < back.getChildCount(); l++){
                    GradientDrawable rgb_grad = (GradientDrawable) back.getChildAt(l).getBackground();
                    if(colors[l] == null)
                        if(l > 0)
                            colors[l] = generateColors(colors[l-1], frequency, scale);
                        else
                            colors[l] = generateColors(null, frequency, scale);
                    for(int i = 0; i < colors[l].length; i++){
                        int a = (colors[l][i] >> 24) & 0xff;
                        int r = (colors[l][i] >> 16) & 0xff;
                        int g = (colors[l][i] >>  8) & 0xff;
                        int b = (colors[l][i]      ) & 0xff;
                        if(r >= 254-speed - darkening){
                            if(b > 1+speed)
                                b-=speed;
                            else
                            if(g <= 255-speed - darkening)
                                g+=speed;
                        }
                        if(g >= 255-speed - darkening){
                            if(r > 1+speed)
                                r-=speed;
                            else
                            if(b <= 255-speed - darkening)
                                b+=speed;
                        }
                        if(b >= 255-speed - darkening){
                            if(g > 1+speed)
                                g-=speed;
                            else
                            if(r <= 255-speed - darkening)
                                r+=speed;
                        }
                        colors[l][i] = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
                    }
                    rgb_grad.setColors(colors[l]);
                }
            }
        });
    }
    public static void stop(){
        def.end();
    }
    public static int[] generateColors (int length){
        float k = 255*3f/length;
        return genCols(null, length, k);
    }
    public static int[] generateColors (int length, float scale){
        float k = 255*3f/length/scale;
        return genCols(null, length, k);
    }
    public static int[] generateColors (int[] colors, int length){
        float k = 255*3f/length;
        return genCols(colors, length, k);
    }
    public static int[] generateColors (int[] colors, int length, float scale){
        float k = 255*3f/length/scale;
        return genCols(colors, length, k);
    }
    public static int[] generateContinue(int[] colors){
        float k = 255*3f/colors.length;
        return genCont(colors, k);
    }
    public static int[] generateContinue(int[] colors, float scale){
        float k = 255*3f/colors.length/scale;
        return genCont(colors, k);
    }
    private static int[] genCont(int[] colors, float k){
        int lcl = 255 - darkening;
        int[] newColors = new int[colors.length];
        int a = (colors[colors.length-1] >> 24) & 0xff; // or color >>> 24
        int r = (colors[colors.length-1] >> 16) & 0xff;
        int g = (colors[colors.length-1] >>  8) & 0xff;
        int b = (colors[colors.length-1]      ) & 0xff;
        //System.out.println(k);
        for(int i = 0; i < colors.length; i++){
            newColors[i] = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
            //System.out.println(i+"."+r+" "+g+" "+b);
            boolean iter = false;
            if(r >= lcl - 1){
                iter = true;
                if(b > 1){
                    b -= k;
                    if(b < 0){
                        g -= b;
                        b = 0;
                    }
                }else
                if(g <= lcl){
                    g += k;
                    if(g > lcl){
                        b += g % lcl;
                        g -= g % lcl;
                    }
                }
            }
            //System.out.println(i+"."+r+" "+g+" "+b);
            if(g >= lcl){
                if(r > 1){
                    r -= k;
                    if(r < 0){
                        b -= r;
                        r = 0;
                    }
                }else
                if(b <= lcl){
                    b += k;
                    if(b > lcl){
                        r += b % lcl;
                        b -= b % lcl;
                    }
                }
            }
            //System.out.println(i+"."+r+" "+g+" "+b);
            if(b >= lcl){
                if(g > 1){
                    g -= k;
                    if(g < 0){
                        r -= g;
                        g = 0;
                    }
                }else
                if(r <= lcl){
                    r += k;
                    if(r > lcl){
                        g += r % lcl;
                        r -= r % lcl;
                    }
                }
            }
            //System.out.println(i+"."+r+" "+g+" "+b);
            if(r >= (lcl - 1)  && !iter){
                if(b > 1){
                    b -= k;
                    if(b < 0){
                        g -= b;
                        b = 0;
                    }
                }else
                if(g <= lcl){
                    g += k;
                    if(g > lcl){
                        b += g % lcl;
                        g -= g % lcl;
                    }
                }
            }
            //System.out.println(i+"."+r+" "+g+" "+b);
        }
        return newColors;
    }
    private static int[] genCols(int[] oldColors, int length, float k){
        int[] colors = new int[length];
        int a = 255; // or color >>> 24
        int r = 255-darkening;
        int g = 0;
        int b = 0;
        if(oldColors != null) {
            a = (oldColors[oldColors.length - 1] >> 24) & 0xff; // or color >>> 24
            r = (oldColors[oldColors.length - 1] >> 16) & 0xff;
            g = (oldColors[oldColors.length - 1] >> 8) & 0xff;
            b = (oldColors[oldColors.length - 1]) & 0xff;
        }
        //System.out.println(k);
        for(int i = 0; i < length; i++){
            colors[i] = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
            boolean iter = false;
            if(r >= 254 - darkening){
                iter = true;
                if(b > 1){
                    b -= k;
                    if(b < 0){
                        g -= b;
                        b = 0;
                    }
                }else
                if(g <= 255 - darkening){
                    g += k;
                    if(g > 255 - darkening){
                        b += g % (255 - darkening);
                        g -= g % (255 - darkening);
                    }
                }
            }
            if(g >= 255 - darkening){
                if(r > 1){
                    r -= k;
                    if(r < 0){
                        b -= r;
                        r = 0;
                    }
                }else
                if(b <= 255 - darkening){
                    b += k;
                    if(b > 255 - darkening){
                        r += b % (255 - darkening);
                        b -= b % (255 - darkening);
                    }
                }
            }
            if(b >= 255 - darkening){
                if(g > 1){
                    g -= k;
                    if(g < 0){
                        r -= g;
                        g = 0;
                    }
                }else
                if(r <= 255 - darkening){
                    r += k;
                    if(r > 255 - darkening){
                        g += r % (255 - darkening);
                        r -= r % (255 - darkening);
                    }
                }
            }
            if(r >= (254 - darkening) && !iter){
                if(b > 1){
                    b -= k;
                    if(b < 0){
                        g -= b;
                        b = 0;
                    }
                }else
                if(g <= 255 - darkening){
                    g += k;
                    if(g > 255 - darkening){
                        b += g % (255 - darkening);
                        g -= g % (255 - darkening);
                    }
                }
            }
/*            int indx = 0;
            int max = Math.max(Math.max(r,g), b);
            if(max == g)
                indx = 1;
            else
                if(max == b)
                    indx = 2;

            int[] test = offsetRGB(new int[]{r, g, b}, indx, (int)k);
            r = test[0];
            g = test[1];
            b = test[2];*/
        }
        return colors;
    }
    public static void init(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences("gradient_settings", Context.MODE_PRIVATE);
        if(darkening != prefs.getInt("nightness", darkening) || frequency != prefs.getInt("length", frequency)
                || scale != prefs.getInt("scale", scale) || speed != prefs.getInt("speed", speed))
            settingsChanged = true;
        frequency = prefs.getInt("length", frequency);
        scale = prefs.getInt("scale", scale);
        speed = prefs.getInt("speed", speed);
        darkening = prefs.getInt("nightness", darkening);
        if(!prefs.contains("length"))
            prefs.edit()
                    .putInt("length", frequency)
                    .putInt("scale", scale)
                    .putInt("speed", speed)
                    .putInt("nightness", darkening)
                    .apply();


//        frequency = Integer.parseInt(readSourceData("grad_length").equals("") ? MovingRgbGradient.frequency+"" : readSourceData("grad_length"));
//        scale = Integer.parseInt(readSourceData("grad_scale").equals("") ? MovingRgbGradient.scale+"" : readSourceData("grad_scale"));
//        speed = Integer.parseInt(readSourceData("grad_speed").equals("") ? MovingRgbGradient.speed+"" : readSourceData("grad_speed"));
//        darkening = Integer.parseInt(readSourceData("nightness").equals("") ? MovingRgbGradient.darkening+"" : readSourceData("nightness"));
    }

    private static int[] offsetRGB(int[] rgb_arr, int indx, int size){
//        rgb_arr[indx] += size;
        int next_indx = indx >= rgb_arr.length-1 ? 0 : indx+1;
        int before_indx = indx <= 0 ? rgb_arr.length-1 : indx-1;
//        rgb_arr[next_indx] += rgb_arr[indx] % 255;
//        rgb_arr[indx] = Math.min(rgb_arr[indx], 255);
//        rgb_arr[before_indx] -= size;
//        rgb_arr[indx] += Math.min(rgb_arr[before_indx], 0);
//        rgb_arr[before_indx] = Math.max(rgb_arr[before_indx], 0);
        if(rgb_arr[next_indx] > 0) {
            rgb_arr[next_indx] += size;
            if(rgb_arr[next_indx] >= 255) {
                rgb_arr[indx] -= rgb_arr[next_indx] % 255;
                rgb_arr[next_indx] = 255;
            }
        }else{
            rgb_arr[before_indx] -= size;
            if(rgb_arr[before_indx] < 0) {
                rgb_arr[next_indx] += rgb_arr[before_indx] % 255;
                rgb_arr[before_indx] = 0;
            }
        }

        return rgb_arr;
    }

    public static int getSpeed() {
        return speed;
    }

    public static int getFrequency() {
        return frequency;
    }

    public static int getScale() {
        return scale;
    }

    public static int getDarkening() {
        return darkening;
    }
}
