'use strict';

class SensorItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.active = undefined;
        this.age = undefined;
    }

    update(item) {
        this.name = item.name;
        this.active = item.active;
        this.age = item.age;
    }
}