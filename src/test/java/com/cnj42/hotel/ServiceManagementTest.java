package com.cnj42.hotel;

import com.cnj42.hotel.model.Service;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.PermissionService;
import com.cnj42.hotel.service.ServiceService;
import com.cnj42.hotel.service.ServiceUsageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceManagementTest {

    @Test
    void serviceValidationRejectsBlankNameAndNonPositivePrice() {
        ServiceService serviceService = new ServiceService();
        Service service = new Service();
        service.setServiceName(" ");
        service.setPrice(100);
        assertFalse(serviceService.isValid(service));

        service.setServiceName("Laundry");
        service.setPrice(0);
        assertFalse(serviceService.isValid(service));
        service.setPrice(-1);
        assertFalse(serviceService.isValid(service));
    }

    @Test
    void serviceValidationAcceptsPositivePriceAndName() {
        Service service = new Service();
        service.setServiceName(" Laundry ");
        service.setPrice(50000);
        assertTrue(new ServiceService().isValid(service));
    }

    @Test
    void servicePermissionsMatchModule3Rules() {
        User admin = userWithRole("ADMIN");
        User manager = userWithRole("MANAGER");
        User employee = userWithRole("EMPLOYEE");

        assertFalse(PermissionService.canManageServices(admin));
        assertTrue(PermissionService.canManageServices(manager));
        assertFalse(PermissionService.canManageServices(employee));
        assertTrue(PermissionService.canRecordServiceUsage(employee));
    }

    @Test
    void usageTotalUsesHistoricalUnitPriceSnapshot() {
        assertEquals(150000, ServiceUsageService.calculateTotal(3, 50000));
        assertEquals(0, ServiceUsageService.calculateTotal(0, 50000));
        assertEquals(0, ServiceUsageService.calculateTotal(2, -1));
    }

    private User userWithRole(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
