package com.cleantrack.controller;

import com.cleantrack.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String listOrders(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        java.util.List<com.cleantrack.model.Order> orders;
        if ("CUSTOMER".equals(user.getRole())) {
            orders = orderRepository.findByCustomerName(user.getUsername());
        } else {
            orders = orderRepository.findAll(); // Staff/Admin see all orders
        }
        model.addAttribute("orders", orders);
        model.addAttribute("user", user);
        return "order-list";
    }

    @GetMapping("/new")
    public String showOrderForm(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "order-form";
    }

    @PostMapping("/new")
    public String createOrder(@org.springframework.web.bind.annotation.RequestParam String serviceType, jakarta.servlet.http.HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Save the order
        com.cleantrack.model.Order order = new com.cleantrack.model.Order(user.getUsername(), serviceType, "PENDING");
        orderRepository.save(order);
        
        return "redirect:/orders";
    }
}
