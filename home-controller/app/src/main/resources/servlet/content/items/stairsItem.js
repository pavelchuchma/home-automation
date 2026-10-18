'use strict';

class StairsItem extends BaseItem {
    constructor(id, x, y, floor, targetFloor, iconChar) {
        super(id, x, y, floor)
        this.targetFloor = targetFloor;
        this.iconChar = iconChar;
    }

    draw(svg) {
        const w = 70;
        const h = 80;
        svg.rect(w, h).move(this.x - w / 2, this.y - h / 2).fill('white').stroke({width: 2, color: 'black'});
        drawCenteredText(svg, this.iconChar, this.x, this.y, h - 20, 'black');
    }
}