package com.qrorder.catalog.application;

import com.qrorder.catalog.api.dto.MenuItemResponse;
import com.qrorder.catalog.api.dto.TableContextResponse;
import com.qrorder.catalog.domain.DiningTable;
import com.qrorder.catalog.domain.MenuItem;
import com.qrorder.catalog.infrastructure.DiningTableRepository;
import com.qrorder.catalog.infrastructure.MenuItemRepository;
import com.qrorder.outlet.domain.Outlet;
import com.qrorder.outlet.infrastructure.OutletRepository;
import com.qrorder.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private OutletRepository outletRepository;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private Outlet outlet;

    @BeforeEach
    void setUp() {
        outlet = new Outlet("Outlet Utama", "Jl. Sudirman 10", "Asia/Jakarta");
        outlet.setId(1L);
        outlet.setTaxRate(1100);
        outlet.setServiceChargeRate(500);
        outlet.setOpen(true);
    }

    @Test
    @DisplayName("getActiveMenu: Successfully returns active items for existing outlet")
    void getActiveMenuSuccess() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet));

        MenuItem item1 = new MenuItem(outlet, "Mie Pedas", "Makanan", 15_000L);
        MenuItem item2 = new MenuItem(outlet, "Es Teh", "Minuman", 5_000L);
        when(menuItemRepository.findByOutletIdAndAvailableTrue(1L)).thenReturn(List.of(item1, item2));

        List<MenuItemResponse> result = catalogService.getActiveMenu(1L);

        assertEquals(2, result.size());
        assertEquals("Mie Pedas", result.get(0).name());
        assertEquals(15_000L, result.get(0).price());
        assertEquals("Es Teh", result.get(1).name());
    }

    @Test
    @DisplayName("getActiveMenu: Throws ResourceNotFoundException if outlet does not exist")
    void getActiveMenuOutletNotFound() {
        when(outletRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> catalogService.getActiveMenu(99L));
        verify(menuItemRepository, never()).findByOutletIdAndAvailableTrue(anyLong());
    }

    @Test
    @DisplayName("getTableContext: Successfully returns table and outlet context")
    void getTableContextSuccess() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet));

        DiningTable table = new DiningTable(outlet, "14");
        when(diningTableRepository.findByOutletIdAndTableNumber(1L, "14")).thenReturn(Optional.of(table));

        TableContextResponse response = catalogService.getTableContext(1L, "14");

        assertNotNull(response);
        assertEquals(1L, response.outletId());
        assertEquals("Outlet Utama", response.outletName());
        assertEquals("14", response.tableNumber());
        assertTrue(response.outletIsOpen());
        assertTrue(response.tableIsActive());
        assertEquals(1100, response.taxRate());
    }

    @Test
    @DisplayName("getTableContext: Throws ResourceNotFoundException if table number does not exist")
    void getTableContextTableNotFound() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet));
        when(diningTableRepository.findByOutletIdAndTableNumber(1L, "99")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> catalogService.getTableContext(1L, "99"));
    }
}
