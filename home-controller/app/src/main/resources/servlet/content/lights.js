let mainCtx;
let currentFloor = 0;
let status;
let toolbar;

window.onload = function () {
    try {
        drawMainCanvas();
        toolbar = new Toolbar();
        status = new Status('/rest/all/status', 750, function () {
            drawItems();
        }, getComponents(), getBaseUrl());
    } catch (e) {
        printException(e);
    }
};

function drawItems() {
    for (const item of status.componentMap.values()) {
        if (item.floor === currentFloor || item.floor < 0) {
            item.draw(mainCtx);
        }
    }
}

function drawMainCanvas() {
    const c = document.getElementById("mainCanvas");
    mainCtx = c.getContext("2d");
    const img = document.getElementById(getFloorIds()[currentFloor]);
    mainCtx.drawImage(img, 0, 0, img.width, img.height);
}

function onCanvasClick(event) {
    // console.log(`new PwmLightItem('pwm', ${+Math.round(parseFloat(event.offsetX))}, ${Math.round(parseFloat(event.offsetY))}, ${currentFloor}),`);
    // return;

    let selectedToolbar = toolbar.getCurrent();
    const item = findNearestItem(event.offsetX, event.offsetY, status.componentMap.values(),
        item => selectedToolbar.isApplicable(item));

    if (item instanceof StairsItem) {
        currentFloor = item.targetFloor;
        drawMainCanvas();
        drawItems();
        return;
    }

    item.doAction(selectedToolbar.onClickAction);

    // draw changed item as gray
    mainCtx.beginPath();
    mainCtx.arc(item.x, item.y, 15, 0, 2 * Math.PI);
    mainCtx.fillStyle = 'gray';
    mainCtx.fill();
}

function findNearestItem(x, y, items, filter) {
    function computeDistance(x1, y1, x2, y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    let result;
    let resDist = Number.MAX_SAFE_INTEGER;
    for (const item of items) {
        if ((item.floor === currentFloor || item.floor < 0) && (filter === undefined || filter(item))) {
            const dist = computeDistance(x, y, item.x, item.y);
            if (dist < resDist) {
                resDist = dist;
                result = item;
            }
        }
    }
    return result;
}
