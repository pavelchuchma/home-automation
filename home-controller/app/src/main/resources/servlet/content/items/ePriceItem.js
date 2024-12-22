'use strict';

class EPriceItem extends AdditionalToolItem {
    constructor() {
        super('eprice', 95)
        this.dist = undefined;
        this.currentEntry = undefined;
        this.data = undefined;
    }

    update(item) {
        this.dist = item.dist;
        this.currentEntry = item.currentEntry;
        this.data = item.data;
    }

    getY(price, minPrice, maxPrice) {
        let graphHeight = 60;
        let bottomY = Math.min(0, minPrice);
        let yPartCount = maxPrice - bottomY;

        return 10 + graphHeight - graphHeight / yPartCount * (price - bottomY);
    }

    draw() {
        const ctx = prepareCanvasContext(this.canvasId);

        if (this.data !== undefined) {
            let maxPrice = Math.round(Math.max(...this.data) + 0.5)
            let minPrice = Math.round(Math.min(...this.data) - 0.5)
            let xStart = (110 - 2 * 47) / 2;

            // hours
            ctx.strokeStyle = 'gray';
            ctx.lineWidth = 1;
            for (let i = 0; i <= 48; i += 12) {
                ctx.beginPath();
                ctx.moveTo(xStart + 2 * i, 10, maxPrice);
                ctx.lineTo(xStart + 2 * i, 70);
                ctx.stroke();
            }

            // zero value
            for (let i = Math.min(0, minPrice); i <= maxPrice; i++) {
                ctx.strokeStyle = (i === 0) ? 'black' : 'gray';
                ctx.lineWidth = 1;
                ctx.beginPath();
                let y = this.getY(i, minPrice, maxPrice);
                ctx.moveTo(xStart, y);
                ctx.lineTo(xStart + 2 * 48, y);
                ctx.stroke();
            }

            ctx.lineWidth = 2;
            for (let i = 0; i <= 48; i++) {
                ctx.strokeStyle = (i === this.currentEntry) ? 'red' : 'blue';
                ctx.beginPath();
                let y = this.getY(this.data[i], minPrice, maxPrice)
                ctx.moveTo(xStart + 2 * i, y);
                ctx.lineTo(xStart + 2 * (i + 1), y);
                ctx.stroke();
            }

            // text
            ctx.font = "12px Arial";
            ctx.fillStyle = 'green';
            ctx.textAlign = 'left'
            ctx.fillText('▼' + this.getPrintablePrice(Math.min(...this.data)), 5, 85);
            ctx.fillStyle = 'black';
            ctx.textAlign = 'center'
            ctx.fillText('▶' + this.getPrintablePrice(this.data[this.currentEntry]), 55, 85);
            ctx.fillStyle = 'red';
            ctx.textAlign = 'right'
            ctx.fillText('▲' + this.getPrintablePrice(Math.max(...this.data)), 105, 85);
        } else {
            ctx.fillStyle = 'black';
            ctx.font = "30px Arial";
            ctx.fillText('?', 15, 30);
        }
    }

    getPrintablePrice(price) {
        return Math.round(price * 10) / 10;
    }
}
