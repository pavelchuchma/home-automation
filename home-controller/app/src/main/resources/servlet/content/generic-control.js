// How long to highlight changes
const changeTimeout = 2000;
const state = {}

// Open web socket connection to send/receive changes
const url = "ws://" + new URL(window.location.href).host + "/web-socket/generic-control";
const ws = new WebSocket(url);
ws.onmessage = function(event) {
    const content = JSON.parse(event.data);
    for (const change of content.changes) {
        changeText("conn." + change.id + ".value", change.value);
    }
};
// TODO: Keep alive timeout and recreate connection on close - handles also server restart

function changeText(id, value) {
    const e = document.getElementById(id);
    if (e) {
        e.innerText = value;
        e.classList.add("changed");
        setTimeout(function() { e.classList.remove("changed") }, changeTimeout);
    }
}

function clicked(node, pin) {
    const id = node + "." + pin;
    var v = state[id];
    if (v == 0) {
        v = 1;
    } else {
        v = 0;
    }
    state[id] = v;
    changeText(`conn.${id}.value`, v);
    ws.send(`${id}=${v}`);
}

function clickedPwm(node, pin, plus) {
    const id = node + "." + pin;
    var v = state[id];
    if (v == undefined) v = 0;
    const o = v;
    if (plus) {
        v += 10;
        if (v > 100) v = 100;
    } else {
        v -= 10;
        if (v < 0) v = 0;
    }
    if (v != o) {
        state[id] = v;
        changeText(`conn.${id}.value`, v + '%');
        ws.send(`${id}=${v}%`);
    }
}

function showHide(node) {
    document.getElementById(node).classList.toggle("hidden");
}
