'use strict';

class StairsItem extends BaseItem {
    constructor(id, x, y, floor, targetFloor, iconChar) {
        super(id, x, y, floor)
        this.targetFloor = targetFloor;
        this.iconChar = iconChar;
    }

    draw(ctx) {
        const w = 70;
        const h = 80;

        // white rectangle
        ctx.beginPath();
        ctx.rect(this.x - w / 2, this.y - h / 2, w, h);
        ctx.fillStyle = 'white';
        ctx.fill();
        ctx.strokeStyle = 'black';
        ctx.lineWidth = 2;
        ctx.stroke();

        ctx.beginPath();
        ctx.font = h - 20 + "px Arial Bold";
        ctx.fillStyle = 'black';
        ctx.textAlign = "center";
        ctx.textBaseline = 'middle';
        ctx.fillText(this.iconChar, this.x, this.y);
        ctx.stroke();
    }
}