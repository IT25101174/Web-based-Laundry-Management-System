package com.cleantrack.controller;

import com.cleantrack.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final com.cleantrack.repository.AuditLogRepository auditLogRepository;
    private final com.cleantrack.repository.OrderRepository orderRepository;
    private final com.cleantrack.repository.InvoiceRepository invoiceRepository;
    private final com.cleantrack.repository.ComplaintRepository complaintRepository;
    private final com.cleantrack.repository.ReportNoteRepository reportNoteRepository;

    @Autowired
    public ReportController(com.cleantrack.repository.AuditLogRepository auditLogRepository,
                            com.cleantrack.repository.OrderRepository orderRepository,
                            com.cleantrack.repository.InvoiceRepository invoiceRepository,
                            com.cleantrack.repository.ComplaintRepository complaintRepository,
                            com.cleantrack.repository.ReportNoteRepository reportNoteRepository) {
        this.auditLogRepository = auditLogRepository;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.complaintRepository = complaintRepository;
        this.reportNoteRepository = reportNoteRepository;
    }

    @GetMapping
    public String viewDashboard(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Security check: Only Admins and Branch Supervisors can view reports
        String role = user.getRole() != null ? user.getRole().name() : "";
        if (!"ADMIN".equals(role) && !"BRANCH_SUPERVISOR".equals(role)) {
            return "redirect:/dashboard?error=Unauthorized";
        }

        // --- ORDER STATS ---
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("ordersPlaced", orderRepository.countByStatus("Order Placed"));
        model.addAttribute("ordersReceived", orderRepository.countByStatus("RECEIVED"));
        model.addAttribute("ordersWashing", orderRepository.countByStatus("WASHING"));
        model.addAttribute("ordersDrying", orderRepository.countByStatus("DRYING"));
        model.addAttribute("ordersIroning", orderRepository.countByStatus("IRONING"));
        model.addAttribute("ordersReady", orderRepository.countByStatus("READY FOR COLLECTION"));
        model.addAttribute("ordersCompleted", orderRepository.countByStatus("COMPLETED"));
        model.addAttribute("ordersCancelled", orderRepository.countByStatus("Cancelled"));
        
        String popularService = orderRepository.findMostPopularService();
        model.addAttribute("popularService", popularService != null ? popularService : "N/A");

        // --- FINANCIAL STATS ---
        Double totalRev = invoiceRepository.sumTotalAmountByStatus("PAID");
        model.addAttribute("totalRevenue", totalRev != null ? totalRev : 0.0);
        model.addAttribute("paidInvoices", invoiceRepository.countByStatus("PAID"));
        
        long unpaidCount = invoiceRepository.countByStatus("UNPAID");
        long pendingCount = invoiceRepository.countByStatus("PENDING_APPROVAL");
        model.addAttribute("unpaidInvoices", unpaidCount + pendingCount);

        // --- COMPLAINT STATS ---
        model.addAttribute("totalComplaints", complaintRepository.count());
        model.addAttribute("openComplaints", complaintRepository.countByStatus("OPEN"));
        model.addAttribute("progressComplaints", complaintRepository.countByStatus("IN_PROGRESS"));
        model.addAttribute("resolvedComplaints", complaintRepository.countByStatus("RESOLVED"));

        // --- AUDIT LOGS ---
        model.addAttribute("auditLogs", auditLogRepository.findTop20ByOrderByTimestampDesc());
        // --- EXECUTIVE NOTES ---
        model.addAttribute("reportNotes", reportNoteRepository.findAllByOrderByCreatedAtDesc());
        
        model.addAttribute("user", user);
        return "reports";
    }

    @PostMapping("/note/add")
    public String addNote(@RequestParam String noteText, jakarta.servlet.http.HttpSession session, RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || (!"ADMIN".equals(user.getRole().name()) && !"BRANCH_SUPERVISOR".equals(user.getRole().name()))) {
            return "redirect:/login";
        }
        
        com.cleantrack.model.ReportNote note = new com.cleantrack.model.ReportNote(noteText, user.getFullName());
        reportNoteRepository.save(note);
        auditLogRepository.save(new com.cleantrack.model.AuditLog("Executive Note added by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Note added successfully.");
        return "redirect:/reports";
    }

    @PostMapping("/note/edit/{id}")
    public String editNote(@PathVariable Long id, @RequestParam String noteText, jakarta.servlet.http.HttpSession session, RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || (!"ADMIN".equals(user.getRole().name()) && !"BRANCH_SUPERVISOR".equals(user.getRole().name()))) {
            return "redirect:/login";
        }
        
        reportNoteRepository.findById(id).ifPresent(note -> {
            note.setNoteText(noteText);
            reportNoteRepository.save(note);
            auditLogRepository.save(new com.cleantrack.model.AuditLog("Executive Note updated by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Note updated successfully.");
        });
        return "redirect:/reports";
    }

    @PostMapping("/note/delete/{id}")
    public String deleteNote(@PathVariable Long id, jakarta.servlet.http.HttpSession session, RedirectAttributes redirectAttributes) {
        com.cleantrack.model.User user = (com.cleantrack.model.User) session.getAttribute("user");
        if (user == null || (!"ADMIN".equals(user.getRole().name()) && !"BRANCH_SUPERVISOR".equals(user.getRole().name()))) {
            return "redirect:/login";
        }
        
        reportNoteRepository.findById(id).ifPresent(note -> {
            reportNoteRepository.delete(note);
            auditLogRepository.save(new com.cleantrack.model.AuditLog("Executive Note deleted by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Note deleted successfully.");
        });
        return "redirect:/reports";
    }
}
