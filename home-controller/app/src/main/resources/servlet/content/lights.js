let status;

window.onload = function () {
    try {
        status = new Status('/rest/pwmLights/status', 750, function () {
            updateAll();
        }, getPwmLightComponents(), getBaseUrl());
        status.startRefresh();
        for (const item of status.componentMap.values()) {
            document.getElementById('lt_' + item.id).addEventListener("wheel", (function (event) {
                onWheel(event, item.id);
            }));
        }
    } catch (e) {
        printException(e);
    }
};

function updateAll() {
    for (const item of status.componentMap.values()) {
        updateValue(item);
    }
}

function onWheel(event, id) {
    event.preventDefault();
    event.stopPropagation();

    const item = status.componentMap.get(id);
    item.increaseValue(event.deltaY * -0.0003);
}

function updateValue(item) {
    const id = item.id;
    const title = document.getElementById('lt_' + id);
    title.innerHTML = `${item.name} ${Math.round(item.val * 100)}%`;
    const detail = document.getElementById('pd_' + id);
    detail.innerHTML = `${item.pwmVal}/${item.maxPwmVal} ${item.curr}A`;
    const main = document.getElementById('main_' + id);
    main.style.borderColor = (item.pwmVal > 0) ? 'red' : 'transparent';
    main.style.backgroundColor = `rgba(255, 0, 0, ${item.val})`;
}