'use strict';

class PwmLightItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.pwmVal = undefined;
        this.maxPwmVal = undefined;
        this.curr = undefined;
    }

    draw(ctx) {
        PwmLightItem.drawLightIcon(this.x, this.y, this.val, ctx);
    }

    static drawLightIcon(x, y, power, ctx) {
        // black background
        ctx.strokeStyle = 'black';
        ctx.lineWidth = 1;
        if (power < 1) {
            ctx.beginPath();
            ctx.arc(x, y, 20, 0, 2 * Math.PI);
            ctx.fillStyle = 'black';
            ctx.fill();
        }

        // yellow pie
        ctx.beginPath();
        if (power < 1) {
            ctx.moveTo(x, y);
        }
        const startAngle = 1.5 * Math.PI;
        const endAngle = (1.5 + 2 * power) * Math.PI;
        ctx.arc(x, y, 20, startAngle, endAngle);
        if (power < 1) {
            ctx.lineTo(x, y);
        }
        ctx.fillStyle = 'yellow';
        ctx.fill();
        ctx.stroke();

        // central circle
        if (power > 0 && power < 1) {
            ctx.beginPath();
            ctx.arc(x, y, 7, 0, 2 * Math.PI);
            ctx.fill();
        }
    }

    static drawLightIconSvg(parent, x, y, power) {
        const r = 20;
        parent.circle(2 * r).center(x, y).fill((power >= 1) ? 'yellow' : 'black');
        if (power > 0 && power < 1) {
            const endAngle = (1.5 + 2 * power) * Math.PI;
            const largeArc = (power > .5) ? 1 : 0;
            parent.path(`M ${x} ${y} L ${x} ${y - r} A ${r} ${r} 0 ${largeArc} 1 ${x + r * Math.cos(endAngle)} ${y + r * Math.sin(endAngle)} Z`)
                .fill('yellow');
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