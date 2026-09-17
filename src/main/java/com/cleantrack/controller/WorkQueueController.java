package com.cleantrack.controller;

import com.cleantrack.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WorkQueueController {

    private final OrderRepository orderRepository;
    private final com.cleantrack.repository.StatusUpdateRepository statusUpdateRepository;
    private final com.cleantrack.repository.AuditLogRepository auditLogRepository;

    @Autowired
    public WorkQueueController(OrderRepository orderRepository, com.cleantrack.repository.StatusUpdateRepository statusUpdateRepository, com.cleantrack.repository.AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @org.springframework.web.bind.annotation.GetMapping("/queue")
    public String viewQueue(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        // Fetch active orders (not completed, not cancelled)
        java.util.List<com.cleantrack.model.Order> allOrders = orderRepository.findAll();
        java.util.List<com.cleantrack.model.Order> activeOrders = new java.util.ArrayList<>();
        
        for (com.cleantrack.model.Order o : allOrders) {
            if (!"COMPLETED".equals(o.getStatus()) && !"Cancelled".equals(o.getStatus())) {
                activeOrders.add(o);
            }
        }
        
        model.addAttribute("orders", activeOrders);
        model.addAttribute("user", user);
        return "work-queue";
    }

    @org.springframework.web.bind.annotation.PostMapping("/queue/{id}/advance")
    public String advanceStage(@org.springframework.web.bind.annotation.PathVariable Long id, jakarta.servlet.http.HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        java.util.Optional<com.cleantrack.model.Order> optOrder = orderRepository.findById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/queue?error=OrderNotFound";
        }
        
        com.cleantrack.model.Order order = optOrder.get();
        String currentStatus = order.getStatus();
        String nextStatus = getNextStage(currentStatus);

        if (nextStatus == null) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be advanced further.");
            return "redirect:/queue";
        }

        // Only Laundry Staff, Admin, or Branch Supervisor can advance stages. Counter staff can only accept or hand out.
        String role = user.getRole() != null ? user.getRole().name() : "";
        if ("COUNTER_STAFF".equals(role) && !currentStatus.equals("Order Placed") && !currentStatus.equals("READY FOR COLLECTION")) {
             redirectAttributes.addFlashAttribute("error", "You do not have permission to advance laundry processing stages.");
             return "redirect:/queue";
        }

        order.setStatus(nextStatus);
        orderRepository.save(order);

        // Record history
        com.cleantrack.model.StatusUpdate update = new com.cleantrack.model.StatusUpdate(order, nextStatus, user);
        statusUpdateRepository.save(update);
        auditLogRepository.save(new com.cleantrack.model.AuditLog("Order ID " + order.getId() + " advanced to " + nextStatus + " by " + user.getFullName()));

        // Logging as MVP Notification
        System.out.println("========== NOTIFICATION ==========");
        System.out.println("To Customer: Order " + order.getTrackingId() + " has reached stage: " + nextStatus);
        System.out.println("==================================");

        redirectAttributes.addFlashAttribute("success", "Order advanced to " + nextStatus);
        return "redirect:/queue";
    }

    @org.springframework.web.bind.annotation.PostMapping("/queue/{id}/cancel")
    public String cancelOrder(@org.springframework.web.bind.annotation.PathVariable Long id, jakarta.servlet.http.HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        java.util.Optional<com.cleantrack.model.Order> optOrder = orderRepository.findById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/queue?error=OrderNotFound";
        }
        
        com.cleantrack.model.Order order = optOrder.get();
        String currentStatus = order.getStatus();

        if ("COMPLETED".equals(currentStatus) || "Cancelled".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "This order cannot be cancelled anymore.");
            return "redirect:/queue";
        }

        // Only Branch Supervisors or Admins should be able to cancel mid-processing
        String role = user.getRole() != null ? user.getRole().name() : "";
        if ("LAUNDRY_STAFF".equals(role) || "COUNTER_STAFF".equals(role)) {
             redirectAttributes.addFlashAttribute("error", "Only Supervisors or Admins can cancel an order in processing.");
             return "redirect:/queue";
        }

        order.setStatus("Cancelled");
        orderRepository.save(order);

        // Record history
        com.cleantrack.model.StatusUpdate update = new com.cleantrack.model.StatusUpdate(order, "Cancelled", user);
        statusUpdateRepository.save(update);
        auditLogRepository.save(new com.cleantrack.model.AuditLog("Order ID " + order.getId() + " cancelled by " + user.getFullName()));

        System.out.println("========== NOTIFICATION ==========");
        System.out.println("To Customer: Order " + order.getTrackingId() + " processing has been Cancelled.");
        System.out.println("==================================");

        redirectAttributes.addFlashAttribute("success", "Order has been cancelled.");
        return "redirect:/queue";
    }
    
    @org.springframework.web.bind.annotation.GetMapping("/queue/{id}/history")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, String>> getHistory(@org.springframework.web.bind.annotation.PathVariable Long id) {
        java.util.List<com.cleantrack.model.StatusUpdate> updates = statusUpdateRepository.findByOrderIdOrderByChangedAtDesc(id);
        java.util.List<java.util.Map<String, String>> historyList = new java.util.ArrayList<>();
        
        for (com.cleantrack.model.StatusUpdate u : updates) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("stage", u.getStage());
            map.put("updatedBy", u.getUpdatedBy().getFullName());
            map.put("date", u.getChangedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            historyList.add(map);
        }
        return historyList;
    }

    private String getNextStage(String current) {
        if (current == null) return "RECEIVED";
        
        switch (current) {
            case "Order Placed": return "RECEIVED";
            case "RECEIVED": return "WASHING";
            case "WASHING": return "DRYING";
            case "DRYING": return "IRONING";
            case "IRONING": return "READY FOR COLLECTION";
            case "READY FOR COLLECTION": return "COMPLETED";
            default: return null;
        }
    }
}
