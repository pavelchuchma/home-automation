'use strict';

function drawLightToolSign(parent, x, y, drawVertical) {
    const r = 15;
    const stroke = {width: 7, color: 'red'};
    parent.line(x - r, y, x + r, y).stroke(stroke);
    if (drawVertical) {
        parent.line(x, y - r, x, y + r).stroke(stroke);
    }
}

/**
 * Pie slice drawn clockwise from startAngle to endAngle (radians, 0 = 3 o'clock); a full turn is drawn as a circle.
 */
function drawPieSlice(parent, x, y, r, startAngle, endAngle) {
    if (endAngle - startAngle >= 2 * Math.PI) {
        return parent.circle(2 * r).center(x, y);
    }
    const point = angle => `${x + r * Math.cos(angle)} ${y + r * Math.sin(angle)}`;
    const largeArc = (endAngle - startAngle > Math.PI) ? 1 : 0;
    return parent.path(`M ${x} ${y} L ${point(startAngle)} A ${r} ${r} 0 ${largeArc} 1 ${point(endAngle)} Z`);
}

function drawCenteredText(parent, text, x, y, size, color) {
    return parent.plain(text).font({family: 'Arial', size: size, anchor: 'middle'})
        .attr({x: x, y: y, 'dominant-baseline': 'central'}).fill(color);
}

function printException(e) {
    console.log(e);
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}