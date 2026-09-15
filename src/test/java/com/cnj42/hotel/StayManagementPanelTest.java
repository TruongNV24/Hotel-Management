package com.cnj42.hotel;

import com.cnj42.hotel.ui.StayManagementPanel;
import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StayManagementPanelTest {

    @Test
    void statusMappingsShouldMatchDisplayLabels() {
        assertEquals("CHỜ CHECK-IN", StayManagementPanel.getStatusLabel("PENDING"));
        assertEquals("ĐANG Ở", StayManagementPanel.getStatusLabel("CHECKED_IN"));
        assertEquals("CHỜ CHECK-OUT", StayManagementPanel.getStatusLabel("CHECKOUT_PENDING"));
        assertEquals("ĐÃ CHECK-OUT", StayManagementPanel.getStatusLabel("CHECKED_OUT"));
    }

    @Test
    void stayManagementPanelShouldExposeGuestAndReservationCreationHooks() {
        Method guestCreateHook = getDeclaredMethod("openCreateGuestDialog");
        Method reservationCreateHook = getDeclaredMethod("openCreateReservationDialog");

        assertNotNull(guestCreateHook);
        assertNotNull(reservationCreateHook);
    }

    @Test
    void actionButtonFactoryShouldCreateStableAndFilledButtons() throws Exception {
        Method createActionButton = StayManagementPanel.class.getDeclaredMethod(
                "createActionButton",
                String.class,
                Color.class,
                Color.class,
                Color.class
        );
        createActionButton.setAccessible(true);

        JButton button = (JButton) createActionButton.invoke(null,
                "Check-in",
                new Color(224, 244, 234),
                new Color(58, 176, 116),
                new Color(58, 176, 116)
        );

        assertTrue(button.isContentAreaFilled());
        assertEquals(new Dimension(90, 34), button.getPreferredSize());
    }

    private Method getDeclaredMethod(String name) {
        try {
            return StayManagementPanel.class.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
