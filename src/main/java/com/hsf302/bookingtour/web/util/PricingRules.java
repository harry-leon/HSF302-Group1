package com.hsf302.bookingtour.web.util;


public final class PricingRules {

    public static final double TAX_RATE = 0.04;
    public static final double DISCOUNT_RATE = 0.07;
    public static final int MIN_TRAVELERS_FOR_GROUP_DISCOUNT = 4;

    private PricingRules() {
    }

    public static double tax(double subtotal) {
        return subtotal * TAX_RATE;
    }

    public static double groupDiscount(double subtotal, int totalTravelers) {
        return totalTravelers >= MIN_TRAVELERS_FOR_GROUP_DISCOUNT ? subtotal * DISCOUNT_RATE : 0.0;
    }
}
