package com.mastik.vk_test_mod;

import android.animation.TimeAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;

import com.mastik.vk_test_mod.settings.AppSettingsTab;
import com.mastik.vk_test_mod.settings.tabs.GradientSetting;

import java.util.HashMap;


public class MovingRgbGradient extends TimeAnimator {
    public static final int DEFAULT_SPEED = 1, DEFAULT_FREQUENCY = 5, DEFAULT_SCALE = 2, DEFAULT_DARKENING = 100, MIN_BRIGHTNESS = 1;
    private static int speed = DEFAULT_SPEED, frequency = DEFAULT_FREQUENCY, scale = DEFAULT_SCALE, darkening = DEFAULT_DARKENING;
    private static int[][] colors;
    private static final TimeAnimator timer = new TimeAnimator();
    private static final HashMap<String, Integer> properties = new HashMap<>();
    public static boolean settingsChanged = false;

    public static void animate(LinearLayout back) {
        timer.start();
        if (colors != null)
            for (int i = 1; i < colors.length; i++)
                colors[i] = generateNextColors(colors[i - 1], scale);
        timer.setTimeListener(new TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator timeAnimator, long total, long change) {
                if (colors == null || settingsChanged) {
                    colors = new int[back.getChildCount()][];
                    settingsChanged = false;
                }
                if (colors.length < back.getChildCount()) {
                    int[][] buff = colors;
                    colors = new int[back.getChildCount()][];
                    System.arraycopy(buff, 0, colors, 0, buff.length);
                }
                for (int l = 0; l < back.getChildCount(); l++) {
                    GradientDrawable rgb_grad = (GradientDrawable) back.getChildAt(l).getBackground();
                    if (colors[l] == null)
                        if (l > 0)
                            colors[l] = generateColors(colors[l - 1], frequency, scale);
                        else
                            colors[l] = generateColors(null, frequency, scale);
                    for (int i = 0; i < colors[l].length; i++) {
                        int a = (colors[l][i] >> 24) & 0xff;
                        int r = (colors[l][i] >> 16) & 0xff;
                        int g = (colors[l][i] >> 8) & 0xff;
                        int b = (colors[l][i]) & 0xff;
                        if (r >= 254 - speed - darkening) {
                            if (b > MIN_BRIGHTNESS + speed)
                                b -= speed;
                            else if (g <= 255 - speed - darkening)
                                g += speed;
                        }
                        if (g >= 255 - speed - darkening) {
                            if (r > MIN_BRIGHTNESS + speed)
                                r -= speed;
                            else if (b <= 255 - speed - darkening)
                                b += speed;
                        }
                        if (b >= 255 - speed - darkening) {
                            if (g > MIN_BRIGHTNESS + speed)
                                g -= speed;
                            else if (r <= 255 - speed - darkening)
                                r += speed;
                        }
                        colors[l][i] = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
                    }
                    rgb_grad.setColors(colors[l]);
                }
            }
        });
    }

    public static void stop() {
        timer.end();
    }

    public static int[] generateColors(int length) {
        return generateColors(null, length, 1);
    }

    public static int[] generateColors(int length, float scale) {
        return generateColors(null, length, scale);
    }

    public static int[] generateColors(int[] colors, int length) {
        return generateColors(colors, length, 1);
    }

    public static int[] generateColors(int[] colors, int length, float scale) {
        float k = 255 * 3f / length / scale;
        int[] newColors = new int[length];
        int a = 255;
        int r = 255 - darkening;
        int g = 0;
        int b = 0;
        if (colors != null) {
            a = (colors[colors.length - 1] >> 24) & 0xff;
            r = (colors[colors.length - 1] >> 16) & 0xff;
            g = (colors[colors.length - 1] >> 8) & 0xff;
            b = (colors[colors.length - 1]) & 0xff;
        }
        for (int i = 0; i < length; i++) {
            newColors[i] = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
            boolean iter = false;
            if (r >= 254 - darkening) {
                iter = true;
                if (b > 1) {
                    b -= k;
                    if (b < 0) {
                        g -= b;
                        b = 0;
                    }
                } else if (g <= 255 - darkening) {
                    g += k;
                    if (g > 255 - darkening) {
                        b += g % (255 - darkening);
                        g -= g % (255 - darkening);
                    }
                }
            }
            if (g >= 255 - darkening) {
                if (r > 1) {
                    r -= k;
                    if (r < 0) {
                        b -= r;
                        r = 0;
                    }
                } else if (b <= 255 - darkening) {
                    b += k;
                    if (b > 255 - darkening) {
                        r += b % (255 - darkening);
                        b -= b % (255 - darkening);
                    }
                }
            }
            if (b >= 255 - darkening) {
                if (g > 1) {
                    g -= k;
                    if (g < 0) {
                        r -= g;
                        g = 0;
                    }
                } else if (r <= 255 - darkening) {
                    r += k;
                    if (r > 255 - darkening) {
                        g += r % (255 - darkening);
                        r -= r % (255 - darkening);
                    }
                }
            }
            if (r >= (254 - darkening) && !iter) {
                if (b > 1) {
                    b -= k;
                    if (b < 0) {
                        g -= b;
                        b = 0;
                    }
                } else if (g <= 255 - darkening) {
                    g += k;
                    if (g > 255 - darkening) {
                        b += g % (255 - darkening);
                        g -= g % (255 - darkening);
                    }
                }
            }
        }
        return newColors;
    }

    public static int[] generateNextColors(int[] colors) {
        return generateNextColors(colors, 1);
    }

    public static int[] generateNextColors(int[] colors, float scale) {
        float k = 255 * 3f / colors.length / scale;

        int lcl = 255 - darkening;
        int[] newColors = new int[colors.length];
        int a = (colors[colors.length - 1] >> 24) & 0xff; // or color >>> 24
        int r = (colors[colors.length - 1] >> 16) & 0xff;
        int g = (colors[colors.length - 1] >> 8) & 0xff;
        int b = (colors[colors.length - 1]) & 0xff;
        //System.out.println(k);
        for (int i = 0; i < colors.length; i++) {
            newColors[i] = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
            boolean iter = false;
            if (r >= lcl - 1) {
                iter = true;
                if (b > 1) {
                    b -= k;
                    if (b < 0) {
                        g -= b;
                        b = 0;
                    }
                } else if (g <= lcl) {
                    g += k;
                    if (g > lcl) {
                        b += g % lcl;
                        g -= g % lcl;
                    }
                }
            }
            if (g >= lcl) {
                if (r > 1) {
                    r -= k;
                    if (r < 0) {
                        b -= r;
                        r = 0;
                    }
                } else if (b <= lcl) {
                    b += k;
                    if (b > lcl) {
                        r += b % lcl;
                        b -= b % lcl;
                    }
                }
            }
            if (b >= lcl) {
                if (g > 1) {
                    g -= k;
                    if (g < 0) {
                        r -= g;
                        g = 0;
                    }
                } else if (r <= lcl) {
                    r += k;
                    if (r > lcl) {
                        g += r % lcl;
                        r -= r % lcl;
                    }
                }
            }
            if (r >= (lcl - 1) && !iter) {
                if (b > 1) {
                    b -= k;
                    if (b < 0) {
                        g -= b;
                        b = 0;
                    }
                } else if (g <= lcl) {
                    g += k;
                    if (g > lcl) {
                        b += g % lcl;
                        g -= g % lcl;
                    }
                }
            }
        }
        return newColors;
    }

    public static void init(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(AppSettingsTab.GradientSettings.sharedPreferencesName, Context.MODE_PRIVATE);
        if (darkening != prefs.getInt(GradientSetting.Darkening.nameInPreferences, darkening) || frequency != prefs.getInt(GradientSetting.Frequency.nameInPreferences, frequency)
                || scale != prefs.getInt(GradientSetting.Scale.nameInPreferences, scale) || speed != prefs.getInt(GradientSetting.Speed.nameInPreferences, speed))
            settingsChanged = true;
        frequency = prefs.getInt(GradientSetting.Frequency.nameInPreferences, frequency);
        scale = prefs.getInt(GradientSetting.Scale.nameInPreferences, scale);
        speed = prefs.getInt(GradientSetting.Speed.nameInPreferences, speed);
        darkening = prefs.getInt(GradientSetting.Darkening.nameInPreferences, darkening);
        if (!prefs.contains("length"))
            prefs.edit()
                    .putInt(GradientSetting.Frequency.nameInPreferences, frequency)
                    .putInt(GradientSetting.Scale.nameInPreferences, scale)
                    .putInt(GradientSetting.Speed.nameInPreferences, speed)
                    .putInt(GradientSetting.Darkening.nameInPreferences, darkening)
                    .apply();


//        frequency = Integer.parseInt(readSourceData("grad_length").equals("") ? MovingRgbGradient.frequency+"" : readSourceData("grad_length"));
//        scale = Integer.parseInt(readSourceData("grad_scale").equals("") ? MovingRgbGradient.scale+"" : readSourceData("grad_scale"));
//        speed = Integer.parseInt(readSourceData("grad_speed").equals("") ? MovingRgbGradient.speed+"" : readSourceData("grad_speed"));
//        darkening = Integer.parseInt(readSourceData("nightness").equals("") ? MovingRgbGradient.darkening+"" : readSourceData("nightness"));
    }

    private static int[] offsetRGB(int[] rgbArr, int indx, int size) {
//        rgb_arr[indx] += size;
        int next_indx = indx >= rgbArr.length - 1 ? 0 : indx + 1;
        int before_indx = indx <= 0 ? rgbArr.length - 1 : indx - 1;
//        rgb_arr[next_indx] += rgb_arr[indx] % 255;
//        rgb_arr[indx] = Math.min(rgb_arr[indx], 255);
//        rgb_arr[before_indx] -= size;
//        rgb_arr[indx] += Math.min(rgb_arr[before_indx], 0);
//        rgb_arr[before_indx] = Math.max(rgb_arr[before_indx], 0);
        if (rgbArr[next_indx] > 0) {
            rgbArr[next_indx] += size;
            if (rgbArr[next_indx] >= 255) {
                rgbArr[indx] -= rgbArr[next_indx] % 255;
                rgbArr[next_indx] = 255;
            }
        } else {
            rgbArr[before_indx] -= size;
            if (rgbArr[before_indx] < 0) {
                rgbArr[next_indx] += rgbArr[before_indx] % 255;
                rgbArr[before_indx] = 0;
            }
        }

        return rgbArr;
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
