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

    afterAction(ctx) {
        // draw changed item as gray
        ctx.beginPath();
        ctx.arc(this.x, this.y, 15, 0, 2 * Math.PI);
        ctx.fillStyle = 'gray';
        ctx.fill();
        ctx.stroke();
    }

}