let mainCtx;
let toolsCtx;
let currentFloor = 0;
let status;

const toolbarItems = getToolbarItems();
let selectedToolIndex = 0;

window.onload = function () {
    try {
        drawMainCanvas();
        drawToolsCanvas();

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

function drawToolSelection() {
    const itemHeight = toolsCtx.canvas.height / toolbarItems.length;
    const itemWidth = toolsCtx.canvas.width;
    for (let i = 0; i < toolbarItems.length; i++) {
        const r = 35;

        toolsCtx.beginPath();
        toolsCtx.rect(itemWidth / 2 - r, (itemHeight * (i + 0.5)) - r, 2 * r, 2 * r);
        toolsCtx.strokeStyle = (i === selectedToolIndex) ? 'red' : 'lightgray';
        toolsCtx.lineWidth = 10;
        toolsCtx.stroke();
    }
}

function drawToolsCanvas() {
    toolsCtx = getCanvasContext('toolsCanvas');
    const itemHeight = toolsCtx.canvas.height / toolbarItems.length;
    for (let i = 0; i < toolbarItems.length; i++) {
        toolbarItems[i].drawFunction(toolsCtx.canvas.width / 2, itemHeight * (i + 0.5), toolsCtx);
    }
    drawToolSelection();
}

function onToolsClick(event) {
    const itemHeight = toolsCtx.canvas.height / toolbarItems.length;
    selectedToolIndex = Math.floor(parseFloat(event.offsetY) / itemHeight);
    drawToolSelection();
}

function onCanvasClick(event) {
    // console.log(`new PwmLightItem('pwm', ${+Math.round(parseFloat(event.offsetX))}, ${Math.round(parseFloat(event.offsetY))}, ${currentFloor}),`);
    // return;

    const item = findNearestItem(event.offsetX, event.offsetY, status.componentMap.values(),
        item => toolbarItems[selectedToolIndex].isApplicable(item));

    if (item instanceof StairsItem) {
        currentFloor = item.targetFloor;
        drawMainCanvas();
        drawItems();
        return;
    }

    item.doAction(toolbarItems[selectedToolIndex].onClickAction);

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

