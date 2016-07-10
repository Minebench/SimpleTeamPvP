package de.themoep.simpleteampvp;

import org.bukkit.Color;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * SimpleTeamPvP
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class Utils {

    public static String formatTime(long time, TimeUnit unit) {
        String format = "HH:mm:ss";
        if(unit.toHours(time) < 1) {
            format = "mm:ss";
        }
        return formatTime(time, unit, format);
    }

    public static String formatTime(long time, TimeUnit unit, String format) {
        long millis = unit.toMillis(time);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(tz);

        return df.format(new Date(millis));
    }

    /**
     * Converts bungee's chat colors to true colors
     * @param color The chat color
     * @return A "true" color; white if unknown; never null
     */
    public static Color convertColor(net.md_5.bungee.api.ChatColor color) {
        switch(color) {
            case MAGIC:
                Random r = new Random();
                return Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));
            case BLACK:
                return Color.fromRGB(0, 0, 0);
            case DARK_BLUE:
                return Color.fromRGB(0, 0, 170);
            case DARK_GREEN:
                return Color.fromRGB(0, 170, 0);
            case DARK_AQUA:
                return Color.fromRGB(0, 170, 170);
            case DARK_RED:
                return Color.fromRGB(170, 0, 0);
            case DARK_PURPLE:
                return Color.fromRGB(170, 0, 170);
            case GOLD:
                return Color.fromRGB(255, 170, 0);
            case GRAY:
                return Color.fromRGB(170, 170, 170);
            case DARK_GRAY:
                return Color.fromRGB(85, 85, 85);
            case BLUE:
                return Color.fromRGB(85, 85, 255);
            case GREEN:
                return Color.fromRGB(85, 255, 85);
            case AQUA:
                return Color.fromRGB(85, 255, 255);
            case RED:
                return Color.fromRGB(255, 85, 85);
            case LIGHT_PURPLE:
                return Color.fromRGB(255, 85, 255);
            case YELLOW:
                return Color.fromRGB(255, 255, 85);
            case WHITE:
            default:
                return Color.fromRGB(255, 255, 255);
        }
    }

    public static Color convertColor(org.bukkit.ChatColor color) {
        return convertColor(color.asBungee());
    }

    /**
     * Join an array of strings with a certain delimiter and a specific range
     * @param args The array to join
     * @param delimiter The delimiter string
     * @param startIndex The start index, if negative from the end
     * @param endIndex The end index, if negative from the end
     * @return The joined array including the string at the startIndex and excluding the one at the endIndex
     */
    public static String join(String[] args, String delimiter, int startIndex, int endIndex) {
        if (startIndex < 0) {
            startIndex = args.length + startIndex;
        }
        if (endIndex < 0) {
            endIndex = args.length + endIndex;
        }
        StringBuilder sb = new StringBuilder(args[startIndex]);
        for (int i = startIndex + 1; i < endIndex; i++) {
            sb.append(delimiter).append(args[i]);
        }
        return sb.toString();
    }
}
