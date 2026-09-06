package com.boldtechnology.nailsalonregister;

public final class SaleItem {
    public final long id;
    public final long groupId;
    public final int revisionNumber;
    public final int revisionCount;
    public final String customerId;
    public final String serviceSummary;
    public final long subtotalCents;
    public final long tipCents;
    public final long totalCents;
    public final String paymentMethod;
    public final long createdAt;
    public final long updatedAt;

    public SaleItem(long id, long groupId, int revisionNumber, int revisionCount,
                    String customerId, String serviceSummary, long subtotalCents, long tipCents,
                    long totalCents, String paymentMethod, long createdAt, long updatedAt) {
        this.id = id;
        this.groupId = groupId;
        this.revisionNumber = revisionNumber;
        this.revisionCount = revisionCount;
        this.customerId = customerId;
        this.serviceSummary = serviceSummary;
        this.subtotalCents = subtotalCents;
        this.tipCents = tipCents;
        this.totalCents = totalCents;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
