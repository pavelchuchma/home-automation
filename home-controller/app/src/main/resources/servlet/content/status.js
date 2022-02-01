'use strict';

class Status {
    constructor(statusRefreshPath, refreshIntervalMs, onRefreshFunction, components, baseUrl) {
        this.statusRefreshPath = statusRefreshPath;
        this.baseUrl = baseUrl;
        this.componentMap = new Map();

        for (const item of components) {
            this.componentMap.set(item.id, item);
        }

        Status.#refreshImpl(this);

        const t = this;
        setInterval(function () {
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
                            const c = t.componentMap.get(item.id);
                            if (c !== undefined) {
                                c.update(item);
                            }
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
    console.log(e);
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}