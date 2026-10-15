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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceImpl.class);

    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository diningTableRepository;
    private final OutletRepository outletRepository;

    public CatalogServiceImpl(MenuItemRepository menuItemRepository,
                              DiningTableRepository diningTableRepository,
                              OutletRepository outletRepository) {
        this.menuItemRepository = menuItemRepository;
        this.diningTableRepository = diningTableRepository;
        this.outletRepository = outletRepository;
    }

    @Override
    public List<MenuItemResponse> getActiveMenu(Long outletId) {
        log.info("Fetching active menu items for outletId={}", outletId);
        ensureOutletExists(outletId);

        List<MenuItem> items = menuItemRepository.findByOutletIdAndAvailableTrue(outletId);
        return items.stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    @Override
    public TableContextResponse getTableContext(Long outletId, String tableNumber) {
        log.info("Validating table context for outletId={} tableNumber={}", outletId, tableNumber);
        Outlet outlet = ensureOutletExists(outletId);

        DiningTable table = diningTableRepository.findByOutletIdAndTableNumber(outletId, tableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Table '%s' not found in outlet %d".formatted(tableNumber, outletId)));

        return TableContextResponse.from(outlet, table);
    }

    private Outlet ensureOutletExists(Long outletId) {
        return outletRepository.findById(outletId)
                .orElseThrow(() -> new ResourceNotFoundException("Outlet not found with id: " + outletId));
    }
}
