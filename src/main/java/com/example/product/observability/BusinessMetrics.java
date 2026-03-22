package com.example.product.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final Counter loginAttempts;
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter signupAttempts;
    private final Counter signupSuccess;
    private final Counter signupFailure;
    private final Counter productsCreated;
    private final Counter orderRequests;
    private final Counter orderRequestPublishSuccess;
    private final Counter orderRequestPublishFailure;
    private final Timer orderRequestPublishDuration;

    public BusinessMetrics(MeterRegistry registry) {
        this.loginAttempts = registry.counter("leninkart.auth.login.attempts");
        this.loginSuccess = registry.counter("leninkart.auth.login.success");
        this.loginFailure = registry.counter("leninkart.auth.login.failure");
        this.signupAttempts = registry.counter("leninkart.auth.signup.attempts");
        this.signupSuccess = registry.counter("leninkart.auth.signup.success");
        this.signupFailure = registry.counter("leninkart.auth.signup.failure");
        this.productsCreated = registry.counter("leninkart.products.created");
        this.orderRequests = registry.counter("leninkart.products.order_requests");
        this.orderRequestPublishSuccess = registry.counter("leninkart.products.order_request_publish.success");
        this.orderRequestPublishFailure = registry.counter("leninkart.products.order_request_publish.failure");
        this.orderRequestPublishDuration = registry.timer("leninkart.products.order_request_publish.duration");
    }

    public void recordLoginAttempt() {
        loginAttempts.increment();
    }

    public void recordLoginSuccess() {
        loginSuccess.increment();
    }

    public void recordLoginFailure() {
        loginFailure.increment();
    }

    public void recordSignupAttempt() {
        signupAttempts.increment();
    }

    public void recordSignupSuccess() {
        signupSuccess.increment();
    }

    public void recordSignupFailure() {
        signupFailure.increment();
    }

    public void recordProductCreated() {
        productsCreated.increment();
    }

    public void recordOrderRequest() {
        orderRequests.increment();
    }

    public void recordOrderRequestPublishSuccess() {
        orderRequestPublishSuccess.increment();
    }

    public void recordOrderRequestPublishFailure() {
        orderRequestPublishFailure.increment();
    }

    public Timer.Sample startOrderRequestPublish() {
        return Timer.start();
    }

    public void stopOrderRequestPublish(Timer.Sample sample) {
        sample.stop(orderRequestPublishDuration);
    }
}
