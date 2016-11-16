var mainCtx;
var toolsCtx;
var e = '';

var currentFloor = 0;
var floorIds = ['1stFloor', '2ndFloor'];

const toolBoxBackground = 'lightgray';
const toolLightPlusValue = 66;

var itemCoordinates = [
    //id, x, y, floor
    ['pwmKuLi', 400, 650, 0],
    ['pwmKch1', 410, 759, 0],
    ['pwmKch2', 382, 708, 0],
    ['pwmKch3', 331, 733, 0],
    ['pwmKch4', 268, 686, 0],
    ['pwmKch5', 285, 808, 0],
    ["pwmJid1", 250, 880, 0],
    ["pwmJid2", 142, 896, 0],
    ["pwmJid3", 205, 962, 0],
    ['pwmOb1', 375, 1338, 0],
    ['pwmOb2', 337, 1303, 0],
    ['pwmOb3', 402, 1269, 0],
    ['pwmOb4', 380, 1199, 0],
    //['pwmOb5', 427, 1112, 0],
    ['pwmOb6', 390, 1037, 0],
    ['pwmOb7', 260, 1340, 0],
    ['pwmOb8', 236, 1269, 0],
    ['pwmOb9', 217, 1217, 0],
    ['pwmOb10', 160, 1135, 0],
    ['pwmOb11', 270, 1157, 0],
    ['pwmOb12', 185, 1276, 0],
    ['pwmOb13', 189, 1327, 0],
    ['pwmPrd1', 407, 408, 0],
    ['pwmPrd2', 361, 158, 0],
    ['pwmZadD', 390, 544, 0],
    ['pwmChoD', 266, 517, 0],
    ['pwmKpD', 153, 489, 0],
    ['pwmKpDZrc', 189, 544, 0],
    //['spajz', 206, 328, 0],
    //['sklepL', 518, 345, 0],
    //['sklepP', 567, 350, 0],
    ['pwmG1', 65, 150, 0],
    ['pwmG2', 45, 210, 0],
    ['pwmG3', 25, 270, 0],
    ['pwmTrs', 550, 595, 0],
    ['pwmDrv', 130, 1459, 0],

    ['lvKuch', 510, 809, 0],
    ['lvOb1', 510, 978, 0],
    ['lvOb2', 510, 1121, 0],
    ['lvOb3', 510, 1285, 0],
    ['lvOb4', 360, 1430, 0],
    ['lvOb5', 65, 1286, 0],
    ['lvOb6', 65, 882, 0],
    ['lvKoupD', 65, 510, 0],

    ['stairsUp', 127, 690, 0],
    ['stairsDown', 127, 690, 1],

    ['pwmVchH', 167, 97, 1],
    ['pwmVrt1', 342, 207, 1],
    ['pwmVrt2', 436, 207, 1],
    ['pwmZadH', 196, 289, 1],
    ['pwmChSch', 137, 564, 1],
    ['pwmChP', 208, 564, 1],
    ['pwmKpH', 373, 446, 1],
    ['pwmKpHZrc', 349, 516, 1],
    ['pwmKry', 349, 636, 1],
    ['pwmPata', 349, 830, 1],
    ['pwmMarek', 349, 1039, 1],
    ['pwmLozM', 309, 1280, 1],
    ['pwmLozV',  431, 1280, 1],
    ['pwmPrac', 167, 1324, 1],
    ['pwmSat', 194, 1128, 1],
    ['pwmWc', 134, 953, 1],

    ['lvVrt1', 260, 118, 1],
    ['lvVrt2', 370, 50, 1],
    ['lvVrt3', 530, 298, 1],
    ['lvKoupH', 530, 473, 1],
    ['lvKrys', 530, 640, 1],
    ['lvPata', 530, 823, 1],
    ['lvMarek', 530, 1006, 1],
    ['lvLoz1', 530, 1311, 1],
    ['lvLoz2', 397, 1440, 1],
    ['lvPrc', 55, 1325, 1],
    ['lvSat', 55, 1085, 1],
    ['lvCh1', 55, 850, 1],
    ['lvCh2', 55, 512, 1]
];

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
    }, [isLouversId, isStairs]]
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
    mainCtx.font = h - 20  + "px Arial Bold";
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
            } else if (isLouversId(id)) {
                drawOneLouvers(id)
            } else if (id == 'stairsUp') {
                drawCharacterIcon(id, '▲')
            } else if (id == 'stairsDown') {
                drawCharacterIcon(id, '▼')
            }
        }
    });
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
    var lightStatus = itemStatusMap[id];
    var power = lightStatus.val / lightStatus.maxVal;

    var coords = itemCoordinateMap[id];
    var x = coords[0];
    var y = coords[1];

    drawLightIcon(x, y, power, mainCtx);
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
    //tmp += "['pwm', " + Math.round(parseFloat(event.offsetX)) + ", " + Math.round(parseFloat(event.offsetY)) + ", 0]<br>";
    //debug(tmp);
    //return;

    var selectedTool = toolCoordinateMap[selectedToolId];

    var itemId = findNearestItem(event.offsetX, event.offsetY, itemCoordinates, selectedTool[2]);
    var itemStatus = itemStatusMap[itemId];

    if (isStairs(itemId)) {
        currentFloor = (itemId == 'stairsUp') ? 1 : 0;
        drawMainCanvas();
        onTimer();
        return;
    }

    var action;
    if (startsWith(selectedToolId, 'light')) {
        var value = getNewLightValue(itemStatus);
        action = '/lights/action?id=' + itemStatus.id + "&" + "val=" + value;
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
function updateItems() {
    updateImpl('/lights/status', function (request) {
        parseJsonStatusResponse(request, 'lights', itemStatusMap);
        updateImpl('/louvers/status', function (request) {
            parseJsonStatusResponse(request, 'louvers', itemStatusMap);
            drawItems();
        });
    });
}

function debug(s) {
    document.getElementById('error').innerHTML = s;
}
