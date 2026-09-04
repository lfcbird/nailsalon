package com.boldtechnology.nailsalonregister;

public final class ServiceItem {
    public final long id;
    public final String name;
    public final long priceCents;

    public ServiceItem(long id, String name, long priceCents) {
        this.id = id;
        this.name = name;
        this.priceCents = priceCents;
    }
}
