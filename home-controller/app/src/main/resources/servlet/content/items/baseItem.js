'use strict';

class BaseItem {
    constructor(id, x, y, floor) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.floor = floor;
    }

    draw(ctx) {
    }

    update(item) {
    }

    doAction(action) {
    }

    _send(path) {
        const request = new XMLHttpRequest();
        request.open('GET', getBaseUrl() + path, false);
        request.send();
    }
}