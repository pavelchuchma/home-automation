'use strict';

class LightItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }

    draw(svg) {
        PwmLightItem.drawLightIcon(svg, this.x, this.y, this.val);
    }

    doAction(action) {
        BaseItem._send(`/rest/onOff/action?id=${this.id}&action=${this.simplifyOnOffAction(action)}`);
    }
}