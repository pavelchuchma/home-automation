'use strict';

class AirValveItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.pos = undefined;
        this.act = undefined;
    }

    update(item) {
        this.name = item.name;
        this.pos = item.pos;
        this.act = item.act;
    }

    draw(ctx) {
        AirValveItem.drawIcon(this.x, this.y, this.pos, this.act, this.#getValveColor(), ctx)
    }

    static drawIcon(x, y, pos, act, color, ctx) {
        const angle = Math.PI * pos / -2;
        ctx.beginPath();
        const r = 17;

        // background rectangle
        ctx.rect(x - r + 1, y - r + 1, 2 * r - 2, 2 * r - 2);
        ctx.strokeStyle = ctx.fillStyle = 'lightgray';
        ctx.lineWidth = 1;
        ctx.fill();
        ctx.stroke();

        // lines around
        ctx.beginPath();
        ctx.moveTo(x - r, y - r + 2);
        ctx.lineTo(x + r, y - r + 2);

        ctx.moveTo(x - r, y + r - 2);
        ctx.lineTo(x + r, y + r - 2);

        ctx.strokeStyle = color;
        ctx.lineWidth = 5;
        ctx.stroke();

        // valve core
        if (pos !== -1) {
            ctx.beginPath();

            ctx.moveTo(x, y);
            ctx.arc(x, y, 1, 0, 2 * Math.PI);
            ctx.lineWidth = 5;
            ctx.stroke();

            ctx.moveTo(x, y);
            ctx.arc(x, y, r - 5, angle, angle);
            ctx.moveTo(x, y);
            ctx.arc(x, y, r - 5, angle + Math.PI, angle + Math.PI);
            ctx.lineWidth = 3;
            ctx.stroke();
        } else {
            ctx.beginPath();
            ctx.font = 1.5 * r + "px Arial Bold";
            ctx.fillStyle = color;
            ctx.textAlign = "center";
            ctx.textBaseline = 'middle';
            ctx.fillText("?", x, y);
            ctx.stroke();
        }
    }

    #getValveColor() {
        switch (this.#getValveState()) {
            case 0:
                return 'green';
            case 1:
                return 'red';
            default:
                return 'gray';
        }
    }

    #getValveState() {
        switch (this.act) {
            case 'stopped':
                return (this.pos === -1) ? -1 : (this.pos === 1) ? 1 : 0;
            case 'movingUp':
                return 0;
            case 'movingDown':
                return 1;
        }
    }

    doAction(action) {
        if (action !== 'toggle') {
            console.log('Unknown action: ' + action);
            return;
        }
        const newValue = (this.#getValveState() === 0) ? 100 : 0;
        const path = `/rest/airValves/action?id=${this.id}&val=${newValue}`;
        this._send(path);
    }
}