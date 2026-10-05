package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.ReferenceDataDAO;
import com.cnj42.hotel.model.LookupOption;

import java.sql.SQLException;
import java.util.List;

public class ReferenceDataService {
    private final ReferenceDataDAO referenceDataDAO = new ReferenceDataDAO();

    public List<LookupOption> findOptions(String type) {
        try {
            return referenceDataDAO.findOptions(type);
        } catch (SQLException e) {
            System.err.println("Lỗi tải dữ liệu tham chiếu " + type + ": " + e.getMessage());
            return List.of();
        }
    }
}