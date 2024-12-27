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

    doAction(action) {
        const path = `/rest/pwmLights/action?id=${this.id}&action=${action}`;
        BaseItem._send(path);
    }

    increaseValue(val) {
        const path = `/rest/pwmLights/action?id=${this.id}&action=increase&val=${val}`;
        BaseItem._send(path);
    }
}