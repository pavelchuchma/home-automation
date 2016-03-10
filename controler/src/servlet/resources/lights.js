var mainCtx;
var toolsCtx;
var e = '';
var tmp = "";

const toolBoxBackground = 'lightgray';
const toolLightPlusValue = 66;

var lightCoordinates = [
    //id, x, y
    ['pwmKch1', 410, 759],
    ['pwmKch2', 382, 708],
    ['pwmKch3', 331, 733],
    ['pwmKch4', 268, 686],
    ['pwmKch5', 285, 808],
    ["pwmJid1", 250, 880],
    ["pwmJid2", 142, 896],
    ["pwmJid3", 205, 962],
    ['pwmOb1', 375, 1338],
    ['pwmOb2', 337, 1303],
    ['pwmOb3', 402, 1269],
    ['pwmOb4', 380, 1199],
    ['pwmOb5', 427, 1112],
    ['pwmOb6', 390, 1037],
    ['pwmOb7', 260, 1340],
    ['pwmOb8', 236, 1269],
    ['pwmOb9', 217, 1217],
    ['pwmOb10', 160, 1135],
    ['pwmOb11', 270, 1157],
    ['pwmOb12', 185, 1276],
    ['pwmOb13', 189, 1327],
    ['pwmPrd1', 407, 408],
    ['pwmPrd2', 361, 158],
    ['pwmZadD', 390, 544],
    ['pwmChoD', 266, 517],
    ['pwmKpD', 153, 489],
    ['pwmKpDZrc', 189, 544],
    //['spajz', 206, 328],
    //['sklepL', 518, 345],
    //['sklepP', 567, 350],
    ['pwmG1', 65, 150],
    ['pwmG2', 45, 210],
    ['pwmG3', 25, 270]
];

var lightStatusMap;
var lightCoordinateMap = {};

var louversCoordinates = [
    //id, x, y
    ['lvKuch', 510, 809],
    ['lvOb1', 510, 978],
    ['lvOb2', 510, 1121],
    ['lvOb3', 510, 1285],
    ['lvOb4', 360, 1430],
    ['lvOb5', 65, 1286],
    ['lvOb6', 65, 882],
    ['lvKoupD', 65, 510]
];

var louversStatusMap;
var louversCoordinateMap = {};

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
        drawLightIcon(x - 10, y, 0, ctx);
        drawLightIcon(x + 10, y, .75, ctx);
    }],
    ['ligthPlus', 50, 150, function (x, y, ctx) {
        drawLightIcon(x, y, toolLightPlusValue / 100, ctx);
        drawLightToolSign(x, y, ctx, true);
    }],
    ['ligthMinus', 50, 250, function (x, y, ctx) {
        drawLightIcon(x, y, .25, ctx);
        drawLightToolSign(x, y, ctx, false);
    }],
    ['ligthFull', 50, 350, function (x, y, ctx) {
        drawLightIcon(x, y, 1, ctx);
    }],
    ['ligthOff', 50, 450, function (x, y, ctx) {
        drawLightIcon(x, y, 0, ctx);
    }],
    ['louversUp', 50, 550, function (x, y, ctx) {
        drawLouversToolIcon(x, y, .5, 0, 'stopped', ctx);
    }],
    ['louversDown', 50, 650, function (x, y, ctx) {
        drawLouversToolIcon(x, y, 1, .5, 'stopped', ctx);
    }]
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

var toolCoordinateMap = {};
var selectedToolId = toolsCoordinates[0][0];


window.onload = function () {
    try {
        lightCoordinates.forEach(function (lc) {
            lightCoordinateMap[lc[0]] = [lc[1], lc[2]];
        });

        louversCoordinates.forEach(function (lc) {
            louversCoordinateMap[lc[0]] = [lc[1], lc[2]];
        });

        toolsCoordinates.forEach(function (tc) {
            toolCoordinateMap[tc[0]] = [tc[1], tc[2]];
        });


        drawMainCanvas();
        drawToolsCanvas();
        setInterval(onTimer, 750);
        onTimer();
    } catch (e) {
        printException(e);
    }
};

function onTimer() {
    try {
        updateLights();
        updateLouvers();
    } catch (e) {
        printException(e);
    }
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

function drawAllLouvers() {
    louversCoordinates.forEach(function (lc) {
        drawOneLouvers(lc[0])
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

function drawLight(id) {
    var lightStatus = lightStatusMap[id];
    var power = lightStatus.val / lightStatus.maxVal;

    var coords = lightCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawLightIcon(x, y, power, mainCtx);
}

function drawOneLouvers(id) {
    var louversStatus = louversStatusMap[id];
    //var power = louversStatus.val / louversStatus.maxVal;

    var coords = louversCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawLouversIcon(x, y, louversStatus.pos, louversStatus.off, louversStatus.act, mainCtx);
}

function parseJsonStatusResponse(request, rootName) {
    var content = JSON.parse(request.responseText);
    var map = {};
    content[rootName].forEach(function (l) {
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
            return (vPerc == 0) ? 75 : 0;
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
    //tmp += "['pwm', " + event.offsetX + ", " + event.offsetY + "]<br>";
    //debug(tmp);
    //return;

    var lightId = findNearestItem(event.offsetX, event.offsetY, lightCoordinates);
    var lightStatus = lightStatusMap[lightId];

    var value = getLightValue(lightStatus);
    var action = '/lights/acton?id=' + lightStatus.id + "&" + "val=" + value;

    sendAction(action);

    var coords = lightCoordinateMap[lightStatus.id];
    // draw changed light as gray
    mainCtx.beginPath();
    mainCtx.arc(coords[0], coords[1], 20, 0, 2 * Math.PI);
    mainCtx.fillStyle = 'gray';
    mainCtx.fill();

}

function printException(e) {
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}

function updateImpl(path, code) {
    var request = new XMLHttpRequest();

    request.open('GET', 'http://10.0.0.150' + path, true);

    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            try {
                code(request);
            } catch (e) {
                printException(e);
            } finally {
                request.close();
            }
        }
    };

    request.send();
}
function updateLights() {
    updateImpl('/lights/status', function (request) {
        lightStatusMap = parseJsonStatusResponse(request, 'lights');
        drawLights();
    });
}

function updateLouvers() {
    updateImpl('/louvers/status', function (request) {
        louversStatusMap = parseJsonStatusResponse(request, 'louvers');
        drawAllLouvers();
    });
}

function debug(s) {
    document.getElementById('error').innerHTML = s;
}