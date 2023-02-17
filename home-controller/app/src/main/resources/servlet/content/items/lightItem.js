'use strict';

class LightItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }

    draw(ctx) {
        PwmLightItem.drawLightIcon(this.x, this.y, this.val, ctx);
    }

    doAction(action) {
        if (action === 'plus' || action === 'full') {
            action = 'on';
        } else if (action === 'minus') {
            action = 'off';
        }
        const path = `/rest/onOff/action?id=${this.id}&action=${action}`;
        BaseItem._send(path);
    }
}