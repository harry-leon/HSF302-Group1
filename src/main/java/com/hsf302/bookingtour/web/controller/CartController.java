package com.hsf302.bookingtour.web.controller;

import com.hsf302.bookingtour.web.model.CartItem;
import com.hsf302.bookingtour.web.model.Tour;
import com.hsf302.bookingtour.web.util.CartSession;
import com.hsf302.bookingtour.web.util.PricingRules;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/tour/cart")
public class CartController {

    @PostMapping("/add")
    public String addToCart(@RequestParam String tourId,
                            @RequestParam String travelers,
                            HttpSession session,
                            RedirectAttributes flash) {
        Tour tour = Tour.findOrNull(tourId);
        if (tour == null) {
            flash.addFlashAttribute("bookingError", "Selected tour could not be found. Please choose a tour again.");
            return "redirect:/tour";
        }

        int safeTravelers = CartSession.parseTravelersOrInvalid(travelers);
        if (safeTravelers < 0) {
            flash.addFlashAttribute("bookingError", "Number of travelers must be a valid number.");
            return "redirect:/tour/book/" + tour.id();
        }

        List<CartItem> cart = CartSession.get(session);

        cart.removeIf(item -> item.tourId().equalsIgnoreCase(tour.id()));
        cart.add(new CartItem(tour.id(), tour.name(), tour.price(), safeTravelers));

        flash.addFlashAttribute("cartMessage", "Added " + tour.name() + " to your cart.");
        return "redirect:/tour/cart";
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItem> cart = CartSession.get(session);
        double subtotal = cart.stream().mapToDouble(CartItem::subtotal).sum();
        int totalTravelers = cart.stream().mapToInt(CartItem::travelers).sum();
        double tax = PricingRules.tax(subtotal);
        double discount = PricingRules.groupDiscount(subtotal, totalTravelers);

        model.addAttribute("cartItems", cart);
        model.addAttribute("cartSubtotal", subtotal);
        model.addAttribute("cartTax", tax);
        model.addAttribute("cartDiscount", discount);
        model.addAttribute("cartTotal", subtotal - discount + tax);
        return "cart";
    }


    @GetMapping("/edit/{tourId}")
    public String editCartItem(@PathVariable String tourId, HttpSession session, Model model) {
        CartItem item = CartSession.get(session).stream()
                .filter(i -> i.tourId().equalsIgnoreCase(tourId))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return "redirect:/tour/cart";
        }
        model.addAttribute("item", item);
        return "cart-edit";
    }

    @GetMapping("/remove/{tourId}")
    public String removeFromCart(@PathVariable String tourId, HttpSession session) {
        CartSession.get(session).removeIf(item -> item.tourId().equalsIgnoreCase(tourId));
        return "redirect:/tour/cart";
    }
}
