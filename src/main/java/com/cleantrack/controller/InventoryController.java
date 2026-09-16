package com.cleantrack.controller;

import com.cleantrack.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                          @RequestParam String quantity,
                          @RequestParam String lowStockThreshold,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }

        String validationError = validateItemFields(itemName, quantity, lowStockThreshold);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/inventory";
        }

        com.cleantrack.model.InventoryItem item = new com.cleantrack.model.InventoryItem(
                itemName.trim(),
                parseNonNegativeInt(quantity),
                parseNonNegativeInt(lowStockThreshold));
        inventoryItemRepository.save(item);
        redirectAttributes.addFlashAttribute("success", "Item added successfully.");
        return "redirect:/inventory";
    }

    @PostMapping("/edit/{id}")
    public String editItem(@PathVariable Long id,
                           @RequestParam String itemName,
                           @RequestParam String quantity,
                           @RequestParam String lowStockThreshold,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }

        String validationError = validateItemFields(itemName, quantity, lowStockThreshold);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/inventory";
        }

        Integer parsedQuantity = parseNonNegativeInt(quantity);
        Integer parsedThreshold = parseNonNegativeInt(lowStockThreshold);

        boolean found = inventoryItemRepository.findById(id).map(item -> {
            item.setItemName(itemName.trim());
            item.setQuantity(parsedQuantity);
            item.setLowStockThreshold(parsedThreshold);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
            return true;
        }).orElse(false);

        redirectAttributes.addFlashAttribute(found ? "success" : "error",
                found ? "Item updated successfully." : "Item not found.");
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }

        if (!inventoryItemRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
            return "redirect:/inventory";
        }

        inventoryItemRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Item deleted successfully.");
        return "redirect:/inventory";
    }

    @PostMapping("/restock/{id}")
    public String restockItem(@PathVariable Long id,
                              @RequestParam String amount,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Integer parsedAmount = parseNonNegativeInt(amount);
        if (parsedAmount == null || parsedAmount == 0) {
            redirectAttributes.addFlashAttribute("error", "Restock amount must be a whole number greater than zero.");
            return "redirect:/inventory";
        }

        boolean found = inventoryItemRepository.findById(id).map(item -> {
            item.setQuantity(item.getQuantity() + parsedAmount);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
            return true;
        }).orElse(false);

        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
        }

        return "redirect:/inventory";
    }

    private String validateItemFields(String itemName, String quantity, String lowStockThreshold) {
        if (itemName == null || itemName.isBlank()) {
            return "Item name cannot be empty.";
        }
        if (itemName.trim().length() > 100) {
            return "Item name cannot exceed 100 characters.";
        }
        if (parseNonNegativeInt(quantity) == null) {
            return "Quantity must be a whole number of zero or greater.";
        }
        if (parseNonNegativeInt(lowStockThreshold) == null) {
            return "Low stock threshold must be a whole number of zero or greater.";
        }
        return null;
    }

    private Integer parseNonNegativeInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}