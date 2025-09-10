'use strict';

class Status {
    refreshInProgress = false;

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
        if (this.refreshInProgress) {
            console.error('Refresh inProgress, skipping the refresh...');
            return;
        }
        this.refreshInProgress = true;
        let params = []
        this.componentMap.values().forEach(item => {
            const itemParams = item.getRefreshParams();
            if (itemParams) {
                itemParams.forEach(param => params.push(param[0] + '=' + encodeURIComponent(param[1])));
            }
        });

        const queryString = params.length > 0 ? '?' + params.join('&') : '';
        const request = new XMLHttpRequest();
        request.open('GET', this.baseUrl + this.statusRefreshPath + queryString, true);
        request.onreadystatechange = (function () {
            this.refreshInProgress = false;
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
