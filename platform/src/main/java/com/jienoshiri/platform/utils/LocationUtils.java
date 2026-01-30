package com.jienoshiri.platform.utils;

import java.math.BigDecimal;

public class LocationUtils {
    private static final double EARTH_RADIUS = 6371; // 地球半径(km)

    /**
     * 计算两点间距离 (单位: km)
     */
    public static double getDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return -1;

        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1.doubleValue()) - Math.toRadians(lng2.doubleValue());
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        // 保留2位小数
        return Math.round(s * 100.0) / 100.0;
    }
}