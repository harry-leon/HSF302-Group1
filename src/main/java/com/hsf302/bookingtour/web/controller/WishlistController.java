package com.hsf302.bookingtour.web.controller;

import com.hsf302.bookingtour.web.model.Tour;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;


@Controller
@RequestMapping("/tour/wishlist")
public class WishlistController {

    private static final String WISHLIST_KEY = "wishlist";

    @GetMapping
    public String wishlist(HttpSession session, Model model) {
        List<String> ids = getWishlist(session);
        List<Tour> wishlistTours = Tour.all().stream()
                .filter(t -> ids.contains(t.id()))
                .toList();
        model.addAttribute("wishlistTours", wishlistTours);
        return "wishlist";
    }

    @PostMapping("/add")
    public String wishlistAdd(@RequestParam String tourId,
                              HttpSession session,
                              RedirectAttributes flash) {
        Tour tour = Tour.findOrNull(tourId);
        if (tour == null) {
            flash.addFlashAttribute("bookingError", "Tour not found.");
            return "redirect:/tour";
        }
        List<String> wishlist = getWishlist(session);
        if (!wishlist.contains(tour.id())) {
            wishlist.add(tour.id());
            flash.addFlashAttribute("wishlistMessage", tour.name() + " saved to wishlist.");
        } else {
            flash.addFlashAttribute("wishlistMessage", tour.name() + " is already in your wishlist.");
        }
        return "redirect:/tour/wishlist";
    }

    @GetMapping("/remove/{tourId}")
    public String wishlistRemove(@PathVariable String tourId,
                                 HttpSession session,
                                 RedirectAttributes flash) {
        boolean removed = getWishlist(session).remove(tourId);
        flash.addFlashAttribute("wishlistMessage",
                removed ? "Removed from wishlist." : "Tour was not in your wishlist.");
        return "redirect:/tour/wishlist";
    }

    @SuppressWarnings("unchecked")
    private List<String> getWishlist(HttpSession session) {
        Object attribute = session.getAttribute(WISHLIST_KEY);
        if (attribute instanceof List<?>) {
            return (List<String>) attribute;
        }
        List<String> wishlist = new ArrayList<>();
        session.setAttribute(WISHLIST_KEY, wishlist);
        return wishlist;
    }
}
