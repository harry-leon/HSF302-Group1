package com.hsf302.bookingtour.web.util;

import com.hsf302.bookingtour.web.model.CartItem;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;


public final class CartSession {

    public static final String CART_KEY = "cart";

    private CartSession() {
    }

    @SuppressWarnings("unchecked")
    public static List<CartItem> get(HttpSession session) {
        Object attribute = session.getAttribute(CART_KEY);
        if (attribute instanceof List<?>) {
            return (List<CartItem>) attribute;
        }
        List<CartItem> cart = new ArrayList<>();
        session.setAttribute(CART_KEY, cart);
        return cart;
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(CART_KEY);
    }


    public static int parseTravelersOrInvalid(String travelers) {
        try {
            int parsed = Integer.parseInt(travelers.trim());
            return Math.max(1, Math.min(parsed, 9));
        } catch (NumberFormatException | NullPointerException ex) {
            return -1;
        }
    }
}
