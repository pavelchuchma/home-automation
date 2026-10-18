'use strict';

class LouversItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.pos = undefined;
        this.off = undefined;
        this.act = undefined;
    }

    draw(svg) {
        LouversItem.drawIcon(svg, this.x, this.y, this.pos, this.off, 70, 80);
    }

    static drawIcon(parent, x, y, position, offset, w, h) {
        const louverHeight = 7;
        const left = x - w / 2;
        const top = y - h / 2;
        const border = {width: 2, color: 'black'};
        parent.rect(w, h).move(left, top).fill('white').stroke(border);

        if (position >= 0) {
            parent.rect(w, h * position).move(left, top).fill('lightgray').stroke(border);

            // zero offset (open louvers) is still drawn as thin lines
            const lineWidth = louverHeight * offset;
            for (let i = h * position - lineWidth / 2; i >= lineWidth / 2; i -= louverHeight) {
                parent.line(left, top + i, left + w, top + i).stroke({width: lineWidth || 2, color: 'black'});
            }
        } else {
            drawCenteredText(parent, '?', x, y, h * .6, 'gray');
        }
    }

    doAction(action) {
        const path = `/rest/louvers/action?id=${this.id}&action=${action}`;
        BaseItem._send(path);
    }
}