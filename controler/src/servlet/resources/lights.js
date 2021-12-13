var mainCtx;
var toolsCtx;
var hvacCtx;
var e = '';

var currentFloor = 0;
var floorIds = ['1stFloor', '2ndFloor'];

const toolBoxBackground = 'lightgray';
const toolLightPlusValue = 66;

const baseUrl = getBaseUrl();
var itemCoordinates = getComponents();

var itemStatusMap = {};
// x, y, floor
var itemCoordinateMap = {};

function drawLightToolSign(x, y, ctx, drawVertical) {
    ctx.beginPath();
    var r = 15;
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

var toolsCoordinates = [
    //id, x, y, floor, draw function, prefixValidator
    ['lightToggle', 50, 50, -1, function (x, y, ctx) {
        drawLightIcon(x - 10, y, 0, ctx);
        drawLightIcon(x + 10, y, .75, ctx);
    }, [isPwmId, isStairs]],
    ['lightPlus', 50, 150, -1, function (x, y, ctx) {
        drawLightIcon(x, y, toolLightPlusValue / 100, ctx);
        drawLightToolSign(x, y, ctx, true);
    }, [isPwmId, isStairs]],
    ['lightMinus', 50, 250, -1, function (x, y, ctx) {
        drawLightIcon(x, y, .25, ctx);
        drawLightToolSign(x, y, ctx, false);
    }, [isPwmId, isStairs]],
    ['lightFull', 50, 350, -1, function (x, y, ctx) {
        drawLightIcon(x, y, 1, ctx);
    }, [isPwmId, isStairs]],
    ['lightOff', 50, 450, -1, function (x, y, ctx) {
        drawLightIcon(x, y, 0, ctx);
    }, [isPwmId, isStairs]],
    ['louversUp', 50, 550, -1, function (x, y, ctx) {
        drawLouversToolIcon(x, y, .3, 0, 'stopped', ctx);
    }, [isLouversId, isStairs]],
    ['louversOutshine', 50, 650, -1, function (x, y, ctx) {
        drawLouversToolIcon(x, y, 1, 0, 'stopped', ctx);
    }, [isLouversId, isStairs]],
    ['louversDown', 50, 750, -1, function (x, y, ctx) {
        drawLouversToolIcon(x, y, 1, 1, 'stopped', ctx);
    }, [isLouversId, isStairs]],
    ['valveToggle', 50, 850, -1, function (x, y, ctx) {
        drawValveIcon(x + 10, y - 5, 1, 'stopped', ctx);
        drawValveIcon(x - 10, y + 5, 0, 'stopped', ctx);
    }, [isValveId, isStairs]]
];


function drawLouversToolIcon(x, y, position, offset, action, ctx) {
    var w = 50;
    var h = 60;
    drawLouversIconImpl(x, y, position, offset, action, ctx, w, h);
}

function drawLouversIcon(x, y, position, offset, action, ctx) {
    var w = 70;
    var h = 80;
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
        var lineWidth = louverHeight * offset;
        ctx.beginPath();
        for (var i = h * position - lineWidth / 2; i >= lineWidth / 2; i -= louverHeight) {
            var yy = y - h / 2 + i;
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

function drawCharacterIcon(id, text) {
    var coords = itemCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    var w = 70;
    var h = 80;

    // white rectangle
    mainCtx.beginPath();
    mainCtx.rect(x - w / 2, y - h / 2, w, h);
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
    mainCtx.fillText(text, x, y);
    mainCtx.stroke();

}

var toolCoordinateMap = {};
var selectedToolId = toolsCoordinates[0][0];


window.onload = function () {
    try {
        itemCoordinates.forEach(function (lc) {
            itemCoordinateMap[lc[0]] = [lc[1], lc[2], lc[3]];
        });

        toolsCoordinates.forEach(function (tc) {
            toolCoordinateMap[tc[0]] = [tc[1], tc[2], tc[5]];
        });

        drawMainCanvas();
        drawToolsCanvas();
        drawHvacCanvas();
        drawPumpCanvas();
        setInterval(onTimer, 750);
        onTimer();
    } catch (e) {
        printException(e);
    }
};

function onTimer() {
    try {
        updateItems();
    } catch (e) {
        printException(e);
    }
}

function computeDistance(x1, y1, x2, y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

function findNearestItem(x, y, coordinates, validatorArray) {
    var resId = null;
    var resDist = -1;
    coordinates.forEach(function (lc) {
        // validate item type
        validatorArray.some(function (validator) {
            var floor = lc[3];
            if ((floor < 0 || floor == currentFloor) && validator(lc[0])) {
                var dist = computeDistance(x, y, lc[1], lc[2]);
                if (resDist < 0 || dist < resDist) {
                    resDist = dist;
                    resId = lc[0];
                }
                return true; // stop loop through validators
            }
            return false;
        });
    });
    return resId;
}

function isStairs(id) {
    return startsWith(id, "stairs");
}

function isPwmId(id) {
    return startsWith(id, "pwm");
}

function isValveId(id) {
    return startsWith(id, "vl");
}

function isPirId(id) {
    return startsWith(id, "pir") || startsWith(id, "mgnt");
}

function startsWith(str, substr) {
    return str.length >= substr.length && (str.substr(0, substr.length) == substr);
}

function isLouversId(id) {
    return startsWith(id, "lv");
}

function drawItems() {
    itemCoordinates.forEach(function (lc) {
        var id = lc[0];
        if (lc[3] == currentFloor) {
            if (isPwmId(id)) {
                drawLight(id)
            } else if (isValveId(id)) {
                drawValve(id)
            } else if (isPirId(id)) {
                drawPir(id)
            } else if (isLouversId(id)) {
                drawOneLouvers(id)
            } else if (id == 'stairsUp') {
                drawCharacterIcon(id, '▲')
            } else if (id == 'stairsDown') {
                drawCharacterIcon(id, '▼')
            } else if (id == 'hvac') {
                drawHvacScreen()
            }
        }
    });
    drawPumpScreen();
}

function drawMainCanvas() {
    var c = document.getElementById("mainCanvas");
    mainCtx = c.getContext("2d");
    var img = document.getElementById(floorIds[currentFloor]);
    mainCtx.drawImage(img, 0, 0, img.width, img.height);

    //document.getElementById('error').innerHTML = 'LOADED!';
}
function drawToolSelection() {
    toolsCoordinates.forEach(function (c) {
        var r = 35;

        toolsCtx.beginPath();
        toolsCtx.rect(c[1] - r, c[2] - r, 2 * r, 2 * r);
        toolsCtx.strokeStyle = (selectedToolId == c[0]) ? 'red' : toolBoxBackground;
        toolsCtx.lineWidth = 10;
        toolsCtx.stroke();
    });
}
function drawToolsCanvas() {
    var c = document.getElementById("toolsCanvas");
    toolsCtx = c.getContext("2d");

    toolsCtx.rect(0, 0, 100, toolsCoordinates.length * 100);
    toolsCtx.fillStyle = toolBoxBackground;
    toolsCtx.fill();
    toolsCtx.stroke();

    toolsCoordinates.forEach(function (c) {
        c[4](c[1], c[2], toolsCtx);
    });

    drawToolSelection();
}

function drawHvacCanvas() {
    var c = document.getElementById("hvacCanvas");
    hvacCtx = c.getContext("2d");

    hvacCtx.rect(0, 0, 100, 150);
    hvacCtx.fillStyle = toolBoxBackground;
    hvacCtx.fill();
    hvacCtx.stroke();
}

function drawHvacScreen() {
    // clr
    hvacCtx.rect(0, 0, 100, 150);
    hvacCtx.fillStyle = toolBoxBackground;
    hvacCtx.fill();
    hvacCtx.stroke();

    var hvacStatus = itemStatusMap['hvac'];

    if (hvacStatus.on) {
        hvacCtx.font = "bold 15px Arial";
        hvacCtx.fontWeight = "500";
        hvacCtx.fillStyle = 'red';
        hvacCtx.fillText(hvacStatus.targetMode, 5, 20);
        hvacCtx.fillText(hvacStatus.fanSpeed, 5, 40);
        hvacCtx.fillText('Tgt temp: ' + hvacStatus.targetTemperature, 5, 60);
        hvacCtx.font = "13px Arial";
        let y = 70;
        let step = 17;
        hvacCtx.fillText('Air temp: ' + hvacStatus.airTemperature + '/' + hvacStatus.air2Temperature, 5, y += step);
        hvacCtx.fillText('Room temp: ' + hvacStatus.roomTemperature, 5, y += step);
        hvacCtx.fillText('Unit temp: ' + hvacStatus.unitTemperature, 5, y += step);
        if (hvacStatus.defrost) {
            hvacCtx.font = "bold 15px Arial";
            hvacCtx.fillText('Defrost!', 5, y += step + 5);
        }
    } else {
        hvacCtx.fillStyle = 'black';
        hvacCtx.font = "30px Arial";
        hvacCtx.fillText('OFF', 15, 30);
    }
}

function onHvacClick(event) {
    var hvacStatus = itemStatusMap['hvac'];
    if (hvacStatus.on) {
        sendAction('/hvac/action?id=hvac&on=false');
    } else {
        sendAction('/hvac/action?id=hvac&on=true');
    }
}

function drawPumpCanvas() {
    var c = document.getElementById("pumpCanvas");
    pumpCtx = c.getContext("2d");

    pumpCtx.rect(0, 0, 100, 50);
    pumpCtx.fillStyle = toolBoxBackground;
    pumpCtx.fill();
    pumpCtx.stroke();
}

function drawPumpScreen() {
    // clr
    pumpCtx.rect(0, 0, 100, 150);
    pumpCtx.fillStyle = toolBoxBackground;
    pumpCtx.fill();
    pumpCtx.stroke();

    var pumpStatus = itemStatusMap['wpmp'];
    pumpCtx.fillStyle = 'black';
    pumpCtx.font = "12px Arial";
    pumpCtx.fillText('Cykly: ' + pumpStatus.lastPeriodRecCount + '/' + pumpStatus.recCount, 5, 20);
    var lastRecordDuration = -1;
    if (pumpStatus.lastRecords.length > 0) {
        lastRecordDuration = pumpStatus.lastRecords[pumpStatus.lastRecords.length - 1].duration;
    }
    pumpCtx.fillText('Posledni: ' + Math.round(lastRecordDuration / 10.0) / 100.0 + ' s', 5, 35);
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
    var startAngle = 1.5 * Math.PI;
    var endAngle = (1.5 + 2 * power) * Math.PI;
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
            return (pos == -1) ? -1 : (pos == 1) ? 1 : 0;
        case 'movingUp':
            return 0;
        case 'movingDown':
            return 1;
    }
}

function drawValveIcon(x, y, pos, act, ctx) {
    var angle = Math.PI * pos / -2;
    ctx.beginPath();
    var r = 17;
    var color = getValveColor(act, pos);

    // background rectagle
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
    if (pos != -1) {
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

    var maxAge = 60.0;

    if (age >= 0 && age < maxAge) {
        var startAngle = (1.5 - 2 * (1 - age / maxAge)) * Math.PI;
        var endAngle = 1.5 * Math.PI;

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

function drawLight(id) {
    var lightStatus = itemStatusMap[id];
    var power = lightStatus.val / lightStatus.maxVal;

    var coords = itemCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawLightIcon(x, y, power, mainCtx);
}

function drawValve(id) {
    var valveStatus = itemStatusMap[id];

    var coords = itemCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawValveIcon(x, y, valveStatus.pos, valveStatus.act, mainCtx);
}

function drawPir(id) {
    var pirStatus = itemStatusMap[id];

    var coords = itemCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawPirIcon(x, y, pirStatus.age, mainCtx);
}

function drawOneLouvers(id) {
    var louversStatus = itemStatusMap[id];

    var coords = itemCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawLouversIcon(x, y, louversStatus.pos, louversStatus.off, louversStatus.act, mainCtx);
}

function parseJsonStatusResponse(request, rootName, map) {
    var content = JSON.parse(request.responseText);
    content[rootName].forEach(function (l) {
        map[l.id] = l;
    });
}

function sendAction(action) {
    //document.getElementById('error').innerHTML = action;
    try {
        var request = new XMLHttpRequest();
        request.open('GET', baseUrl + action, true);
        request.onreadystatechange = function () {
            //request.close();
        };
        request.send();

    } catch (e) {
        printException(e);
    }
}

function onToolsClick(event) {
    selectedToolId = findNearestItem(event.offsetX, event.offsetY, toolsCoordinates, [function () {
        return true
    }]);
    drawToolSelection();
}

function getNewLightValue(lightStatus) {
    const step = 15;
    var val = parseInt(lightStatus.val);
    var maxVal = parseInt(lightStatus.maxVal);
    var vPerc = Math.round(val / maxVal * 100);
    switch (selectedToolId) {
        case 'lightToggle':
            return (vPerc == 0) ? 75 : 0;
        case 'lightPlus':
            return (vPerc == 0) ? toolLightPlusValue : Math.min(100, vPerc + step);
        case 'lightMinus':
            return (vPerc == 0) ? 1 : Math.max(0, vPerc - step);
        case 'lightFull' :
            return 100;
        case 'lightOff':
            return 0;
    }
    return val;
}

function buildLouversActionLink(itemStatus, possiton, offset) {
    return '/louvers/action?id=' + itemStatus.id + '&pos=' + possiton + '&off=' + offset;
}

var tmp = '';
function onCanvasClick(event) {

    var selectedTool = toolCoordinateMap[selectedToolId];

    var itemId = findNearestItem(event.offsetX, event.offsetY, itemCoordinates, selectedTool[2]);
    var itemStatus = itemStatusMap[itemId];

    if (isStairs(itemId)) {
        currentFloor = (itemId == 'stairsUp') ? 1 : 0;
        drawMainCanvas();
        onTimer();
        return;
    }

    // tmp += "['pwm', " + Math.round(parseFloat(event.offsetX)) + ", " + Math.round(parseFloat(event.offsetY)) + ", 0],<br>";
    // debug(tmp);
    // return;

    var action;
    if (startsWith(selectedToolId, 'light')) {
        var value = getNewLightValue(itemStatus);
        action = '/lights/action?id=' + itemStatus.id + "&" + "val=" + value;
    } else if (startsWith(selectedToolId, 'valveToggle')) {
        var valveVal = (getValveState(itemStatus.act, itemStatus.pos) == 0) ? 100 : 0;
        action = '/airvalves/action?id=' + itemStatus.id + "&" + "val=" + valveVal;
    } else if (startsWith(selectedToolId, 'louvers')) {
        switch (selectedToolId) {
            case 'louversUp':
                action = buildLouversActionLink(itemStatus, 0, 0);
                break;
            case 'louversOutshine':
                action = buildLouversActionLink(itemStatus, 100, 0);
                break;
            case 'louversDown':
                action = buildLouversActionLink(itemStatus, 100, 100);
                break;
        }
    }

    if (action) {
        sendAction(action);
    }

    var coords = itemCoordinateMap[itemStatus.id];
    // draw changed light as gray
    mainCtx.beginPath();
    mainCtx.arc(coords[0], coords[1], 15, 0, 2 * Math.PI);
    mainCtx.fillStyle = 'gray';
    mainCtx.fill();
}


function printException(e) {
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}

function updateImpl(path, code) {
    var request = new XMLHttpRequest();

    request.open('GET', baseUrl + path, true);

    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            try {
                code(request);
            } catch (e) {
                printException(e);
            } finally {
                //request.close();
            }
        }
    };

    request.send();
}

function updateItems() {
    updateImpl('/lights/status', function (request) {
        parseJsonStatusResponse(request, 'lights', itemStatusMap);
        updateImpl('/louvers/status', function (request) {
            parseJsonStatusResponse(request, 'louvers', itemStatusMap);
            updateImpl('/airvalves/status', function (request) {
                parseJsonStatusResponse(request, 'airValves', itemStatusMap);
                updateImpl('/pir/status', function (request) {
                    parseJsonStatusResponse(request, 'pir', itemStatusMap);
                    updateImpl('/hvac/status', function (request) {
                        parseJsonStatusResponse(request, 'hvac', itemStatusMap);
                        updateImpl('/wpump/status', function (request) {
                            parseJsonStatusResponse(request, 'wpmp', itemStatusMap);
                            drawItems();
                        });
                    });
                });
            });
        });
    });
}

function debug(s) {
    document.getElementById('error').innerHTML = s;
}
