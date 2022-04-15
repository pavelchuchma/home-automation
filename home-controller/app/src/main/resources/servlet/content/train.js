
// Open web socket connection to send/receive changes
const url = "ws://" + new URL(window.location.href).host + "/web-socket/train";
const ws = new WebSocket(url);
ws.onmessage = function(event) {
    const content = JSON.parse(event.data);
    switch (content.action) {
        case "switch": drawSwitch(content.dir); break;
    }
};
// TODO: Keep alive timeout and recreate connection on close - handles also server restart

// Draw switch position - 0 - straight, 1 - turn, -1 unknown
function drawSwitch(dir) {
    var canvas = document.getElementById("switchCanvas");
    var context = canvas.getContext("2d");
    context.clearRect(0, 0, canvas.width, canvas.height);
    context.beginPath();
    if (dir == -1) {
        context.moveTo(canvas.width / 2, 0);
        context.lineTo(canvas.width / 2, canvas.height);
        return;
    } else {
        context.moveTo(canvas.width, 3 * canvas.height / 4);
        if (dir == 0) {
            context.lineTo(0, 3 * canvas.height / 4);
        } else {
            context.lineTo(0, canvas.height / 4);
        }
    }
    context.closePath();
    context.stroke();
}
