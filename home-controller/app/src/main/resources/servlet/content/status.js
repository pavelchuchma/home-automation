'use strict';

class Status {
    constructor(statusRefreshPath, refreshIntervalMs, onRefreshFunction, components, baseUrl, factoryMethod) {
        this.statusRefreshPath = statusRefreshPath;
        this.refreshIntervalMs = refreshIntervalMs;
        this.onRefreshFunction = onRefreshFunction;
        // keep ordered list of components for drawing order to define possible overlays
        this.components = components;
        this.baseUrl = baseUrl;
        this.componentMap = new Map();
        this.factoryMethod = factoryMethod;

        if (components !== undefined) {
            for (const item of components) {
                this.componentMap.set(item.id, item);
            }
        }
    }

    startRefresh() {
        this._refreshImpl();

        setInterval((function () {
            this._refreshImpl();
        }).bind(this), this.refreshIntervalMs);
    }

    _refreshImpl() {
        const request = new XMLHttpRequest();
        request.open('GET', this.baseUrl + this.statusRefreshPath, true);
        request.onreadystatechange = (function () {
            if (request.readyState === 4 && request.status === 200) {
                try {
                    const content = JSON.parse(request.responseText);
                    for (const [type, items] of Object.entries(content)) {
                        for (const item of items) {
                            let c = this.componentMap.get(item.id);
                            if (c === undefined && this.factoryMethod !== undefined) {
                                c = this.factoryMethod(item.id);
                                this.componentMap.set(item.id, c)
                            }
                            if (c !== undefined) {
                                c.update(item);
                            }
                        }
                    }
                    this.onRefreshFunction();
                } catch (e) {
                    printException(e);
                }
            }
        }).bind(this);
        request.send();
    }

    doAction(id, action) {
        const item = this.componentMap.get(id);
        item.doAction(action);
    }
}
