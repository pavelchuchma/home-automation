'use strict';

class AbstractItem {
    constructor(id) {
        this.id = id;
    }

    update(item) {
    }

    doAction(action) {
    }
}

class BaseItem extends AbstractItem {
    constructor(id, x, y, floor) {
        super(id);
        this.x = x;
        this.y = y;
        this.floor = floor;
    }

    draw(ctx) {
    }

    static _send(path) {
        const request = new XMLHttpRequest();
        request.open('GET', getBaseUrl() + path, false);
        request.send();
    }
}