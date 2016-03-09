myTimer = setInterval(onTimer, 750);
var mainCtx;
var toolsCtx;
var e = '';

const toolBoxBackground = 'lightgray';
const toolLightPlusValue = 66;

var lightCoordinates = [
    //id, x, y
    ['pwmKch1', 465, 205],
    ['pwmKch2', 429, 133],
    ['pwmKch3', 360, 169],
    ['pwmKch4', 269, 102],
    ['pwmKch5', 292, 273],
    ["pwmJid1", 247, 372],
    ["pwmJid2", 97, 396],
    ["pwmJid3", 185, 485],
    ['pwmOb1', 418, 1004],
    ['pwmOb2', 362, 956],
    ['pwmOb3', 454, 904],
    ['pwmOb4', 423, 811],
    ['pwmOb5', 486, 694],
    ['pwmOb6', 435, 591],
    ['pwmOb7', 255, 1002],
    ['pwmOb8', 226, 904],
    ['pwmOb9', 198, 833],
    ['pwmOb10', 120, 723],
    ['pwmOb11', 272, 753],
    ['pwmOb12', 153, 916],
    ['pwmOb13', 158, 986]
];

var lightStatusMap;
var lightCoordinateMap = {};
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
    //id, x, y, draw function
    ['ligthToogle', 50, 50, function (x, y, ctx) {
        drawLightImpl(x-10, y, 0, ctx);
        drawLightImpl(x+10, y, 1, ctx);
    }],
    ['ligthPlus', 50, 150, function (x, y, ctx) {
        drawLightImpl(x, y, toolLightPlusValue / 100, ctx);
        drawLightToolSign(x, y, ctx, true);
    }],
    ['ligthMinus', 50, 250, function (x, y, ctx) {
        drawLightImpl(x, y, .25, ctx);
        drawLightToolSign(x, y, ctx, false);
    }],
    ['ligthFull', 50, 350, function (x, y, ctx) {
        drawLightImpl(x, y, 1, ctx);
    }],
    ['ligthOff', 50, 450, function (x, y, ctx) {
        drawLightImpl(x, y, 0, ctx);
    }]
];

var toolCoordinateMap = {};
var selectedToolId = toolsCoordinates[0][0];


// Creation
lightCoordinates.forEach(function (lc) {
    lightCoordinateMap[lc[0]] = [lc[1], lc[2]];
});

toolsCoordinates.forEach(function (tc) {
    toolCoordinateMap[tc[0]] = [tc[1], tc[2]];
});

window.onload = function () {
    drawMainCanvas();
    drawToolsCanvas();
    updateLights();
};

function onTimer() {
    updateLights();
}

function computeDistance(x1, y1, x2, y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

function findNearestItem(x, y, coordinates) {
    var resId = null;
    var resDist = -1;
    coordinates.forEach(function (lc) {
        var dist = computeDistance(x, y, lc[1], lc[2]);
        if (resDist < 0 || dist < resDist) {
            resDist = dist;
            resId = lc[0];
        }
    });
    return resId;
}

function drawLights() {
    lightCoordinates.forEach(function (lc) {
        drawLight(lc[0])
    });
}
function drawMainCanvas() {
    var c = document.getElementById("mainCanvas");
    mainCtx = c.getContext("2d");
    var img = document.getElementById("background");
    mainCtx.drawImage(img, 0, 0, img.width, img.height);

    //document.getElementById('error').innerHTML = 'LOADED!';
}
function drawToolSelection() {
    toolsCoordinates.forEach(function (c) {
        //var  = toolCoordinateMap[selectedToolId]
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
        c[3](c[1], c[2], toolsCtx);
    });

    drawToolSelection();
}

function drawLightImpl(x, y, power, ctx) {
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
function drawLight(id) {
    var lightStatus = lightStatusMap[id];
    var power = lightStatus.val / lightStatus.maxVal;

    var coords = lightCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawLightImpl(x, y, power, mainCtx);
}

function createLightStatusMap(request) {
    var content = JSON.parse(request.responseText);
    var map = {};
    content.lights.forEach(function (l) {
        map[l.id] = l;
    });
    return map;
}

function sendAction(action) {
    //document.getElementById('error').innerHTML = action;
    try {
        var request = new XMLHttpRequest();
        request.open('GET', 'http://10.0.0.150' + action, true);
        request.onreadystatechange = function () {
            request.close();
        };
        request.send();

    } catch (e) {
        printException(e);
    }
}

function onToolsClick(event) {
    selectedToolId = findNearestItem(event.offsetX, event.offsetY, toolsCoordinates);
    drawToolSelection();
}

function getLightValue(lightStatus) {
    const step = 15;
    var val = parseInt(lightStatus.val);
    var maxVal = parseInt(lightStatus.maxVal);
    var vPerc = Math.round(val / maxVal * 100);
    switch (selectedToolId) {
        case 'ligthToogle':
            return (vPerc == 0) ? 100 : 0;
        case 'ligthPlus':
            return (vPerc == 0) ? toolLightPlusValue : Math.min(100, vPerc + step);
        case 'ligthMinus':
            return (vPerc == 0) ? 1 : Math.max(0, vPerc - step);
        case 'ligthFull' :
            return 100;
        case 'ligthOff':
            return 0;
    }
    return val;
}
function onCanvasClick(event) {
    var lightId = findNearestItem(event.offsetX, event.offsetY, lightCoordinates);
    var lightStatus = lightStatusMap[lightId];

    var value = getLightValue(lightStatus);
    var action = '/lights/acton?id=' + lightStatus.id + "&" + "val=" + value;

    sendAction(action);

    var coords = lightCoordinateMap[lightStatus.id];

    mainCtx.beginPath();
    mainCtx.arc(coords[0], coords[1], 20, 0, 2 * Math.PI);
    mainCtx.fillStyle = 'gray';
    mainCtx.fill();

}

function printException(e) {
    var stackTrace = new Error().stack;
    document.getElementById('error').innerHTML = e.message + '<br>' + stackTrace.replace('\n', '<br>');
}

function updateLights() {
    var request = new XMLHttpRequest();
    request.open('GET', 'http://10.0.0.150/lights/status', true);

    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            try {
                lightStatusMap = createLightStatusMap(request);
                drawLights();
            } catch (e) {
                printException(e);
            } finally {
                request.close();
            }
        }
    };

    request.send();
}

function debug(s) {
    document.getElementById('error').innerHTML = s;
}