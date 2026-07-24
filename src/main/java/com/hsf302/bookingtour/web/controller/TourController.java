package com.hsf302.bookingtour.web.controller;

import com.hsf302.bookingtour.storage.R2ImageStorageService;
import com.hsf302.bookingtour.web.model.Tour;
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

import java.util.List;

@Controller
@RequestMapping("/tour")
public class TourController {

    private final R2ImageStorageService imageStorageService;

    public TourController(R2ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public String tours(@RequestParam(value = "chat", required = false) String chat, Model model) {
        model.addAttribute("tours", Tour.all());
        model.addAttribute("featuredTour", Tour.all().get(0));
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
        Tour tour = Tour.findOrNull(id);
        if (tour == null) {
            return "redirect:/tour";
        }
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
        Tour tour = Tour.findOrNull(id);
        if (tour == null) {
            return "redirect:/tour";
        }
        model.addAttribute("tour", tour);
        return "booking";
    }

    @GetMapping("/blog")
    public String blog() {
        return "blog";
    }

    @GetMapping("/chat")
    public String chat() {
        return "redirect:/tour?chat=1";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("tours", Tour.all());
        model.addAttribute("totalTours", Tour.all().size());
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
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = imageStorageService.uploadTourImage(imageFile);
                flash.addFlashAttribute("adminMessage",
                        "Uploaded image for " + tourForm.name() + " to R2: " + imageUrl + " (tour data not saved - demo only).");
                return "redirect:/tour/admin";
            } catch (RuntimeException exception) {
                model.addAttribute("adminMessage", "Image upload failed: " + exception.getMessage());
                model.addAttribute("tourForm", tourForm);
                boolean isCreate = tourForm.id() == null || tourForm.id().isBlank();
                model.addAttribute("formMode", isCreate ? "Create" : "Update");
                model.addAttribute("pageTitle", isCreate ? "Create Tour" : "Edit Tour");
                return "admin-tour-form";
            }
        }

        flash.addFlashAttribute("adminMessage", "Saved tour " + tourForm.name() + " (demo only).");
        return "redirect:/tour/admin";
    }

    @GetMapping("/admin/edit/{id}")
    public String adminEditForm(@PathVariable String id, Model model) {
        Tour tour = Tour.findOrNull(id);
        if (tour == null) {
            return "redirect:/tour/admin";
        }
        model.addAttribute("tourForm", tour);
        model.addAttribute("formMode", "Update");
        model.addAttribute("pageTitle", "Edit Tour");
        return "admin-tour-form";
    }

    @GetMapping("/admin/delete/{id}")
    public String adminDelete(@PathVariable String id, RedirectAttributes flash) {

        flash.addFlashAttribute("adminMessage", "Deleted tour (demo only).");
        return "redirect:/tour/admin";
    }

    record ChatMessage(String role, String text) {}
}
