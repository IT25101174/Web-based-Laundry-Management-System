package com.cleantrack.repository;

import com.cleantrack.model.ReportNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportNoteRepository extends JpaRepository<ReportNote, Long> {
    List<ReportNote> findAllByOrderByCreatedAtDesc();
}
