package com.boldtechnology.nailsalonregister;

public final class SaleItem {
    public final long id;
    public final String customerId;
    public final String serviceSummary;
    public final long totalCents;
    public final String paymentMethod;
    public final long createdAt;

    public SaleItem(long id, String customerId, String serviceSummary, long totalCents,
                    String paymentMethod, long createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.serviceSummary = serviceSummary;
        this.totalCents = totalCents;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
    }
}
