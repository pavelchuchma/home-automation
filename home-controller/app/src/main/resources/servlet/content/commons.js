'use strict';

function getCanvasContext(canvasId) {
    const ctx = document.getElementById(canvasId).getContext("2d");
    ctx.rect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.fillStyle = 'lightgray';
    ctx.fill();
    ctx.stroke();
    return ctx;
}

function findNearestItem(x, y, items, filter) {
    function computeDistance(x1, y1, x2, y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
    let result;
    let resDist = Number.MAX_SAFE_INTEGER;
    for (const item of items) {
        if ((item.floor === currentFloor || item.floor < 0) && (filter === undefined || filter(item))) {
            const dist = computeDistance(x, y, item.x, item.y);
            if (dist < resDist) {
                resDist = dist;
                result = item;
            }
        }
    }
    return result;
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