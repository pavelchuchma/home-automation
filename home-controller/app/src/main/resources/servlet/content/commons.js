'use strict';

function drawLightToolSign(parent, x, y, drawVertical) {
    const r = 15;
    const stroke = {width: 7, color: 'red'};
    parent.line(x - r, y, x + r, y).stroke(stroke);
    if (drawVertical) {
        parent.line(x, y - r, x, y + r).stroke(stroke);
    }
}

function printException(e) {
    console.log(e);
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}