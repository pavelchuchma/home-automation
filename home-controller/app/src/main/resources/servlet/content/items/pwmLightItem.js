'use strict';

class PwmLightItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.pwmVal = undefined;
        this.maxPwmVal = undefined;
        this.curr = undefined;
    }

    draw(svg) {
        PwmLightItem.drawLightIcon(svg, this.x, this.y, this.val);
    }

    static drawLightIcon(parent, x, y, power) {
        const r = 20;
        parent.circle(2 * r).center(x, y).fill((power >= 1) ? 'yellow' : 'black');
        if (power > 0 && power < 1) {
            drawPieSlice(parent, x, y, r, 1.5 * Math.PI, (1.5 + 2 * power) * Math.PI).fill('yellow');
            parent.circle(14).center(x, y).fill('yellow');
        }
        // single outline on top so the yellow and black parts share the same radius
        parent.circle(2 * r).center(x, y).fill('none').stroke({width: 1, color: 'black'});
    }

    doAction(action) {
        const path = `/rest/pwmLights/action?id=${this.id}&action=${action}`;
        BaseItem._send(path);
    }

    increaseValue(val) {
        const path = `/rest/pwmLights/action?id=${this.id}&action=increase&val=${val}`;
        BaseItem._send(path);
    }
}