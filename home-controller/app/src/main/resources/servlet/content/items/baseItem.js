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
}