let mainCtx;
let toolsCtx;

let currentFloor = 0;

const TOOLBOX_BACKGROUND = 'lightgray';
const TOOL_LIGHT_PLUS_VALUE = 66;

let status;

function drawLightToolSign(x, y, ctx, drawVertical) {
    ctx.beginPath();
    const r = 15;
    ctx.moveTo(x - r, y);
    ctx.lineTo(x + r, y);
    if (drawVertical) {
        ctx.moveTo(x, y - r);
        ctx.lineTo(x, y + r);
    }

    ctx.strokeStyle = 'red';
    ctx.lineWidth = 7;
    ctx.stroke();
}

const toolsCoordinates = [
    new ToolBarItem('lightToggle', 50, 50, function (x, y, ctx) {
        PwmLightItem.drawIcon(x - 10, y, 0, ctx);
        PwmLightItem.drawIcon(x + 10, y, .75, ctx);
    }, [PwmLightItem.name, StairsItem.name], 'toggle'),

    new ToolBarItem('lightPlus', 50, 150, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, TOOL_LIGHT_PLUS_VALUE / 100, ctx);
        drawLightToolSign(x, y, ctx, true);
    }, [PwmLightItem.name, StairsItem.name], 'plus'),

    new ToolBarItem('lightMinus', 50, 250, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, .25, ctx);
        drawLightToolSign(x, y, ctx, false);
    }, [PwmLightItem.name, StairsItem.name], 'minus'),

    new ToolBarItem('lightFull', 50, 350, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, 1, ctx);
    }, [PwmLightItem.name, StairsItem.name], 'full'),

    new ToolBarItem('lightOff', 50, 450, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, 0, ctx);
    }, [PwmLightItem.name, StairsItem.name], 'off'),

    new ToolBarItem('louversUp', 50, 550, function (x, y, ctx) {
        LouversItem.drawIcon(x, y, .3, 0, 'stopped', ctx, 50, 60)
    }, [LouversItem.name, StairsItem.name], 'up'),

    new ToolBarItem('louversOutshine', 50, 650, function (x, y, ctx) {
        LouversItem.drawIcon(x, y, 1, 0, 'stopped', ctx, 50, 60);
    }, [LouversItem.name, StairsItem.name], 'outshine'),

    new ToolBarItem('louversDown', 50, 750, function (x, y, ctx) {
        LouversItem.drawIcon(x, y, 1, 1, 'stopped', ctx, 50, 60);
    }, [LouversItem.name, StairsItem.name], 'down'),

    new ToolBarItem('valveToggle', 50, 850, function (x, y, ctx) {
        AirValveItem.drawIcon(x + 10, y - 5, 1, 'stopped', 'red', ctx);
        AirValveItem.drawIcon(x - 10, y + 5, 0, 'stopped', 'green', ctx);
    }, [AirValveItem.name, StairsItem.name], 'toggle')
];

const toolCoordinateMap = {};
let selectedTool = toolsCoordinates[0];

window.onload = function () {
    try {
        toolsCoordinates.forEach(function (tc) {
            toolCoordinateMap[tc.id] = tc;
        });

        drawMainCanvas();
        drawToolsCanvas();

        status = new Status('/rest/all/status', 750, function () {
            drawItems();
        }, getComponents(), getBaseUrl());
    } catch (e) {
        printException(e);
    }
};

function computeDistance(x1, y1, x2, y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

function findNearestItem(x, y, items, filter) {
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
    toolsCoordinates.forEach(function (c) {
        const r = 35;

        toolsCtx.beginPath();
        toolsCtx.rect(c.x - r, c.y - r, 2 * r, 2 * r);
        toolsCtx.strokeStyle = (selectedTool === c) ? 'red' : TOOLBOX_BACKGROUND;
        toolsCtx.lineWidth = 10;
        toolsCtx.stroke();
    });
}

function drawToolsCanvas() {
    toolsCtx = getCanvasContext('toolsCanvas')
    toolsCoordinates.forEach(function (c) {
        c.drawFunction(c.x, c.y, toolsCtx);
    });

    drawToolSelection();
}

function onToolsClick(event) {
    selectedTool = findNearestItem(event.offsetX, event.offsetY, toolsCoordinates);
    drawToolSelection();
}

function onCanvasClick(event) {
    // console.log(['pwm', " + Math.round(parseFloat(event.offsetX)) + ", " + Math.round(parseFloat(event.offsetY)) + ", currentFloor])
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

