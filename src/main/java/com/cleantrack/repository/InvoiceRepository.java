package com.cleantrack.repository;

import com.cleantrack.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByOrderId(Long orderId);
    long countByStatus(String status);
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = :status")
    Double sumTotalAmountByStatus(@org.springframework.data.repository.query.Param("status") String status);
}
