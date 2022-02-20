let status;

window.onload = function () {
    try {
        status = new Status('/rest/louvers/status', 750, function () {
            drawItems();
        }, getLouversComponents(), getBaseUrl());
        status.startRefresh();
    } catch (e) {
        printException(e);
    }
};

function handleClick(id, action) {
    const item = status.componentMap.get(id);
    item.doAction(action);
}

function drawArrow(item, icon, endIcon, endPosition, idSuffix, movingAction) {
    const element = document.getElementById('act_' + item.id + '_' + idSuffix);
    let clazz = 'louversArrow';
    if (item.act === movingAction) clazz += ' louversArrow-moving';
    element.innerHTML = (item.pos === endPosition) ? endIcon : icon;
    element.className = clazz;
}

function drawItems() {
    for (const item of status.componentMap.values()) {
        drawArrow(item, '△', '▲', 0, 'up', 'movingUp');
        drawArrow(item, '▽', '▼', 1, 'blind', 'movingDown');
    }
}