package com.cleantrack.controller;

import com.cleantrack.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ComplaintController {

    private final ComplaintRepository complaintRepository;

    @Autowired
    public ComplaintController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }
}
