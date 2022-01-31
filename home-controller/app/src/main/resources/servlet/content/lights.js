let mainCtx;
let toolsCtx;
let hvacCtx;

let currentFloor = 0;
const floorIds = ['1stFloor', '2ndFloor'];

const TOOLBOX_BACKGROUND = 'lightgray';
const TOOL_LIGHT_PLUS_VALUE = 66;

const status = new Status('/rest/all/status', 750, function () {
    drawItems();
});

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

class ToolBarItem extends CoordinateItem {
    constructor(id, x, y, drawFunction, applicableOn, onItemClickHandler) {
        super(id, x, y, -1, 'toolbar')
        this.drawFunction = drawFunction;
        this.applicableOn = applicableOn;
        this.onItemClickHandler = onItemClickHandler;
    }

    isApplicable(item) {
        return item.type !== undefined && this.applicableOn.includes(item.type);
    }
}

function handleLightClick(itemStatus, toolbar) {
    const value = getNewLightValue(itemStatus, toolbar);
    return '/rest/pwmLights/action?id=' + itemStatus.id + "&" + "val=" + value;
}

const toolsCoordinates = [
        new ToolBarItem('lightToggle', 50, 50, function (x, y, ctx) {
            drawLightIcon(x - 10, y, 0, ctx);
            drawLightIcon(x + 10, y, .75, ctx);
        }, ['pwmLight'], handleLightClick),

        new ToolBarItem('lightPlus', 50, 150, function (x, y, ctx) {
            drawLightIcon(x, y, TOOL_LIGHT_PLUS_VALUE / 100, ctx);
            drawLightToolSign(x, y, ctx, true);
        }, ['pwmLight'], handleLightClick),

        new ToolBarItem('lightMinus', 50, 250, function (x, y, ctx) {
            drawLightIcon(x, y, .25, ctx);
            drawLightToolSign(x, y, ctx, false);
        }, ['pwmLight'], handleLightClick),

        new ToolBarItem('lightFull', 50, 350, function (x, y, ctx) {
            drawLightIcon(x, y, 1, ctx);
        }, ['pwmLight'], handleLightClick),

        new ToolBarItem('lightOff', 50, 450, function (x, y, ctx) {
            drawLightIcon(x, y, 0, ctx);
        }, ['pwmLight'], handleLightClick),

        new ToolBarItem('louversUp', 50, 550, function (x, y, ctx) {
            drawLouversToolIcon(x, y, .3, 0, 'stopped', ctx);
        }, ['louvers'], function (itemStatus) {
            return buildLouversActionLink(itemStatus, 0, 0);
        }),

        new ToolBarItem('louversOutshine', 50, 650, function (x, y, ctx) {
            drawLouversToolIcon(x, y, 1, 0, 'stopped', ctx);
        }, ['louvers'], function (itemStatus) {
            return buildLouversActionLink(itemStatus, 100, 0);
        }),

        new ToolBarItem('louversDown', 50, 750, function (x, y, ctx) {
            drawLouversToolIcon(x, y, 1, 1, 'stopped', ctx);
        }, ['louvers'], function (itemStatus) {
            return buildLouversActionLink(itemStatus, 100, 100);
        }),

        new ToolBarItem('valveToggle', 50, 850, function (x, y, ctx) {
            drawValveIcon(x + 10, y - 5, 1, 'stopped', ctx);
            drawValveIcon(x - 10, y + 5, 0, 'stopped', ctx);
        }, ['airValve'], function (itemStatus) {
            const valveVal = (getValveState(itemStatus.act, itemStatus.pos) === 0) ? 100 : 0;
            return '/rest/airValves/action?id=' + itemStatus.id + "&" + "val=" + valveVal;
        })
    ]
;


function drawLouversToolIcon(x, y, position, offset, action, ctx) {
    const w = 50;
    const h = 60;
    drawLouversIconImpl(x, y, position, offset, action, ctx, w, h);
}

function drawLouversIcon(x, y, position, offset, action, ctx) {
    const w = 70;
    const h = 80;
    drawLouversIconImpl(x, y, position, offset, action, ctx, w, h);
}

