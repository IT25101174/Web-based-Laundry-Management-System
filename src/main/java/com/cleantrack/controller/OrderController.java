package com.cleantrack.controller;

import com.cleantrack.model.Order;
import com.cleantrack.model.User;
import com.cleantrack.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private User getSessionUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }

    @GetMapping
    public String listOrders(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        List<Order> orders;
        if ("CUSTOMER".equals(user.getRole())) {
            orders = orderRepository.findByCustomerId(user.getId());
        } else {
            orders = orderRepository.findAll();
        }
        
        model.addAttribute("orders", orders);
        model.addAttribute("user", user);
        return "order-list";
    }

    @GetMapping("/new")
    public String showOrderForm(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("order", new Order());
        model.addAttribute("user", user);
        return "order-form";
    }

    @PostMapping("/new")
    public String createOrder(@ModelAttribute Order order, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        // Basic validation
        if (order.getItemDescription() == null || order.getItemDescription().isEmpty() ||
            order.getQuantity() == null || order.getQuantity() <= 0 ||
            order.getServiceType() == null || order.getServiceType().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please fill all required fields correctly.");
            return "redirect:/orders/new";
        }

        order.setTrackingId("CT-" + System.currentTimeMillis());
        order.setStatus("Order Placed");
        
        // If customer is creating it, set them as customer. 
        // If counter staff is creating it, ideally we'd pick a customer from a dropdown, 
        // but for this MVP lean edition, we will set customer to the logged in user if they are CUSTOMER,
        // otherwise if it's staff, we'll assign the staff as the counterStaff, but we need a customer.
        // For simplicity, we'll assume the logged-in user is the customer for now unless specified.
        // Since we bind directly, customer might be null.
        if ("CUSTOMER".equals(user.getRole())) {
            order.setCustomer(user);
        } else {
            // For staff creating walk-in orders, we need to assign a customer. 
            // In a real app we'd have a select field. Let's assume staff can create orders for themselves for testing, 
            // or the form passes customer.id.
            if (order.getCustomer() == null || order.getCustomer().getId() == null) {
                 order.setCustomer(user); // Fallback
            }
            order.setCounterStaff(user);
        }

        orderRepository.save(order);
        redirectAttributes.addFlashAttribute("success", "Order created successfully! Tracking ID: " + order.getTrackingId());
        
        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }
        Order order = orderOpt.get();

        if (isProcessingOrBeyond(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be edited once it has entered processing.");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("user", user);
        return "order-form";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(@PathVariable Long id, @ModelAttribute Order updatedOrder, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }
        Order existingOrder = orderOpt.get();

        if (isProcessingOrBeyond(existingOrder.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be edited once it has entered processing.");
            return "redirect:/orders";
        }

        // Update allowed fields
        existingOrder.setItemDescription(updatedOrder.getItemDescription());
        existingOrder.setQuantity(updatedOrder.getQuantity());
        existingOrder.setServiceType(updatedOrder.getServiceType());

        orderRepository.save(existingOrder);
        redirectAttributes.addFlashAttribute("success", "Order updated successfully!");
        
        return "redirect:/orders";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }
        Order order = orderOpt.get();

        if (isProcessingOrBeyond(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be cancelled once it has entered processing.");
            return "redirect:/orders";
        }

        order.setStatus("Cancelled");
        orderRepository.save(order);
        redirectAttributes.addFlashAttribute("success", "Order cancelled successfully!");
        
        return "redirect:/orders";
    }

    private boolean isProcessingOrBeyond(String status) {
        if (status == null) return false;
        // Assume anything other than "Order Placed" or "PENDING" is processing or beyond.
        // Or explicitly check for "WASHING", "DRYING", "IRONING", "READY FOR COLLECTION", "COMPLETED"
        return status.equalsIgnoreCase("WASHING") || 
               status.equalsIgnoreCase("DRYING") || 
               status.equalsIgnoreCase("IRONING") || 
               status.equalsIgnoreCase("READY FOR COLLECTION") || 
               status.equalsIgnoreCase("COMPLETED");
    }
}
