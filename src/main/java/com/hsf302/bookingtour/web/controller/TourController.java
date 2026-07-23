package com.hsf302.bookingtour.web.controller;

import com.hsf302.bookingtour.storage.R2ImageStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tour")
public class TourController {

    private final R2ImageStorageService imageStorageService;

    private final List<Tour> tours = new ArrayList<>(List.of(
            new Tour("t1", "Sapa 3N2D", "Sapa, Vietnam", 429, 4.9,
                    "Mountain views, local market, and cable car.",
                    "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?auto=format&fit=crop&w=1200&q=80"),
            new Tour("t2", "Da Lat Chill", "Da Lat, Vietnam", 319, 4.8,
                    "Coffee farms, pine hills, and lakeside sunsets.",
                    "https://images.unsplash.com/photo-1540202404-a2f29016b523?auto=format&fit=crop&w=1200&q=80"),
            new Tour("t3", "Phu Quoc Escape", "Phu Quoc, Vietnam", 559, 4.9,
                    "Beach resort, snorkelling, and night market.",
                    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80"),
            new Tour("t4", "Ha Long Cruise", "Ha Long Bay, Vietnam", 489, 4.7,
                    "Luxury cruise, cave visit, and sunset deck.",
                    "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80")
    ));

    public TourController(R2ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public String tours(@RequestParam(value = "chat", required = false) String chat, Model model) {
        model.addAttribute("tours", tours);
        model.addAttribute("featuredTour", tours.get(0));
        model.addAttribute("chatMessages", List.of(
                new ChatMessage("user", "Tour Sapa co gi?"),
                new ChatMessage("assistant", "Sapa 3N2D co mountain views, local market, va cable car."),
                new ChatMessage("user", "Tour nao cao cap nhat?"),
                new ChatMessage("assistant", "Phu Quoc Escape la tour gia cao nhat trong dataset demo nay.")
        ));
        model.addAttribute("knowledgeBase", List.of(
                "Sapa 3N2D - 429 USD",
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
        model.addAttribute("tourForm", new Tour("", "", "", 0, 0.0, "", ""));
        model.addAttribute("formMode", "Create");
        model.addAttribute("pageTitle", "Create Tour");
        return "admin-tour-form";
    }

    @PostMapping("/admin/save")
    public String adminSave(@ModelAttribute("tourForm") Tour tourForm,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            Model model,
                            RedirectAttributes flash) {
        String imageUrl = tourForm.imageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                imageUrl = imageStorageService.uploadTourImage(imageFile);
                flash.addFlashAttribute("adminMessage", "Uploaded image to R2 and saved tour " + tourForm.name() + ".");
            } catch (RuntimeException exception) {
                model.addAttribute("adminMessage", "Image upload failed: " + exception.getMessage());
                model.addAttribute("tourForm", tourForm);
                model.addAttribute("formMode", tourForm.id() == null || tourForm.id().isBlank() ? "Create" : "Update");
                model.addAttribute("pageTitle", tourForm.id() == null || tourForm.id().isBlank() ? "Create Tour" : "Edit Tour");
                return "admin-tour-form";
            }
        }
        if (tourForm.id() == null || tourForm.id().isBlank()) {
            tourForm = new Tour(nextTourId(), tourForm.name(), tourForm.location(), tourForm.price(),
                    tourForm.rating(), tourForm.summary(), imageUrl);
        } else {
            tourForm = new Tour(tourForm.id(), tourForm.name(), tourForm.location(), tourForm.price(),
                    tourForm.rating(), tourForm.summary(), imageUrl);
        }
        removeTourById(tourForm.id());
        tours.add(tourForm);
        if (!flash.getFlashAttributes().containsKey("adminMessage")) {
            flash.addFlashAttribute("adminMessage", "Saved tour " + tourForm.name() + " successfully.");
        }
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

    record Tour(String id, String name, String location, int price, double rating, String summary, String imageUrl) {}
    record ChatMessage(String role, String text) {}
}
