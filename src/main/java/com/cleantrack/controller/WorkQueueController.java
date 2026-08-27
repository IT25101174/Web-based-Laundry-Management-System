package com.cleantrack.controller;

import com.cleantrack.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WorkQueueController {

    private final OrderRepository orderRepository;

    @Autowired
    public WorkQueueController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
