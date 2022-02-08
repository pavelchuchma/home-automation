let mainCtx;
let toolsCtx;
let currentFloor = 0;
let status;


const toolbarItemMap = new Map();
let selectedTool;

window.onload = function () {
    try {
        const toolbarItems = getToolbarItems();
        toolbarItems.forEach(function (item) {
            toolbarItemMap.set(item.id, item);
        });
        selectedTool = toolbarItems[0];

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
    for (const item of toolbarItemMap.values()) {
        const r = 35;

        toolsCtx.beginPath();
        toolsCtx.rect(item.x - r, item.y - r, 2 * r, 2 * r);
        toolsCtx.strokeStyle = (selectedTool === item) ? 'red' : 'lightgray';
        toolsCtx.lineWidth = 10;
        toolsCtx.stroke();
    }
}

function drawToolsCanvas() {
    toolsCtx = getCanvasContext('toolsCanvas')
    for (const item of toolbarItemMap.values()) {
        item.drawFunction(item.x, item.y, toolsCtx);
    }
    drawToolSelection();
}

function onToolsClick(event) {
    selectedTool = findNearestItem(event.offsetX, event.offsetY, toolbarItemMap.values());
    drawToolSelection();
}

function onCanvasClick(event) {
    // console.log(`new PwmLightItem('pwm', ${+Math.round(parseFloat(event.offsetX))}, ${Math.round(parseFloat(event.offsetY))}, ${currentFloor}),`);
    // return;

    const item = findNearestItem(event.offsetX, event.offsetY, status.componentMap.values(),
        item => selectedTool.isApplicable(item));

    if (item instanceof StairsItem) {
        currentFloor = item.targetFloor;
        drawMainCanvas();
        drawItems();
        return;
    }

    item.doAction(selectedTool.onClickAction);

    // draw changed item as gray
    mainCtx.beginPath();
    mainCtx.arc(item.x, item.y, 15, 0, 2 * Math.PI);
    mainCtx.fillStyle = 'gray';
    mainCtx.fill();
}

