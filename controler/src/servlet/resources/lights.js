var myTimer = setInterval(onTimer, 750);
var ctx;
var e = '';

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

// Creation
lightCoordinates.forEach(function (lc) {
    lightCoordinateMap[lc[0]] = [lc[1], lc[2]];
});

window.onload = function () {
    drawCanvas();
    updateLights();
};

function onTimer() {
    updateLights();
}

function computeDistance(x1, y1, x2, y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

function findNearestLight(x, y) {
    var resId = null;
    var resDist = -1;
    lightCoordinates.forEach(function (lc) {
        var dist = computeDistance(x, y, lc[1], lc[2]);
        if (resDist < 0 || dist < resDist) {
            resDist = dist;
            resId = lc[0];
        }
    });
    return lightStatusMap[resId];
}

function drawLights() {
    lightCoordinates.forEach(function (lc) {
        drawLight(lc[0])
    });
}
function drawCanvas() {
    var c = document.getElementById("canvas");
    ctx = c.getContext("2d");
    var img = document.getElementById("background");
    ctx.drawImage(img, 0, 0, img.width, img.height);

    //document.getElementById('error').innerHTML = 'LOADED!';
}

function drawLight(id) {
    var lightStatus = lightStatusMap[id];
    var power = lightStatus.val / lightStatus.maxVal;

    var coords = lightCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    // black background
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
function onCanvasClick(event) {
    var lightStatus = findNearestLight(event.offsetX, event.offsetY);

    var value = (lightStatus.val == 0) ? 66 : 0;
    var action = '/lights/acton?id=' + lightStatus.id + "&" + "val=" + value;

    sendAction(action);

    var coords = lightCoordinateMap[lightStatus.id];

    ctx.beginPath();
    ctx.arc(coords[0], coords[1], 20, 0, 2 * Math.PI);
    ctx.fillStyle = 'gray';
    ctx.fill();

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
