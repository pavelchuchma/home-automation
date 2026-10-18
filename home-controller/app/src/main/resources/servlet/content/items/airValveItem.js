'use strict';

class AirValveItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.pos = undefined;
        this.act = undefined;
    }

    draw(svg) {
        AirValveItem.drawIcon(svg, this.x, this.y, this.pos, this.#getValveColor());
    }

    static drawIcon(parent, x, y, pos, color) {
        const r = 17;
        parent.rect(2 * r - 2, 2 * r - 2).move(x - r + 1, y - r + 1).fill('lightgray').stroke({width: 1, color: 'lightgray'});

        // lines around
        const lineStroke = {width: 5, color: color};
        parent.line(x - r, y - r + 2, x + r, y - r + 2).stroke(lineStroke);
        parent.line(x - r, y + r - 2, x + r, y + r - 2).stroke(lineStroke);

        // valve core
        if (pos >= 0) {
            const angle = Math.PI * pos / -2;
            const dx = (r - 5) * Math.cos(angle);
            const dy = (r - 5) * Math.sin(angle);
            parent.circle(7).center(x, y).fill(color);
            parent.line(x - dx, y - dy, x + dx, y + dy).stroke({width: 3, color: color});
        } else {
            drawCenteredText(parent, '?', x, y, 1.5 * r, color);
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
        const path = `/rest/airValves/action?id=${this.id}&action=${(this.#getValveState() === 0) ? 'close' : 'open'}`;
        BaseItem._send(path);
    }
}