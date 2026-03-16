package com.netcracker.cloud.dbaas.client.it.cassandra.service;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("service_film")
public class ServiceFilm {
    @PrimaryKey
    private UUID id;

    @Column
    private String title;

    @Column
    private int year;

    public ServiceFilm() {
    }

    public ServiceFilm(UUID id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}

