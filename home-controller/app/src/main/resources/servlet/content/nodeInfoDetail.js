let status;

function onLoad(id) {
    try {
        status = new Status(`/rest/nodes/status?id=${id}`, 750, function () {
            updateAll();
        }, undefined, getBaseUrl(), function (id) {
            return new NodeInfoItem(id, undefined)
        });
        status.startRefresh();
    } catch (e) {
        printException(e);
    }
}

function updateAll() {
    for (const item of status.componentMap.values()) {
        const testMode = item.testMode;
        const btnTestCycle = document.getElementById('btnTestCycle');
        const btnTestOn = document.getElementById('btnTestOn');
        const btnTestOff = document.getElementById('btnTestOff');
        const btnEndTest = document.getElementById('btnEndTest');
        btnTestCycle.hidden = btnTestOn.hidden = btnTestOff.hidden = btnEndTest.hidden = !testMode;
        btnTestCycle.disabled = (!testMode || testMode === 'cycle');
        btnTestOn.disabled = (!testMode || testMode === 'fullOn');
        btnTestOff.disabled = (!testMode || testMode === 'fullOff');
        btnEndTest.disabled = (!testMode || testMode === 'testReady');
    }
}

function resetNode(id) {
    const path = `/rest/nodes/action?id=${id}&action=reset`;
    BaseItem._send(path);
}

function testNode(id, mode) {
    const path = `/rest/nodes/action?id=${id}&action=test&mode=${mode}`;
    BaseItem._send(path);
}