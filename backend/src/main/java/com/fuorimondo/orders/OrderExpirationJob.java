package com.fuorimondo.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OrderExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationJob.class);

    private final OrderRepository orderRepository;

    public OrderExpirationJob(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelayString = "PT${fuorimondo.order.expiration-job-interval-minutes:5}M")
    @Transactional
    public int run() {
        int swept = orderRepository.expireStaleOrders(Instant.now());
        if (swept > 0) log.info("Expired {} stale orders", swept);
        return swept;
    }
}
