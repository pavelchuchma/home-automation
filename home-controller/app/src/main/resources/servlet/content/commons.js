
function getCanvasContext(canvasId) {
    const ctx = document.getElementById(canvasId).getContext("2d");
    ctx.rect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.fillStyle = TOOLBOX_BACKGROUND;
    ctx.fill();
    ctx.stroke();
    return ctx;
}