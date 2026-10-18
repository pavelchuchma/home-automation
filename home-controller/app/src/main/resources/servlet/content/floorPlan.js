let planSvg;
let planImage;
let planItems;
let currentFloor = 0;
let status;
let toolbar;
let floorImages = [];

window.onload = function () {
    try {
        toolbar = new Toolbar('toolsContainer');
        toolbar.createMenuIcon('/nodes');

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
        const floorImage = floorImages[currentFloor];
        // fits next to the toolbar in narrow windows, never above the natural image size
        planSvg = SVG().addTo('#planContainer').viewbox(0, 0, floorImage.naturalWidth, floorImage.naturalHeight)
            .css('width', `min(${floorImage.naturalWidth}px, calc(100vw - 150px))`);
        planSvg.attr('id', 'floorPlanSvg');
        planSvg.on('click', event => onPlanClick(event));
        planImage = planSvg.image(floorImage.src).size(floorImage.naturalWidth, floorImage.naturalHeight);
        planItems = planSvg.group();

        const toolbarTable = document.getElementById('toolbarTable');
        const additionalToolbars = getAdditionalToolbars();
        for (const additionalToolbar of additionalToolbars) {
            let tr = document.createElement('tr');
            let td = document.createElement('td');
            toolbarTable.appendChild(tr).appendChild(td)
            td.id = 'td-' + additionalToolbar.canvasId;
            additionalToolbar.onCanvasCreated();
        }

        status = new Status('/rest/all/status', 750, function () {
            drawItems();
        }, getComponents().concat(additionalToolbars), getBaseUrl());
        status.startRefresh();
    } catch (e) {
        printException(e);
    }
}

function drawItems() {
    planItems.clear();
    for (const item of status.components) {
        if (item.floor === currentFloor || item.floor < 0) {
            try {
                item.draw(planItems);
            } catch (e) {
                printException(e);
            }
        }
    }
}

function onPlanClick(event) {
    // plan coordinates of the click, independent of which SVG element was hit
    const point = planSvg.point(event.clientX, event.clientY);

    let selectedToolbar = toolbar.getCurrent();
    const item = findNearestItem(point.x, point.y, status.componentMap.values(),
        item => selectedToolbar.isApplicable(item));

    if (item instanceof StairsItem) {
        currentFloor = item.targetFloor;
        planImage.load(floorImages[currentFloor].src);
        drawItems();
        return;
    }
    // console.log(`new LouversItem('lv', ${Math.round(point.x)}, ${Math.round(point.y)}, ${currentFloor}),`);
    // return;

    item.doAction(selectedToolbar.onClickAction);
    item.afterAction(planItems);
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
