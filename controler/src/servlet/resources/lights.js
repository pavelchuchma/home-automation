var myTimer = setInterval(onTimer, 1000);
var ctx;
var e = '';

var lightsCoordinates = [
    //x, y, actorIndex, name
    [463, 205, 26, 'Kuchyn 1'],
    [429, 133, 21, 'Kuchyn 2'],
    [360, 169, 22, 'Kuchyn 3'],
    [270, 106, 23, 'Kuchyn 4'],
    [292, 275, 40, 'Kuchyn 5'],
    [244, 374, 30, 'Jídelna 1'],
    [94, 396, 38, 'Jidelna 2'],
    [185, 487, 39, 'Jidelna 3'],
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
    [158, 986, 34, 'Obyvák 13']];

// Creation
selectedLights = [];
for (n = 0; n < lightsCoordinates.length; n++) {
    selectedLights[n] = false;
}

window.onload = function () {
    //updateLights();
    drawCanvas();
}

function onTimer() {
    //updateLights();
}

function computeDistance(x1, y1, x2, y2) {
    return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

function findNearestLight(x, y) {
    var resIndex = 0;
    var resDist = computeDistance(x, y, lightsCoordinates[0][0], lightsCoordinates[0][1]);
    for (i = 1; i < lightsCoordinates.length; i++) {
        var dist = computeDistance(x, y, lightsCoordinates[i][0], lightsCoordinates[i][1]);
        if (dist < resDist) {
            resDist = dist;
            resIndex = i;
        }
    }
    return resIndex;
}

function drawCanvas() {
    var c = document.getElementById("canvas");
    ctx = c.getContext("2d");
    var img = document.getElementById("background");
    ctx.drawImage(img, 0, 0, img.width, img.height);

    //drawLight(lightsCoordinates[0][0], lightsCoordinates[0][1]);
    for (var i = 0; i < lightsCoordinates.length; i++) {
        drawLight(i);
    }

    //document.getElementById('error').innerHTML = 'LOADED!';
}

function drawLight(index) {
    ctx.beginPath();
    ctx.arc(lightsCoordinates[index][0], lightsCoordinates[index][1], 20, 0, 2 * Math.PI);
    ctx.fillStyle = selectedLights[index] ? 'red' : 'black';
    ctx.fill();
    ctx.stroke();
}

function createLightsMap(request) {
    var content = JSON.parse(request.responseText);
    var map = {};
    for (var i = 0; i < content.lights.length; i++) {
        map[content.lights[i].id] = content.lights[i];
    }
    return map;
}

function canvasClick(event) {
    //onLightClick(30);
    var lightIndex = findNearestLight(event.offsetX, event.offsetY);
    selectedLights[lightIndex] = !selectedLights[lightIndex];
    var action = (lightsCoordinates[lightIndex][2] * 4) + ((selectedLights[lightIndex]) ? 0 : 3);
    try {
        var request = new XMLHttpRequest();
        request.open('GET', 'http://10.0.0.150/lights/a' + action, true);
        request.onreadystatechange = function () {
            request.close();
        }
        request.send();

    } catch (e) {
        document.getElementById('error').innerHTML = e.message;
    }
    //e = ;
    drawLight(lightIndex);

    //drawLight(event.offsetX, event.offsetY);
    //e = e + event.offsetX + ' , ' + event.offsetY + '<br>';
    //document.getElementById('error').innerHTML = e;

}

function onLightClick(id) {
    selectedLights[id] = !selectedLights[id];
    setLightClass(id);
}

function onClick(id) {
    var request = new XMLHttpRequest();
    request.open('GET', 'http://localhost:6070/lights/status', true);
    request.onreadystatechange = function () {
    }
    request.send();

    if (document.getElementById(id + 'link').innerHTML == "click") {
        document.getElementById(id + 'link').innerHTML = "CLICK";
    } else {
        document.getElementById(id + 'link').innerHTML = "click";
    }
}

function setLightClass(id) {
    document.getElementById(id).className = (selectedLights[id]) ? 'lightNameSelected' : 'lightName';
}

function updateLights() {
    var request = new XMLHttpRequest();
    request.open('GET', 'http://10.0.0.150/lights/status', true);

    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            try {
                var map = createLightsMap(request);
                var elements = document.getElementById('lights').getElementsByTagName('td');
                for (var i = 0; i < elements.length; i++) {
                    var e = elements[i];
                    var item = map[e.id];
                    document.getElementById(e.id + 'name').innerHTML = item.name;
                    document.getElementById(e.id + 'val').innerHTML = '(' + item.val + '/' + item.maxVal + ') ' + item.curr + 'A';
                    setLightClass(e.id);
                    /*
                     if (map[e.id].val != 0) {
                     document.getElementById(e.id + 'val').className = "activeLight";
                     } else {
                     document.getElementById(e.id + 'val').className = "";
                     }
                     */


                }

            } catch (e) {
                document.getElementById('error').innerHTML = e.message;
            } finally {
                request.close();
            }
        }
    }

    request.send();
}
