'use strict';

class FloorHeaterItem extends OnOffItem {
    constructor(id) {
        super(id, 0, 0, -1)
    }

    doAction(action) {
        BaseItem._send(`/rest/onOff/action?id=${this.id}&action=${action}`);
    }
}
