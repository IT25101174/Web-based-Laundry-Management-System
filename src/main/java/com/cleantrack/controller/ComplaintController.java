package com.cleantrack.controller;

import com.cleantrack.repository.ComplaintRepository;
import com.cleantrack.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public ComplaintController(ComplaintRepository complaintRepository, OrderRepository orderRepository) {
        this.complaintRepository = complaintRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String listComplaints(HttpSession session, Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("complaints", complaintRepository.findAll());
        model.addAttribute("user", user);
        return "complaint-form";
    }

    @PostMapping("/submit")
    public String submitComplaint(@RequestParam Long orderId,
                                  @RequestParam String issueDescription,
                                  HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        com.cleantrack.model.Complaint complaint = new com.cleantrack.model.Complaint(orderId, issueDescription, "OPEN");
        complaintRepository.save(complaint);
        return "redirect:/complaints";
    }

    @PostMapping("/resolve/{id}")
    public String resolveComplaint(@PathVariable Long id, HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }
        complaintRepository.findById(id).ifPresent(c -> {
            c.setStatus("RESOLVED");
            complaintRepository.save(c);
        });
        return "redirect:/complaints";
    }
}
