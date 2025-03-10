'use strict';

class LouversItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.pos = undefined;
        this.off = undefined;
        this.act = undefined;
    }

    draw(ctx) {
        LouversItem.drawIcon(this.x, this.y, this.pos, this.off, this.act, ctx, 70, 80);
    }

    static drawIcon(x, y, position, offset, action, ctx, w, h) {
        const louverHeight = 7;
        // white rectangle
        ctx.beginPath();
        ctx.rect(x - w / 2, y - h / 2, w, h);
        ctx.fillStyle = 'white';
        ctx.fill();
        ctx.strokeStyle = 'black';
        ctx.lineWidth = 2;
        ctx.stroke();

        if (position >= 0) {
            // louvers background box
            ctx.beginPath();
            ctx.rect(x - w / 2, y - h / 2, w, h * position);
            ctx.fillStyle = 'lightgray';
            ctx.fill();

            ctx.strokeStyle = 'black';
            ctx.lineWidth = 2;
            ctx.stroke();

            // louvers
            const lineWidth = louverHeight * offset;
            ctx.beginPath();
            for (let i = h * position - lineWidth / 2; i >= lineWidth / 2; i -= louverHeight) {
                const yy = y - h / 2 + i;
                ctx.moveTo(x - w / 2, yy);
                ctx.lineTo(x + w / 2, yy);
            }
            ctx.strokeStyle = 'black';
            ctx.lineWidth = lineWidth;
            ctx.stroke();
        } else {
            ctx.beginPath();
            ctx.font = h + "px Arial Bold";
            ctx.fillStyle = 'black';
            ctx.textAlign = "center";
            ctx.textBaseline = 'middle';
            ctx.fillText("?", x, y);
            ctx.stroke();
        }
    }

    doAction(action) {
        const path = `/rest/louvers/action?id=${this.id}&action=${action}`;
        BaseItem._send(path);
    }
}