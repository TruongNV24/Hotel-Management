package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.DatabaseBootstrapDAO;

public class DatabaseBootstrapService {
    private final DatabaseBootstrapDAO databaseBootstrapDAO = new DatabaseBootstrapDAO();

    public void ensureReady() {
        databaseBootstrapDAO.ensureReady();
    }
}