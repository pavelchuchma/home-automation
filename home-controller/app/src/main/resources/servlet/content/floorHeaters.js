let status;

window.onload = function () {
    try {
        status = new Status('/rest/onOff/status', 750, function () {
            updateAll();
        }, getFloorHeaterComponents(), getBaseUrl());
        status.startRefresh();
    } catch (e) {
        printException(e);
    }
};

function updateAll() {
    for (const item of status.componentMap.values()) {
        const state = document.getElementById('act_' + item.id);
        state.innerHTML = item.val ? '🔴' : '⚪';
    }
}
