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
    private final com.cleantrack.repository.AuditLogRepository auditLogRepository;

    @Autowired
    public InventoryController(InventoryItemRepository inventoryItemRepository, com.cleantrack.repository.AuditLogRepository auditLogRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listInventory(HttpSession session, Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        java.util.List<com.cleantrack.model.InventoryItem> items = inventoryItemRepository.findAll();
        
        java.math.BigDecimal totalValue = java.math.BigDecimal.ZERO;
        for (com.cleantrack.model.InventoryItem item : items) {
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                totalValue = totalValue.add(item.getUnitPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
            }
        }
        
        model.addAttribute("items", items);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("user", user);
        return "inventory-list";
    }

    @PostMapping("/add")
    public String addItem(@RequestParam String itemName,
                          @RequestParam Integer quantity,
                          @RequestParam Integer lowStockThreshold,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String unit,
                          @RequestParam(required = false) String supplier,
                          @RequestParam(required = false) java.math.BigDecimal unitPrice,
                          HttpSession session) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null) || "COUNTER_STAFF".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        com.cleantrack.model.InventoryItem item = new com.cleantrack.model.InventoryItem(itemName, quantity, lowStockThreshold, category, unit, supplier, unitPrice);
        inventoryItemRepository.save(item);
        auditLogRepository.save(new com.cleantrack.model.AuditLog("Inventory item added: " + itemName + " by " + user.getFullName()));
        return "redirect:/inventory";
    }

    @PostMapping("/restock/{id}")
    public String restockItem(@PathVariable Long id, @RequestParam Integer amount, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null) || "COUNTER_STAFF".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Restock amount must be greater than zero.");
            return "redirect:/inventory";
        }
        inventoryItemRepository.findById(id).ifPresent(item -> {
            item.setQuantity(item.getQuantity() + amount);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
            auditLogRepository.save(new com.cleantrack.model.AuditLog("Restocked " + amount + " units of " + item.getItemName() + " by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Successfully restocked " + item.getItemName());
        });
        return "redirect:/inventory";
    }

    @PostMapping("/consume/{id}")
    public String consumeItem(@PathVariable Long id, @RequestParam Integer amount, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Consume amount must be greater than zero.");
            return "redirect:/inventory";
        }
        inventoryItemRepository.findById(id).ifPresent(item -> {
            if (item.getQuantity() < amount) {
                redirectAttributes.addFlashAttribute("error", "Cannot consume more than available stock for " + item.getItemName());
            } else {
                item.setQuantity(item.getQuantity() - amount);
                item.setUpdatedAt(LocalDateTime.now());
                inventoryItemRepository.save(item);
                auditLogRepository.save(new com.cleantrack.model.AuditLog("Consumed " + amount + " units of " + item.getItemName() + " by " + user.getFullName()));
                redirectAttributes.addFlashAttribute("success", "Successfully recorded usage of " + item.getItemName());
            }
        });
        return "redirect:/inventory";
    }

    @PostMapping("/edit/{id}")
    public String editItem(@PathVariable Long id,
                           @RequestParam String itemName,
                           @RequestParam Integer lowStockThreshold,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String unit,
                           @RequestParam(required = false) String supplier,
                           @RequestParam(required = false) java.math.BigDecimal unitPrice,
                           HttpSession session,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null) || "COUNTER_STAFF".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        inventoryItemRepository.findById(id).ifPresent(item -> {
            item.setItemName(itemName);
            item.setLowStockThreshold(lowStockThreshold);
            item.setCategory(category);
            item.setUnit(unit);
            item.setSupplier(supplier);
            item.setUnitPrice(unitPrice);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
            auditLogRepository.save(new com.cleantrack.model.AuditLog("Updated inventory details for " + itemName + " by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Inventory item updated successfully.");
        });
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null) || "COUNTER_STAFF".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        inventoryItemRepository.findById(id).ifPresent(item -> {
            inventoryItemRepository.delete(item);
            auditLogRepository.save(new com.cleantrack.model.AuditLog("Deleted inventory item " + item.getItemName() + " by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Inventory item removed.");
        });
        return "redirect:/inventory";
    }
}
