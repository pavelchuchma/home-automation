let status;

window.onload = function () {
    try {
        status = new Status('/rest/sensors/status', 750, function () {
            updateAll();
        }, getSensorComponents(), getBaseUrl());
        status.startRefresh();
    } catch (e) {
        printException(e);
    }
};

function updateAll() {
    for (const item of status.componentMap.values()) {
        updateValue(item);
    }
}

function updateValue(item) {
    const active = document.getElementById('act_' + item.id);
    active.innerHTML = item.active ? '🔴' : '⚪';
    active.className = item.active ? 'active' : 'inactive';
    const date = document.getElementById('dt_' + item.id);
    date.innerHTML = toHHMMSS(item.age);
}

function toHHMMSS(sec_num) {
    if (sec_num === undefined || sec_num < 0) {
        return '?';
    }
    let hours = Math.floor(sec_num / 3600);
    let minutes = Math.floor((sec_num - (hours * 3600)) / 60);
    let seconds = sec_num - (hours * 3600) - (minutes * 60);

    if (hours < 10) {
        hours = "0" + hours;
    }
    if (minutes < 10) {
        minutes = "0" + minutes;
    }
    if (seconds < 10) {
        seconds = "0" + seconds;
    }
    return hours + ':' + minutes + ':' + seconds;
}