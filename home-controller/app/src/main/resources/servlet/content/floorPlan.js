let mainCtx;
let currentFloor = 0;
let status;
let toolbar;
let floorImages = [];

window.onload = function () {
    try {
        toolbar = new Toolbar('toolsCanvas');

        let imgIndex = 0;
        for (const imageSrc of getFloorImages()) {
            const img = new Image();
            floorImages.push(img);
            if (imgIndex++ === currentFloor) {
                img.onload = function () {
                    this.onLoadContinue();
                }.bind(this);
            }
            img.src = imageSrc;
        }
    } catch (e) {
        printException(e);
    }
};

function onLoadContinue() {
    try {
        const planCanvas = document.getElementById('planCanvas');
        planCanvas.width = floorImages[currentFloor].naturalWidth;
        planCanvas.height = floorImages[currentFloor].naturalHeight;
        planCanvas.addEventListener("click", function (event) {
            onPlanClick(event);
        });
        mainCtx = planCanvas.getContext("2d");
        drawPlanCanvas();

        const toolbarTable = document.getElementById('toolbarTable');
        const additionalToolbars = getAdditionalToolbars();
        for (const additionalToolbar of additionalToolbars) {
            let tr = document.createElement('tr');
            let td = document.createElement('td');
            toolbarTable.appendChild(tr).appendChild(td)
            if (additionalToolbar.type === 'canvas') {
                let canvas = document.createElement('canvas');
                canvas.id = additionalToolbar.canvasId;
                canvas.width = additionalToolbar.canvasWidth;
                canvas.height = additionalToolbar.canvasHeight;
                toolbarTable.appendChild(canvas);
            } else if (additionalToolbar.type === 'svg') {
                td.id = 'td-' + additionalToolbar.canvasId;
            }
            additionalToolbar.onCanvasCreated();
        }

        status = new Status('/rest/all/status', 750, function () {
            drawItems();
        }, getComponents().concat(additionalToolbars), getBaseUrl());
        status.startRefresh();

        initConfiguration();
    } catch (e) {
        printException(e);
    }
}

function drawItems() {
    drawPlanCanvas();
    for (const item of status.components) {
        if (item.floor === currentFloor || item.floor < 0) {
            try {
                item.draw(mainCtx);
            } catch (e) {
                printException(e);
            }
        }
    }
}

function drawPlanCanvas() {
    mainCtx.drawImage(floorImages[currentFloor], 0, 0);
}

function onPlanClick(event) {

    let selectedToolbar = toolbar.getCurrent();
    const item = findNearestItem(event.offsetX, event.offsetY, status.componentMap.values(),
        item => selectedToolbar.isApplicable(item));

    if (item instanceof StairsItem) {
        currentFloor = item.targetFloor;
        drawPlanCanvas();
        drawItems();
        return;
    }
    // console.log(`new PwmLightItem('pwm', ${+Math.round(parseFloat(event.offsetX))}, ${Math.round(parseFloat(event.offsetY))}, ${currentFloor}),`);
    // return;

    item.doAction(selectedToolbar.onClickAction);
    item.afterAction(mainCtx);
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
