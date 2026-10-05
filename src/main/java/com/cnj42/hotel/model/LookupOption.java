package com.cnj42.hotel.model;

public class LookupOption {
    private final Integer id;
    private final String label;

    public LookupOption(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public Integer getId() { return id; }
    public String getLabel() { return label; }

    @Override
    public String toString() {
        return label;
    }
}