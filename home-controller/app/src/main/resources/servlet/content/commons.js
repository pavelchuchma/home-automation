'use strict';

function prepareCanvasContext(canvasId) {
    const ctx = document.getElementById(canvasId).getContext("2d");
    ctx.beginPath();
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'black';
    ctx.rect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.fillStyle = 'lightgray';
    ctx.fill();
    ctx.stroke();
    return ctx;
}

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

function printException(e) {
    console.log(e);
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}