package com.hmdp.service;

import com.hmdp.entity.VoucherOrder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class OrderReceiver {
    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = "voucherOrder.queue")
    public void receiveOrder(VoucherOrder order) {
        voucherOrderService.handlerVoucherOrder(order);
    }
}
