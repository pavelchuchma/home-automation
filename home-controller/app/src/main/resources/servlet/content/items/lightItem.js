'use strict';

class LightItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }

    draw(ctx) {
        PwmLightItem.drawLightIcon(this.x, this.y, this.val, ctx);
    }

    doAction(action) {
        BaseItem._send(`/rest/onOff/action?id=${this.id}&action=${this.simplifyOnOffAction(action)}`);
    }
}