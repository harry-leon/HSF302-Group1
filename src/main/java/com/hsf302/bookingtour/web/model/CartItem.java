package com.hsf302.bookingtour.web.model;

import java.io.Serializable;

public record CartItem(String tourId, String tourName, int unitPrice, int travelers) implements Serializable {

    public double subtotal() {
        return unitPrice * (double) travelers;
    }
}
