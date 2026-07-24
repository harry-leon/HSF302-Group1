package com.hsf302.bookingtour.web.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.hsf302.bookingtour.web.model.CartItem;
import com.hsf302.bookingtour.web.model.Tour;
import com.hsf302.bookingtour.web.util.CartSession;
import com.hsf302.bookingtour.web.util.PricingRules;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/tour")
public class PaymentController {

    private static final DateTimeFormatter PAID_ON_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ORDER_KEY = "pendingOrder";
    // Demo discount codes only
    private static final Map<String, Double> PROMO_CODES = Map.of(
            "DIEUDAO10", 0.10,
            "WELCOME5", 0.05
    );

    @GetMapping("/payment")
    public String payment(HttpSession session, Model model) {
        List<CartItem> cart = CartSession.get(session);
        if (cart.isEmpty()) {
            return "redirect:/tour/cart";
        }
        double subtotal = cart.stream().mapToDouble(CartItem::subtotal).sum();
        int totalTravelers = cart.stream().mapToInt(CartItem::travelers).sum();
        double tax = PricingRules.tax(subtotal);
        double discount = PricingRules.groupDiscount(subtotal, totalTravelers);

        model.addAttribute("cartItems", cart);
        model.addAttribute("cartSubtotal", subtotal);
        model.addAttribute("cartTax", tax);
        model.addAttribute("cartDiscount", discount);
        model.addAttribute("cartTotal", subtotal - discount + tax);
        return "payment";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam(required = false) String paymentMethod,
                           @RequestParam(required = false) String discountCode,
                           HttpSession session,
                           RedirectAttributes flash) {
        List<CartItem> cart = CartSession.get(session);
        if (cart.isEmpty()) {
            flash.addFlashAttribute("bookingError", "Your cart is empty. Please add a tour before checking out.");
            return "redirect:/tour";
        }

        String promoCode = discountCode != null && !discountCode.isBlank()
                ? discountCode.trim().toUpperCase()
                : null;
        double promoRate = promoCode != null ? PROMO_CODES.getOrDefault(promoCode, 0.0) : 0.0;


        List<CartItem> validItems = new ArrayList<>();
        boolean droppedSome = false;
        for (CartItem item : cart) {
            if (Tour.findOrNull(item.tourId()) != null) {
                validItems.add(item);
            } else {
                droppedSome = true;
            }
        }
        if (validItems.isEmpty()) {
            flash.addFlashAttribute("bookingError", "The tours in your cart are no longer available. Please choose again.");
            CartSession.clear(session);
            return "redirect:/tour";
        }

        double subtotal      = validItems.stream().mapToDouble(CartItem::subtotal).sum();
        int totalTravelers   = validItems.stream().mapToInt(CartItem::travelers).sum();
        double tax           = PricingRules.tax(subtotal);
        double groupDiscount = PricingRules.groupDiscount(subtotal, totalTravelers);
        double promoDiscount = subtotal * promoRate;
        double total         = subtotal - groupDiscount - promoDiscount + tax;

        String transactionId = "MOCK-" + System.currentTimeMillis();
        String qrContent     = "DieuDaoTravel|txn=" + transactionId + "|amount=" + String.format("%.2f", total);

        // Lưu đơn hàng vào session để trang scan & pay đọc
        Map<String, Object> order = new HashMap<>();
        order.put("items",          validItems);
        order.put("fullName",       fullName);
        order.put("email",          email);
        order.put("paymentMethod",  paymentMethod);
        order.put("totalTravelers", totalTravelers);
        order.put("subtotal",       subtotal);
        order.put("tax",            tax);
        order.put("groupDiscount",  groupDiscount);
        order.put("promoDiscount",  promoDiscount);
        order.put("promoCode",      promoCode);
        order.put("total",          total);
        order.put("transactionId",  transactionId);
        order.put("qrImage",        generateQrCodeBase64(qrContent));
        session.setAttribute(ORDER_KEY, order);
        CartSession.clear(session);

        if (droppedSome) {
            flash.addFlashAttribute("payNotice",
                    "Note: one or more tours in your cart were no longer available and were dropped from this order.");
        }
        return "redirect:/tour/checkout/pay";
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/checkout/pay")
    public String checkoutPay(HttpSession session, Model model) {
        Object raw = session.getAttribute(ORDER_KEY);
        if (!(raw instanceof Map)) {
            return "redirect:/tour/cart";
        }
        model.addAttribute("order", (Map<String, Object>) raw);
        return "checkout-pay";
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/checkout/confirm")
    public String checkoutConfirm(HttpSession session, RedirectAttributes flash) {
        Object raw = session.getAttribute(ORDER_KEY);
        if (!(raw instanceof Map)) {
            flash.addFlashAttribute("bookingError", "Your payment session expired. Please check out again.");
            return "redirect:/tour/cart";
        }
        Map<String, Object> order = (Map<String, Object>) raw;
        List<CartItem> items = (List<CartItem>) order.get("items");
        String tourSummary = items.size() == 1 ? items.get(0).tourName() : items.size() + " tours";

        flash.addFlashAttribute("checkoutMessage",
                "Mock payment approved for " + order.get("fullName") + " on " + tourSummary
                        + " via " + order.get("paymentMethod") + ".");
        flash.addFlashAttribute("checkoutItems",         items);
        flash.addFlashAttribute("checkoutTravelers",     order.get("totalTravelers"));
        flash.addFlashAttribute("checkoutEmail",         order.get("email"));
        flash.addFlashAttribute("checkoutPaymentMethod", order.get("paymentMethod"));
        flash.addFlashAttribute("checkoutTax",           order.get("tax"));
        flash.addFlashAttribute("checkoutDiscount",      order.get("groupDiscount"));
        flash.addFlashAttribute("checkoutPromoDiscount", order.get("promoDiscount"));
        flash.addFlashAttribute("checkoutPromoCode",     order.get("promoCode"));
        flash.addFlashAttribute("checkoutTotal",         order.get("total"));
        flash.addFlashAttribute("checkoutTransactionId", order.get("transactionId"));
        flash.addFlashAttribute("checkoutPaidOn", LocalDateTime.now().format(PAID_ON_FORMAT));

        session.removeAttribute(ORDER_KEY);
        return "redirect:/tour/confirmation";
    }

    @GetMapping("/confirmation")
    public String confirmation(Model model) {

        if (!model.containsAttribute("checkoutItems")) {
            return "redirect:/tour";
        }
        return "confirmation";
    }

    /**
     * Renders a QR code (PNG, base64-encoded) for the mock payment receipt.
     * This never talks to a real payment gateway - it just encodes the mock
     * transaction details so the "scan & pay" page has something scannable.
     */
    private String generateQrCodeBase64(String content) {
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException ex) {
            return null;
        }
    }
}
