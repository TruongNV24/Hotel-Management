package com.cnj42.hotel;

import com.cnj42.hotel.ui.StayManagementPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StayManagementPanelTest {

    @Test
    void statusMappingsShouldMatchDisplayLabels() {
        assertEquals("CHỜ CHECK-IN", StayManagementPanel.getStatusLabel("PENDING"));
        assertEquals("ĐANG Ở", StayManagementPanel.getStatusLabel("CHECKED_IN"));
        assertEquals("CHỜ CHECK-OUT", StayManagementPanel.getStatusLabel("CHECKOUT_PENDING"));
        assertEquals("ĐÃ CHECK-OUT", StayManagementPanel.getStatusLabel("CHECKED_OUT"));
    }
}
