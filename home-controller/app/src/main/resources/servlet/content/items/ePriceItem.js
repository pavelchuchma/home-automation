'use strict';

const graphLineCount = 100;
const gridStyle = {width: 1, color: 'gray'};
const priceLineStyle = {width: 2, color: 'blue'};

class EPriceItem extends AdditionalSvgToolItem {
    constructor() {
        super('eprice', 95)
        this.dist = undefined;
        this.currentEntry = undefined;
        this.data = undefined;
    }

    getY(price, bottomY, topY) {
        let yPartCount = topY - bottomY;
        return this.yStart + this.graphHeight - this.graphHeight / yPartCount * (price - bottomY);
    }

    onCanvasCreatedImpl() {
        const legendY = 73;
        const items = [];
        items.push(this.textMinPrice = this.svg.text('?').move(5, legendY).font(this.baseFont).attr('fill', 'green'));
        items.push(this.textCurrentPrice = this.svg.text('?').move(55, legendY).font(this.baseFont).attr('text-anchor', 'middle'));
        items.push(this.textMaxPrice = this.svg.text('?').move(105, legendY).font(this.baseFont).attr('fill', 'red').attr('text-anchor', 'end'));

        this.graphWidth = 100.0;
        this.graphHeight = 65.0;
        this.xStart = (this.canvasWidth - this.graphWidth) / 2;
        this.yStart = 5;

        for (let i = 0; i < 6; i++) {
            items.push(
                this.svg.line(this.xStart + this.graphWidth / 4 * i, this.yStart, this.xStart + this.graphWidth / 4 * i,
                    this.yStart + this.graphHeight).stroke(gridStyle)
            );
        }

        this.horizontalLines = [];
        for (let i = 0; i < graphLineCount; i++) {
            const l = this.svg.line(this.xStart, 0, this.xStart + this.graphWidth, 0).stroke(gridStyle);
            this.horizontalLines.push(l);
        }
        this.priceLineParts = [];
        const step = this.graphWidth / 48;
        for (let i = 0; i < 48; i++) {
            const l = this.svg.line(this.xStart + i * step, 0, this.xStart + (i + 1) * step, 0).stroke(priceLineStyle);
            this.priceLineParts.push(l);
        }

        this.hideOnNoData.push(...items, ...this.horizontalLines, ...this.priceLineParts);
        this.showOnData.push(...items);
    }

    updateHorizontalLine(l, value, bottomY, topY, color) {
        let y = this.getY(value, bottomY, topY);
        l.attr('y1', y).attr('y2', y);
        this.setVisibility(l, true);
        l.attr('stroke', color);
    }

    drawImpl() {
        let maxPrice = Math.round(Math.max(...this.data) + 0.5)
        let minPrice = Math.round(Math.min(...this.data) - 0.5)
        let bottomY = Math.min(0, minPrice);
        this.textMinPrice.text('▼' + this.getPrintablePrice(Math.min(...this.data)));
        this.textCurrentPrice.text('▶' + this.getPrintablePrice(this.data[this.currentEntry]));
        this.textMaxPrice.text('▲' + this.getPrintablePrice(Math.max(...this.data)));
        let lineCount = maxPrice - bottomY + 1
        for (let i = 0; i < lineCount; i++) {
            this.updateHorizontalLine(this.horizontalLines[i], bottomY + i, bottomY, maxPrice, gridStyle['color']);
        }
        for (let i = lineCount; i < graphLineCount; i++) {
            this.setVisibility(this.horizontalLines[i]);
        }
        for (let i = 0; i < 48; i++) {
            let color = (i === this.currentEntry) ? 'red' : 'blue';
            this.updateHorizontalLine(this.priceLineParts[i], this.data[i], bottomY, maxPrice, color);
        }
    }

    getPrintablePrice(price) {
        return Math.round(price * 10) / 10;
    }
}
