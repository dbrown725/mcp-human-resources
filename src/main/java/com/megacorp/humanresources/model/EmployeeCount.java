package com.megacorp.humanresources.model;

public class EmployeeCount {
    private Long count;

    public EmployeeCount(Long count) {
        this.count = count;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "EmployeeCount{" +
                "count=" + count +
                '}';
    }
}
