// How long to highlight changes
const changeTimeout = 2000;

// Open web socket connection to send/receive changes
const url = "ws://" + new URL(window.location.href).host + "/web-socket/simulation";
const ws = new SimpleWebSocket(url);
ws.onmessage = function(content) {
    for (const change of content.changes) {
        if (change.what == "value") {
            changeText("pic." + change.id + ".value", change.value);
            changeText("conn." + change.id + ".value", change.value);
        } else if (change.what == "dir") {
            changeDir("pic." + change.id + ".dir", change.value);
            changeDir("conn." + change.id + ".dir", change.value);
        }
    }
};

function changeText(id, value) {
    const e = document.getElementById(id);
    if (e) {
        e.innerText = value;
        e.classList.add("changed");
        setTimeout(function() { e.classList.remove("changed") }, changeTimeout);
    }
}

function changeDir(id, value) {
    const e = document.getElementById(id);
    if (e) {
        if (value == "1") {
            e.classList.remove("out");
            e.classList.add("in");
        } else {
            e.classList.remove("in");
            e.classList.add("out");
        }
        e.classList.add("changed");
        setTimeout(function() { e.classList.remove("changed") }, changeTimeout);
    }
}

function clicked(node, pin) {
    ws.send(`${node}.${pin}`);
}

function showHide(node) {
    document.getElementById(node).classList.toggle("hidden");
}
