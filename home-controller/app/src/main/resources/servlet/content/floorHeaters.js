'use strict';

let status;
let priceStatus;

/**
 * Holder of the /rest/eprice/status item for the chart of the dialog.
 */
class FloorHeaterPriceItem extends AbstractItem {
    updateToDataProperty() {
        return true;
    }
}

window.onload = function () {
    try {
        status = new Status('/rest/floorHeater/status', 750, function () {
            updateAll();
        }, getFloorHeaterComponents(), getBaseUrl());
        status.startRefresh();

        const priceItem = new FloorHeaterPriceItem('eprice');
        priceStatus = new Status('/rest/eprice/status', 60_000, function () {
            FLOOR_HEATER_PRICES.data = priceItem.updatedWithValidData ? priceItem.data : null;
        }, [priceItem], getBaseUrl());
        priceStatus.startRefresh();

        for (const item of status.componentMap.values()) {
            document.getElementById(item.id).addEventListener('click', () => {
                if (item.hasData()) {
                    SettingsSheet.open(floorHeaterSettings(item), item);
                }
            });
        }
    } catch (e) {
        printException(e);
    }
};

function updateAll() {
    for (const item of status.componentMap.values()) {
        document.getElementById('act_' + item.id).innerHTML = item.val ? '🔴' : '⚪';
        document.getElementById('sch_' + item.id).innerHTML = item.hasData() ? item.badgeText() : '';
        SettingsSheet.itemDrawn(item);
    }
}
