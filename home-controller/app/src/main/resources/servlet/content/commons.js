'use strict';

class CoordinateItem {
    constructor(id, x, y, floor, type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.floor = floor;
        this.type = type;
    }
}

class Status {
    constructor(statusRefreshPath, refreshIntervalMs, onRefreshFunction) {
        this.statusRefreshPath = statusRefreshPath;
        this.coords = getComponents();
        this.baseUrl = getBaseUrl();
        this.statusMap = {};
        this.coordsMap = {};

        const t = this;
        Status.#refreshImpl(t);

        this.coords.forEach(function (item) {
            const statusItem = t.statusMap[item.id];
            if (statusItem !== undefined && item.type === undefined) {
                item.type = statusItem.type;
            }
            t.coordsMap[item.id] = item;
        });

        setInterval(function() {
            Status.#refreshImpl(t);
            onRefreshFunction();
        }, refreshIntervalMs);
    }

    static #refreshImpl(t) {
        const request = new XMLHttpRequest();
        request.open('GET', t.baseUrl + t.statusRefreshPath, false);
        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200) {
                try {
                    const content = JSON.parse(request.responseText);
                    for (const [type, items] of Object.entries(content)) {
                        for (const item of items) {
                            item.type = type;
                            t.statusMap[item.id] = item;
                        }
                    }
                } catch (e) {
                    printException(e);
                }
            }
        };
        request.send();
    }

    sendAction(action) {
        //document.getElementById('error').innerHTML = action;
        try {
            const request = new XMLHttpRequest();
            request.open('GET', this.baseUrl + action, true);
            request.send();
        } catch (e) {
            printException(e);
        }
    }
}

function printException(e) {
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}