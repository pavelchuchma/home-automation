let status;

window.onload = function () {
    try {
        status = new Status('/rest/nodes/status', 750, function () {
            updateAll();
        }, undefined, getBaseUrl(), function (id) {
            return new NodeInfoItem(id, undefined)
        });
        status.startRefresh();
    } catch (e) {
        printException(e);
    }
};

function updateAll() {
    for (const item of status.componentMap.values()) {
        updateValue(item);
    }
}

function updateValue(item) {
    const id = item.id;
    document.getElementById('name' + id).innerHTML = `<a href='/nodes/detail?id=${item.id}'>${item.id}-${item.name}</a>`;

    const age = document.getElementById('pa' + id);
    age.innerHTML = (item.lastPingAge >= 0) ? `${item.lastPingAge}` : '-';
    age.className = (item.lastPingAge <= 60) ? 'pingOK' : 'pingLate'

    formatTime('boot' + id, item.bootTime);
    formatTime('bld' + id, item.buildTime);

    let msgElement = '';
    for (const msg of item.messages) {
        const msgTypeName = msg.typeName.startsWith('MSG_') ? msg.typeName.substring(4) : msg.typeName;
        msgElement += `<div class='${(msg.dir === 'r') ? 'receivedMessage' : 'sentMessage'}'>${msgTypeName}${msg.data}</div>`;
    }
    document.getElementById('msg' + id).innerHTML = msgElement;
}

function formatTime(id, time) {
    const td = document.getElementById(id);
    td.innerHTML = (time != null) ? time : '-';
}

function performServletAction(id) {
    const path = `/rest/servletActions/action?id=${id}&action=perform`;
    BaseItem._send(path);
}