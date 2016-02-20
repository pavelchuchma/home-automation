var myTimer = setInterval(onTimer, 750);
var ctx;
var e = '';

var lightCoordinates = [
    //x, y, actorIndex, name
    [465, 205, 26, 'Kuchyn 1'],
    [429, 133, 21, 'Kuchyn 2'],
    [360, 169, 22, 'Kuchyn 3'],
    [269, 102, 23, 'Kuchyn 4'],
    [292, 273, 40, 'Kuchyn 5'],
    [247, 372, 30, 'Jídelna 1'],
    [97, 396, 38, 'Jidelna 2'],
    [185, 485, 39, 'Jidelna 3'],
    [418, 1004, 33, 'Obyvák 01'],
    [362, 956, 16, 'Obyvák 02'],
    [454, 904, 10, 'Obyvák 03'],
    [423, 811, 37, 'Obyvák 04'],
    [486, 694, 27, 'Obyvák 05'],
    [435, 591, 31, 'Obyvák 06'],
    [255, 1002, 9, 'Obyvák 07'],
    [226, 904, 8, 'Obyvák 08'],
    [198, 833, 7, 'Obyvák 09'],
    [120, 723, 32, 'Obyvák 10'],
    [272, 753, 20, 'Obyvák 11'],
    [153, 916, 41, 'Obyvák 12'],
    [158, 986, 34, 'Obyvák 13']
];

var lightStatusMap;
var lightCoordinateMap = {};

// Creation
lightCoordinates.forEach(function(lc) {
    lightCoordinateMap[lc[2]] = [lc[0], lc[1]];
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
    var resIndex = 0;
    var resDist = computeDistance(x, y, lightCoordinates[0][0], lightCoordinates[0][1]);
    for (var i = 1; i < lightCoordinates.length; i++) {
        var dist = computeDistance(x, y, lightCoordinates[i][0], lightCoordinates[i][1]);
        if (dist < resDist) {
            resDist = dist;
            resIndex = i;
        }
    }
    return lightStatusMap[lightCoordinates[resIndex][2]];
}

function drawLights() {
    lightCoordinates.forEach(function(lc) {
        drawLight(lc[2])
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
    var coords = lightCoordinateMap[id];
    var power = lightStatus.val / lightStatus.maxVal;

    // black background
    if (power < 1) {
        ctx.beginPath();
        ctx.arc(coords[0], coords[1], 20, 0, 2 * Math.PI);
        ctx.fillStyle = 'black';
        ctx.fill();
    }

    // yellow pie
    ctx.beginPath();
    if (power < 1) {
        ctx.moveTo(coords[0], coords[1]);
    }
    var startAngle = 1.5 * Math.PI;
    var endAngle = (1.5 + 2 * power) * Math.PI;
    ctx.arc(coords[0], coords[1], 20, startAngle, endAngle);
    if (power < 1) {
        ctx.lineTo(coords[0], coords[1]);
    }
    ctx.fillStyle = 'yellow';
    ctx.fill();
    ctx.stroke();

    // central circle
    if (power > 0 && power < 1) {
        ctx.beginPath();
        ctx.arc(coords[0], coords[1], 7, 0, 2 * Math.PI);
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

function onCanvasClick(event) {
    var lightStatus = findNearestLight(event.offsetX, event.offsetY);

    var action = (lightStatus.id * 4) + ((lightStatus.val == 0) ? 0 : 3);
    try {
        var request = new XMLHttpRequest();
        request.open('GET', 'http://10.0.0.150/lights/a' + action, true);
        request.onreadystatechange = function () {
            request.close();
        };
        request.send();

    } catch (e) {
        document.getElementById('error').innerHTML = e.message;
    }
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
                document.getElementById('error').innerHTML = e.message;
            } finally {
                request.close();
            }
        }
    }

    request.send();
}
