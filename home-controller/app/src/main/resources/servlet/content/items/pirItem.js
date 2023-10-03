'use strict';

class PirItem extends SensorItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }

    draw(ctx) {
        ctx.strokeStyle = 'orange';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.arc(this.x, this.y, 7, 0, 2 * Math.PI);
        ctx.fillStyle = 'orange';
        ctx.fill();
        ctx.stroke();

        const maxAge = 60.0;

        if (this.age >= 0 && this.age < maxAge) {
            const startAngle = (1.5 - 2 * (1 - this.age / maxAge)) * Math.PI;
            const endAngle = 1.5 * Math.PI;

            ctx.beginPath();
            ctx.strokeStyle = 'red';
            ctx.moveTo(this.x, this.y);
            ctx.arc(this.x, this.y, 7, startAngle, endAngle);
            ctx.lineTo(this.x, this.y);
            ctx.fillStyle = 'red';
            ctx.fill();
            ctx.stroke();
        }
    }
}