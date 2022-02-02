'use strict';

class PwmLightItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.val = undefined;
        this.maxVal = undefined;
        this.curr = undefined;
    }

    update(item) {
        this.name = item.name;
        this.val = item.val;
        this.maxVal = item.maxVal;
        this.curr = item.curr;
    }

    draw(ctx) {
        const power = this.val / this.maxVal;
        PwmLightItem.drawIcon(this.x, this.y, power, ctx);
    }

    static drawIcon(x, y, power, ctx) {
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
        const newValue = this.#getNewValue(action);
        const path = `/rest/pwmLights/action?id=${this.id}&val=${newValue}`;
        this._send(path);
    }

    #getNewValue(action) {
        const step = 15;
        const currentValue = Math.round(this.val / this.maxVal * 100);
        switch (action) {
            case 'toggle':
                return (currentValue === 0) ? 75 : 0;
            case 'plus':
                return (currentValue === 0) ? TOOL_LIGHT_PLUS_VALUE : Math.min(100, currentValue + step);
            case 'minus':
                return (currentValue === 0) ? 1 : Math.max(0, currentValue - step);
            case 'full' :
                return 100;
            case 'off':
                return 0;
        }
        console.log('Unknown action: ' + action);
    }
}