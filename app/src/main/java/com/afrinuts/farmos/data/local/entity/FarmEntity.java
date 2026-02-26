package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Farm entity representing a cashew farm.
 * Designed for multi-farm future from Day 1.
 */
@Entity(tableName = "farms")
public class FarmEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String location;
    private double totalHectares;
    private double cashewHectares;
    private int treesPerHectare;
    private Integer plantingYear; // Integer allows null
    private long createdAt;
    private long updatedAt;

    // Constructor
    public FarmEntity(String name, String location, double totalHectares,
                      double cashewHectares, int treesPerHectare, Integer plantingYear) {
        this.name = name;
        this.location = location;
        this.totalHectares = totalHectares;
        this.cashewHectares = cashewHectares;
        this.treesPerHectare = treesPerHectare;
        this.plantingYear = plantingYear;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getTotalHectares() {
        return totalHectares;
    }

    public void setTotalHectares(double totalHectares) {
        this.totalHectares = totalHectares;
    }

    public double getCashewHectares() {
        return cashewHectares;
    }

    public void setCashewHectares(double cashewHectares) {
        this.cashewHectares = cashewHectares;
    }

    public int getTreesPerHectare() {
        return treesPerHectare;
    }

    public void setTreesPerHectare(int treesPerHectare) {
        this.treesPerHectare = treesPerHectare;
    }

    public Integer getPlantingYear() {
        return plantingYear;
    }

    public void setPlantingYear(Integer plantingYear) {
        this.plantingYear = plantingYear;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FarmEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", totalHectares=" + totalHectares +
                ", cashewHectares=" + cashewHectares +
                ", treesPerHectare=" + treesPerHectare +
                ", plantingYear=" + plantingYear +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}