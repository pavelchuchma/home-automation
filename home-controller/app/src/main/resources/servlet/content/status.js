'use strict';

class Status {
    constructor(statusRefreshPath, refreshIntervalMs, onRefreshFunction, components, baseUrl) {
        this.statusRefreshPath = statusRefreshPath;
        this.baseUrl = baseUrl;
        this.componentMap = new Map();

        for (const item of components) {
            this.componentMap.set(item.id, item);
        }

        this._refreshImpl();

        setInterval((function () {
            this._refreshImpl();
            onRefreshFunction();
        }).bind(this), refreshIntervalMs);
    }

    _refreshImpl() {
        const request = new XMLHttpRequest();
        request.open('GET', this.baseUrl + this.statusRefreshPath, false);
        request.onreadystatechange = (function () {
            if (request.readyState === 4 && request.status === 200) {
                try {
                    const content = JSON.parse(request.responseText);
                    for (const [type, items] of Object.entries(content)) {
                        for (const item of items) {
                            const c = this.componentMap.get(item.id);
                            if (c !== undefined) {
                                c.update(item);
                            }
                        }
                    }
                } catch (e) {
                    printException(e);
                }
            }
        }).bind(this);
        request.send();
    }
}

function printException(e) {
    console.log(e);
    document.getElementById('error').innerHTML = e.message + '<br>' + e.stack.split('\n').join('<br>');
}