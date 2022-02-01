let mainCtx;
let toolsCtx;
let hvacCtx;
let pumpCtx;

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

class ToolBarItem extends BaseItem {
    constructor(id, x, y, drawFunction, applicableOn, onItemClickHandler) {
        super(id, x, y, -1)
        this.drawFunction = drawFunction;
        this.applicableOn = applicableOn;
        this.onItemClickHandler = onItemClickHandler;
    }

    isApplicable(item) {
        return this.applicableOn.includes(item.constructor.name);
    }
}

function handleLightClick(itemStatus, toolbar) {
    const value = getNewLightValue(itemStatus, toolbar);
    return '/rest/pwmLights/action?id=' + itemStatus.id + "&" + "val=" + value;
}

const toolsCoordinates = [
    new ToolBarItem('lightToggle', 50, 50, function (x, y, ctx) {
        PwmLightItem.drawIcon(x - 10, y, 0, ctx);
        PwmLightItem.drawIcon(x + 10, y, .75, ctx);
    }, [PwmLightItem.name], handleLightClick),

    new ToolBarItem('lightPlus', 50, 150, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, TOOL_LIGHT_PLUS_VALUE / 100, ctx);
        drawLightToolSign(x, y, ctx, true);
    }, [PwmLightItem.name], handleLightClick),

    new ToolBarItem('lightMinus', 50, 250, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, .25, ctx);
        drawLightToolSign(x, y, ctx, false);
    }, [PwmLightItem.name], handleLightClick),

    new ToolBarItem('lightFull', 50, 350, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, 1, ctx);
    }, [PwmLightItem.name], handleLightClick),

    new ToolBarItem('lightOff', 50, 450, function (x, y, ctx) {
        PwmLightItem.drawIcon(x, y, 0, ctx);
    }, [PwmLightItem.name], handleLightClick),

    new ToolBarItem('louversUp', 50, 550, function (x, y, ctx) {
        LouversItem.drawIcon(x, y, .3, 0, 'stopped', ctx, 50, 60)
    }, [LouversItem.name], function (itemStatus) {
        return buildLouversActionLink(itemStatus, 0, 0);
    }),

    new ToolBarItem('louversOutshine', 50, 650, function (x, y, ctx) {
        LouversItem.drawIcon(x, y, 1, 0, 'stopped', ctx, 50, 60);
    }, [LouversItem.name], function (itemStatus) {
        return buildLouversActionLink(itemStatus, 100, 0);
    }),

    new ToolBarItem('louversDown', 50, 750, function (x, y, ctx) {
        LouversItem.drawIcon(x, y, 1, 1, 'stopped', ctx, 50, 60);
    }, [LouversItem.name], function (itemStatus) {
        return buildLouversActionLink(itemStatus, 100, 100);
    }),

    new ToolBarItem('valveToggle', 50, 850, function (x, y, ctx) {
        AirValveItem.drawIcon(x + 10, y - 5, 1, 'stopped', ctx);
        AirValveItem.drawIcon(x - 10, y + 5, 0, 'stopped', ctx);
    }, [AirValveItem.name], function (itemStatus) {
        const valveVal = (AirValveItem.getValveState(itemStatus.act, itemStatus.pos) === 0) ? 100 : 0;
        return '/rest/airValves/action?id=' + itemStatus.id + "&" + "val=" + valveVal;
    })
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
        drawHvacCanvas();
        drawPumpCanvas();

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
        if (filter(item) && item.floor === currentFloor || item.floor < 0) {
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
            if (item instanceof HvacItem) {
                item.draw(hvacCtx);
            } else if (item instanceof WaterPumpItem) {
                item.draw(pumpCtx);
            } else {
                item.draw(mainCtx);
            }
        }
    }
}

function drawMainCanvas() {
    const c = document.getElementById("mainCanvas");
    mainCtx = c.getContext("2d");
    const img = document.getElementById(getFloorIds()[currentFloor]);
    mainCtx.drawImage(img, 0, 0, img.width, img.height);

    //document.getElementById('error').innerHTML = 'LOADED!';
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
    const c = document.getElementById("toolsCanvas");
    toolsCtx = c.getContext("2d");

    toolsCtx.rect(0, 0, 100, toolsCoordinates.length * 100);
    toolsCtx.fillStyle = TOOLBOX_BACKGROUND;
    toolsCtx.fill();
    toolsCtx.stroke();

    toolsCoordinates.forEach(function (c) {
        c.drawFunction(c.x, c.y, toolsCtx);
    });

    drawToolSelection();
}

function drawHvacCanvas() {
    const c = document.getElementById("hvacCanvas");
    hvacCtx = c.getContext("2d");

    hvacCtx.rect(0, 0, 100, 150);
    hvacCtx.fillStyle = TOOLBOX_BACKGROUND;
    hvacCtx.fill();
    hvacCtx.stroke();
}

function onHvacClick() {
    const hvacStatus = status.componentMap.get('hvac');
    if (hvacStatus.on) {
        status.sendAction('/rest/hvac/action?id=hvac&on=false');
    } else {
        status.sendAction('/rest/hvac/action?id=hvac&on=true');
    }
}

function drawPumpCanvas() {
    const c = document.getElementById("pumpCanvas");
    pumpCtx = c.getContext("2d");

    pumpCtx.rect(0, 0, 100, 50);
    pumpCtx.fillStyle = TOOLBOX_BACKGROUND;
    pumpCtx.fill();
    pumpCtx.stroke();
}

function onToolsClick(event) {
    selectedTool = findNearestItem(event.offsetX, event.offsetY, toolsCoordinates, function () {
        return true
    });
    drawToolSelection();
}

function getNewLightValue(lightStatus, toolbar) {
    const step = 15;
    const val = parseInt(lightStatus.val);
    const maxVal = parseInt(lightStatus.maxVal);
    const vPerc = Math.round(val / maxVal * 100);
    switch (toolbar.id) {
        case 'lightToggle':
            return (vPerc === 0) ? 75 : 0;
        case 'lightPlus':
            return (vPerc === 0) ? TOOL_LIGHT_PLUS_VALUE : Math.min(100, vPerc + step);
        case 'lightMinus':
            return (vPerc === 0) ? 1 : Math.max(0, vPerc - step);
        case 'lightFull' :
            return 100;
        case 'lightOff':
            return 0;
    }
    return val;
}

function buildLouversActionLink(itemStatus, position, offset) {
    return '/rest/louvers/action?id=' + itemStatus.id + '&pos=' + position + '&off=' + offset;
}

function onCanvasClick(event) {
    const item = findNearestItem(event.offsetX, event.offsetY, status.componentMap.values(), function (item) {
        return selectedTool.isApplicable(item) || item instanceof StairsItem;
    });

    if (item instanceof StairsItem) {
        currentFloor = item.targetFloor;
        drawMainCanvas();
        drawItems();
        return;
    }

    // console.log(['pwm', " + Math.round(parseFloat(event.offsetX)) + ", " + Math.round(parseFloat(event.offsetY)) + ", currentFloor])
    // return;

    const action = selectedTool.onItemClickHandler(item, selectedTool);
    if (action) {
        status.sendAction(action);
    }

    // draw changed light as gray
    mainCtx.beginPath();
    mainCtx.arc(item.x, item.y, 15, 0, 2 * Math.PI);
    mainCtx.fillStyle = 'gray';
    mainCtx.fill();
}