function drawLouversIconImpl(x, y, position, offset, action, ctx, w, h) {
    const louverHeight = 7;
    // white rectangle
    ctx.beginPath();
    ctx.rect(x - w / 2, y - h / 2, w, h);
    ctx.fillStyle = 'white';
    ctx.fill();
    ctx.strokeStyle = 'black';
    ctx.lineWidth = 2;
    ctx.stroke();

    if (position >= 0) {
        // louvers background box
        ctx.beginPath();
        ctx.rect(x - w / 2, y - h / 2, w, h * position);
        ctx.fillStyle = 'lightgray';
        ctx.fill();

        ctx.strokeStyle = 'black';
        ctx.lineWidth = 2;
        ctx.stroke();

        // louvers
        const lineWidth = louverHeight * offset;
        ctx.beginPath();
        for (let i = h * position - lineWidth / 2; i >= lineWidth / 2; i -= louverHeight) {
            const yy = y - h / 2 + i;
            ctx.moveTo(x - w / 2, yy);
            ctx.lineTo(x + w / 2, yy);
        }
        ctx.strokeStyle = 'black';
        ctx.lineWidth = lineWidth;
        ctx.stroke();
    } else {
        ctx.beginPath();
        ctx.font = h + "px Arial Bold";
        ctx.fillStyle = 'black';
        ctx.textAlign = "center";
        ctx.textBaseline = 'middle';
        ctx.fillText("?", x, y);
        ctx.stroke();
    }
}

function drawCharacterIcon(item, text) {
    const w = 70;
    const h = 80;

    // white rectangle
    mainCtx.beginPath();
    mainCtx.rect(item.x - w / 2, item.y - h / 2, w, h);
    mainCtx.fillStyle = 'white';
    mainCtx.fill();
    mainCtx.strokeStyle = 'black';
    mainCtx.lineWidth = 2;
    mainCtx.stroke();

    mainCtx.beginPath();
    mainCtx.font = h - 20 + "px Arial Bold";
    mainCtx.fillStyle = 'black';
    mainCtx.textAlign = "center";
    mainCtx.textBaseline = 'middle';
    mainCtx.fillText(text, item.x, item.y);
    mainCtx.stroke();
}

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
            switch (item.type) {
                case 'airValve':
                    drawValveIcon(item.x, item.y, item.pos, item.act, mainCtx);
                    break;
                case 'louvers':
                    drawLouversIcon(item.x, item.y, item.pos, item.off, item.act, mainCtx);
                    break;
                case 'pwmLight':
                    const power = item.val / item.maxVal;
                    drawLightIcon(item.x, item.y, power, mainCtx);
                    break;
                case 'pir':
                    drawPirIcon(item.x, item.y, item.age, mainCtx);
                    break;
                case 'hvac':
                    drawHvacScreen();
                    break;
                case 'wpump':
                    drawPumpScreen();
                    break;
                case 'stairs':
                    drawStairsIcon(item);
                    break;
            }
        }
    }
}

function drawStairsIcon(item) {
    if (item.id === 'stairsUp') {
        drawCharacterIcon(item, '▲')
    } else if (item.id === 'stairsDown') {
        drawCharacterIcon(item, '▼')
    }
}

