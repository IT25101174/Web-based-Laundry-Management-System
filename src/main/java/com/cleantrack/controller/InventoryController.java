package com.cleantrack.controller;

import com.cleantrack.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;

    @Autowired
    public InventoryController(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @GetMapping
    public String listInventory(HttpSession session, Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("items", inventoryItemRepository.findAll());
        model.addAttribute("user", user);
        return "inventory-list";
    }

    @PostMapping("/add")
    public String addItem(@RequestParam String itemName,
                          @RequestParam Integer quantity,
                          @RequestParam Integer lowStockThreshold,
                          HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }
        com.cleantrack.model.InventoryItem item = new com.cleantrack.model.InventoryItem(itemName, quantity, lowStockThreshold);
        inventoryItemRepository.save(item);
        return "redirect:/inventory";
    }

    @PostMapping("/restock/{id}")
    public String restockItem(@PathVariable Long id, @RequestParam Integer amount, HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }
        inventoryItemRepository.findById(id).ifPresent(item -> {
            item.setQuantity(item.getQuantity() + amount);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
        });
        return "redirect:/inventory";
    }
}
