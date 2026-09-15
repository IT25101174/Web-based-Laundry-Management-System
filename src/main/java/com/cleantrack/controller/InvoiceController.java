package com.cleantrack.controller;

import com.cleantrack.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@org.springframework.web.bind.annotation.RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final com.cleantrack.repository.OrderRepository orderRepository;

    @Autowired
    public InvoiceController(InvoiceRepository invoiceRepository, com.cleantrack.repository.OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    @org.springframework.web.bind.annotation.GetMapping
    public String listInvoices(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        java.util.List<com.cleantrack.model.Invoice> invoices = new java.util.ArrayList<>();
        if ("CUSTOMER".equals(user.getRole())) {
            java.util.List<com.cleantrack.model.Order> userOrders = orderRepository.findByCustomerName(user.getUsername());
            for (com.cleantrack.model.Order order : userOrders) {
                invoices.addAll(invoiceRepository.findByOrderId(order.getId()));
            }
        } else {
            invoices = invoiceRepository.findAll();
        }
        
        model.addAttribute("invoices", invoices);
        model.addAttribute("user", user);
        return "invoice-view";
    }

    @org.springframework.web.bind.annotation.PostMapping("/generate/{orderId}")
    public String generateInvoice(@org.springframework.web.bind.annotation.PathVariable Long orderId, jakarta.servlet.http.HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }
        
        // Mock generation logic: Flat $25 for WASH_ONLY, $45 for WASH_IRON, $65 for DRY_CLEAN
        java.util.Optional<com.cleantrack.model.Order> optOrder = orderRepository.findById(orderId);
        if (optOrder.isPresent()) {
            com.cleantrack.model.Order order = optOrder.get();
            java.math.BigDecimal amount = new java.math.BigDecimal("25.00");
            if ("WASH_IRON".equals(order.getServiceType())) amount = new java.math.BigDecimal("45.00");
            if ("DRY_CLEAN".equals(order.getServiceType())) amount = new java.math.BigDecimal("65.00");
            
            com.cleantrack.model.Invoice invoice = new com.cleantrack.model.Invoice(orderId, amount, "UNPAID");
            invoiceRepository.save(invoice);
            
            // Update order status
            order.setStatus("PROCESSING");
            orderRepository.save(order);
        }
        
        return "redirect:/invoices";
    }
}
