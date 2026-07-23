package com.hsf302.bookingtour.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tour")
public class TourController {

    private final List<Tour> tours = new ArrayList<>(List.of(
            new Tour("t1", "Sapa 3N2Đ", "Sapa, Vietnam", 429, 4.9, "Mountain views, local market, and cable car."),
            new Tour("t2", "Da Lat Chill", "Da Lat, Vietnam", 319, 4.8, "Coffee farms, pine hills, and lakeside sunsets."),
            new Tour("t3", "Phu Quoc Escape", "Phu Quoc, Vietnam", 559, 4.9, "Beach resort, snorkelling, and night market."),
            new Tour("t4", "Ha Long Cruise", "Ha Long Bay, Vietnam", 489, 4.7, "Luxury cruise, cave visit, and sunset deck.")
    ));

    @GetMapping
    public String tours(@RequestParam(value = "chat", required = false) String chat, Model model) {
        model.addAttribute("tours", tours);
        model.addAttribute("featuredTour", tours.get(0));
        model.addAttribute("chatMessages", List.of(
                new ChatMessage("user", "Tour Sapa có gì?"),
                new ChatMessage("assistant", "Sapa 3N2Đ có mountain views, local market, và cable car."),
                new ChatMessage("user", "Tour nào cao cấp nhất?"),
                new ChatMessage("assistant", "Phu Quoc Escape là tour giá cao nhất trong dataset demo này.")
        ));
        model.addAttribute("knowledgeBase", List.of(
                "Sapa 3N2Đ - 429 USD",
                "Da Lat Chill - 319 USD",
                "Phu Quoc Escape - 559 USD",
                "Ha Long Cruise - 489 USD"
        ));
        model.addAttribute("openChat", chat != null);
        return "tour-list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable String id, Model model) {
        Tour tour = findTour(id);
        model.addAttribute("tour", tour);
        model.addAttribute("highlights", List.of(
                "Instant confirmation demo",
                "Flexible schedule",
                "Local guide support",
                "Hotel and transfer included"
        ));
        return "tour-detail";
    }

    @GetMapping("/book/{id}")
    public String book(@PathVariable String id, Model model) {
        model.addAttribute("tour", findTour(id));
        return "booking";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String tourId,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam int travelers,
                           @RequestParam String paymentMethod,
                           RedirectAttributes flash) {
        Tour tour = findTour(tourId);
        flash.addFlashAttribute("checkoutMessage",
                "Mock payment approved for " + fullName + " on " + tour.name() + " via " + paymentMethod + ".");
        flash.addFlashAttribute("checkoutTour", tour);
        flash.addFlashAttribute("checkoutTravelers", travelers);
        flash.addFlashAttribute("checkoutEmail", email);
        return "redirect:/tour/confirmation";
    }

    @GetMapping("/confirmation")
    public String confirmation() {
        return "confirmation";
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        return "blog";
    }

    @GetMapping("/chat")
    public String chat() {
        return "redirect:/tour?chat=1";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("tours", tours);
        model.addAttribute("totalTours", tours.size());
        return "admin-tours";
    }

    @GetMapping("/admin/create")
    public String adminCreateForm(Model model) {
        model.addAttribute("tourForm", new Tour("", "", "", 0, 0.0, ""));
        model.addAttribute("formMode", "Create");
        model.addAttribute("pageTitle", "Create Tour");
        return "admin-tour-form";
    }

    @PostMapping("/admin/save")
    public String adminSave(@ModelAttribute("tourForm") Tour tourForm, RedirectAttributes flash) {
        if (tourForm.id() == null || tourForm.id().isBlank()) {
            tourForm = new Tour(nextTourId(), tourForm.name(), tourForm.location(), tourForm.price(), tourForm.rating(), tourForm.summary());
        }
        removeTourById(tourForm.id());
        tours.add(tourForm);
        flash.addFlashAttribute("adminMessage", "Saved tour " + tourForm.name() + " successfully.");
        return "redirect:/tour/admin";
    }

    @GetMapping("/admin/edit/{id}")
    public String adminEditForm(@PathVariable String id, Model model) {
        Tour tour = findTour(id);
        model.addAttribute("tourForm", tour);
        model.addAttribute("formMode", "Update");
        model.addAttribute("pageTitle", "Edit Tour");
        return "admin-tour-form";
    }

    @GetMapping("/admin/delete/{id}")
    public String adminDelete(@PathVariable String id, RedirectAttributes flash) {
        Tour removed = removeTourById(id);
        flash.addFlashAttribute("adminMessage", removed == null ? "Tour not found." : "Deleted tour " + removed.name() + ".");
        return "redirect:/tour/admin";
    }

    private Tour findTour(String id) {
        return tours.stream()
                .filter(tour -> tour.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(tours.get(0));
    }

    private Tour removeTourById(String id) {
        for (int i = 0; i < tours.size(); i++) {
            if (tours.get(i).id().equalsIgnoreCase(id)) {
                return tours.remove(i);
            }
        }
        return null;
    }

    private String nextTourId() {
        return "t" + (tours.size() + 1);
    }

    record Tour(String id, String name, String location, int price, double rating, String summary) {}
    record ChatMessage(String role, String text) {}
}