function drawMainCanvas() {
    const c = document.getElementById("mainCanvas");
    mainCtx = c.getContext("2d");
    const img = document.getElementById(floorIds[currentFloor]);
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

function drawHvacScreen() {
    hvacCtx.rect(0, 0, 100, 150);
    hvacCtx.fillStyle = TOOLBOX_BACKGROUND;
    hvacCtx.fill();
    hvacCtx.stroke();

    const hvacStatus = status.componentMap.get('hvac');

    if (hvacStatus.on) {
        hvacCtx.font = "bold 15px Arial";
        hvacCtx.fontWeight = "500";
        hvacCtx.fillStyle = 'red';
        hvacCtx.fillText(hvacStatus.targetMode, 5, 20);
        hvacCtx.fillText(hvacStatus.fanSpeed, 5, 40);
        hvacCtx.fillText('Tgt temp: ' + hvacStatus.targetTemperature, 5, 60);
        hvacCtx.font = "13px Arial";
        let y = 70;
        const step = 17;
        hvacCtx.fillText('Air temp: ' + hvacStatus.airTemperature, 5, y += step);
        hvacCtx.fillText('Room temp: ' + hvacStatus.roomTemperature, 5, y += step);
        hvacCtx.fillText('Unit temp: ' + hvacStatus.unitTemperature, 5, y + step);
        if (hvacStatus.defrost) {
            hvacCtx.font = "bold 15px Arial";
            hvacCtx.fillText('Defrost!', 5, step + 5);
        }
    } else {
        hvacCtx.fillStyle = 'black';
        hvacCtx.font = "30px Arial";
        hvacCtx.fillText('OFF', 15, 30);
    }
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

function drawPumpScreen() {
    // clr
    pumpCtx.rect(0, 0, 100, 150);
    pumpCtx.fillStyle = TOOLBOX_BACKGROUND;
    pumpCtx.fill();
    pumpCtx.stroke();

    const pumpStatus = status.componentMap.get('wpump');
    pumpCtx.fillStyle = 'black';
    pumpCtx.font = "12px Arial";
    pumpCtx.fillText('Cykly: ' + pumpStatus.lastPeriodRecCount + '/' + pumpStatus.recCount, 5, 20);
    let lastRecordDuration = -1;
    if (pumpStatus.lastRecords.length > 0) {
        lastRecordDuration = pumpStatus.lastRecords[pumpStatus.lastRecords.length - 1].duration;
    }
    pumpCtx.fillText('Poslední: ' + Math.round(lastRecordDuration * 10.0) / 10.0 + ' s', 5, 35);
}

function drawLightIcon(x, y, power, ctx) {
    // black background
    ctx.strokeStyle = 'black';
    ctx.lineWidth = 1;
    if (power < 1) {
        ctx.beginPath();
        ctx.arc(x, y, 20, 0, 2 * Math.PI);
        ctx.fillStyle = 'black';
        ctx.fill();
    }

    // yellow pie
    ctx.beginPath();
    if (power < 1) {
        ctx.moveTo(x, y);
    }
    const startAngle = 1.5 * Math.PI;
    const endAngle = (1.5 + 2 * power) * Math.PI;
    ctx.arc(x, y, 20, startAngle, endAngle);
    if (power < 1) {
        ctx.lineTo(x, y);
    }
    ctx.fillStyle = 'yellow';
    ctx.fill();
    ctx.stroke();

    // central circle
    if (power > 0 && power < 1) {
        ctx.beginPath();
        ctx.arc(x, y, 7, 0, 2 * Math.PI);
        ctx.fill();
    }
}

function getValveColor(act, pos) {
    switch (getValveState(act, pos)) {
        case 0:
            return 'green';
        case 1:
            return 'red';
        default:
            return 'gray';
    }
}

function getValveState(act, pos) {
    switch (act) {
        case 'stopped':
            return (pos === -1) ? -1 : (pos === 1) ? 1 : 0;
        case 'movingUp':
            return 0;
        case 'movingDown':
            return 1;
    }
}

function drawValveIcon(x, y, pos, act, ctx) {
    const angle = Math.PI * pos / -2;
    ctx.beginPath();
    const r = 17;
    const color = getValveColor(act, pos);

    // background rectangle
    ctx.rect(x - r + 1, y - r + 1, 2 * r - 2, 2 * r - 2);
    ctx.strokeStyle = ctx.fillStyle = 'lightgray';
    ctx.lineWidth = 1;
    ctx.fill();
    ctx.stroke();

    // lines around
    ctx.beginPath();
    ctx.moveTo(x - r, y - r + 2);
    ctx.lineTo(x + r, y - r + 2);

    ctx.moveTo(x - r, y + r - 2);
    ctx.lineTo(x + r, y + r - 2);

    ctx.strokeStyle = color;
    ctx.lineWidth = 5;
    ctx.stroke();

    // valve core
    if (pos !== -1) {
        ctx.beginPath();

        ctx.moveTo(x, y);
        ctx.arc(x, y, 1, 0, 2 * Math.PI);
        ctx.lineWidth = 5;
        ctx.stroke();

        ctx.moveTo(x, y);
        ctx.arc(x, y, r - 5, angle, angle);
        ctx.moveTo(x, y);
        ctx.arc(x, y, r - 5, angle + Math.PI, angle + Math.PI);
        ctx.lineWidth = 3;
        ctx.stroke();
    } else {
        ctx.beginPath();
        ctx.font = 1.5 * r + "px Arial Bold";
        ctx.fillStyle = color;
        ctx.textAlign = "center";
        ctx.textBaseline = 'middle';
        ctx.fillText("?", x, y);
        ctx.stroke();
    }
}

function drawPirIcon(x, y, age, ctx) {
    ctx.strokeStyle = 'orange';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.arc(x, y, 7, 0, 2 * Math.PI);
    ctx.fillStyle = 'orange';
    ctx.fill();
    ctx.stroke();

    const maxAge = 60.0;

    if (age >= 0 && age < maxAge) {
        const startAngle = (1.5 - 2 * (1 - age / maxAge)) * Math.PI;
        const endAngle = 1.5 * Math.PI;

        ctx.beginPath();
        ctx.strokeStyle = 'red';
        ctx.moveTo(x, y);
        ctx.arc(x, y, 7, startAngle, endAngle);
        ctx.lineTo(x, y);
        ctx.fillStyle = 'red';
        ctx.fill();
        ctx.stroke();
    }

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

let tmp = '';

function onCanvasClick(event) {
    const item = findNearestItem(event.offsetX, event.offsetY, status.componentMap.values(), function (item) {
        return selectedTool.isApplicable(item) || item.type === 'stairs';
    });

    if (item.type === 'stairs') {
        currentFloor = (item.id === 'stairsUp') ? 1 : 0;
        drawMainCanvas();
        drawItems();
        return;
    }

    // tmp += "['pwm', " + Math.round(parseFloat(event.offsetX)) + ", " + Math.round(parseFloat(event.offsetY)) + ", 0],<br>";
    // debug(tmp);
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

function debug(s) {
    document.getElementById('error').innerHTML = s;
}
