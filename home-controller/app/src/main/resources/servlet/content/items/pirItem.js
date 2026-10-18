'use strict';

class PirItem extends SensorItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }

    draw(svg) {
        const r = 7;
        drawPieSlice(svg, this.x, this.y, r, 0, 2 * Math.PI).fill('orange').stroke({width: 1, color: 'orange'});

        const maxAge = 60.0;

        if (this.age >= 0 && this.age < maxAge) {
            const startAngle = (1.5 - 2 * (1 - this.age / maxAge)) * Math.PI;
            drawPieSlice(svg, this.x, this.y, r, startAngle, 1.5 * Math.PI).fill('red').stroke({width: 1, color: 'red'});
        }
    }
}