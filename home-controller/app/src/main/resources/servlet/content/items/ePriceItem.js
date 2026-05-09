'use strict';


class EPriceItem extends AdditionalSvgToolItem {
    data = {
        distFee: undefined,
        sellFee: undefined,
        now: undefined,
        values: [{
            time: undefined,
            price: undefined
        }],
    };
    graphLineCount = 100;
    gridStyle = {width: .5, color: 'gray'};
    boundaryLineStyle = {width: .5, color: 'gray'};
    twelveHourGridStyle = {width: .5, color: 'gray'};
    threeHourGridStyle = {width: .2, color: 'gray'};
    hourGridStyle = {width: .1, color: 'gray'};
    priceLineStyle = {width: 1, color: 'blue'};

    constructor() {
        super('eprice', 95)
    }

    updateToDataProperty() {
        return true;
    }

    getY(price) {
        let yPartCount = this.topYPrice - this.bottomYPrice;
        return this.yStart + this.graphHeight - this.graphHeight / yPartCount * (price - this.bottomYPrice);
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
        this.fortyEightHourItems = [];
        this.fortyNinthHourItems = [];
        this.currentChartHourCount = 0;

        // negative sell zone
        this.negativeSellZone = this.svg.rect(this.graphWidth, this.graphHeight).move(this.xStart, this.yStart).fill('gray').opacity(0.3);
        // negative zone
        this.negativeBuyZone = this.svg.rect(this.graphWidth, this.graphHeight).move(this.xStart, this.yStart).fill('red').opacity(0.3);

        // boundary
        items.push(
            this.svg.line(this.xStart, this.yStart, this.xStart, this.yStart + this.graphHeight).stroke(this.boundaryLineStyle),
            this.svg.line(this.xStart + this.graphWidth, this.yStart, this.xStart + this.graphWidth, this.yStart + this.graphHeight).stroke(this.boundaryLineStyle)
        );

        // hour lines
        this.hourLines = [];
        for (let i = 0; i < 48; i++) {
            const l = this.svg.line(0, this.yStart, 0, this.yStart + this.graphHeight);
            this.hourLines.push(l);
        }
        this.fortyEightHourItems.push(this.hourLines[46]);
        this.fortyNinthHourItems.push(this.hourLines[47]);

        // horizontal lines
        this.horizontalLines = [];
        for (let i = 0; i < this.graphLineCount; i++) {
            const l = this.svg.line(this.xStart, 0, this.xStart + this.graphWidth, 0).stroke(this.gridStyle);
            this.horizontalLines.push(l);
        }

        items.push(this.currentPoint = this.svg.circle(3).attr({fill: 'red'}));

        // price line parts
        this.priceLineParts = [];
        for (let i = 0; i < (49 * 4); i++) {
            this.priceLineParts.push(this.svg.line(0, 0, 0, 1).stroke(this.priceLineStyle));
        }
        this.fortyEightHourItems.push(...this.priceLineParts.slice(47 * 4, 48 * 4));
        this.fortyNinthHourItems.push(...this.priceLineParts.slice(48 * 4, 49 * 4));

        this.hideOnNoData.push(items, this.horizontalLines, this.priceLineParts, this.hourLines, this.negativeSellZone, this.negativeBuyZone);
        this.showOnData.push(items, this.horizontalLines, this.priceLineParts.slice(0, 47 * 4), this.hourLines.slice(0, 46), this.negativeSellZone, this.negativeBuyZone);
    }

    updateHorizontalLine(l, value) {
        let y = this.getY(value);
        l.attr('y1', y).attr('y2', y);
        this.setVisibility(l, true);
    }

    getXForTime(time) {
        return this.xStart + this.graphWidth / (this.data.values.length / 4) * (time - this.data.values[0].time) / 1000 / 60 / 60;
    }

    updateHourLines() {
        const hourCount = this.data.values.length / 4;
        if (this.currentChartHourCount === hourCount) {
            // no change needed
            return;
        }
        // set a proper count of hours
        this.showOnData = this.showOnData.filter(item => item !== this.fortyEightHourItems && item !== this.fortyNinthHourItems);
        if (hourCount >= 48) {
            this.showOnData.push(this.fortyEightHourItems);
        }
        if (hourCount >= 49) {
            this.showOnData.push(this.fortyNinthHourItems);
        }
        this.setVisibility(this.fortyEightHourItems, hourCount >= 48);
        this.setVisibility(this.fortyNinthHourItems, hourCount >= 49);

        for (let h = 0; h < hourCount - 1; h++) {
            const t = this.data.values[0].time + (h + 1) * 3_600_000;
            const x = this.getXForTime(t);
            const serverHour = (h + 1) % 24;
            this.hourLines[h]
                .attr('x1', x)
                .attr('x2', x)
                .attr('id', 'hourLine' + h)
                .stroke((serverHour % 12 === 0) ? this.twelveHourGridStyle : (serverHour % 3 === 0) ? this.threeHourGridStyle : this.hourGridStyle);
        }

        // price line parts
        const step = this.graphWidth / this.data.values.length;
        for (let i = 0; i < this.data.values.length; i++) {
            this.priceLineParts[i]
                .attr('x1', this.xStart + i * step)
                .attr('x2', this.xStart + (i + 1) * step);
        }
        this.currentChartHourCount = hourCount;
    }

    drawImpl() {
        const now = this.data.now;
        const maxPrice = Math.max(...this.data.values.map(v => v.price));
        const minPrice = Math.min(...this.data.values.map(v => v.price));
        this.topYPrice = Math.round(maxPrice + 0.5);
        this.bottomYPrice = Math.min(0, Math.round((minPrice) - 0.5));
        const currentEntryIndex = this.data.values.findIndex(v => {
            const timeDiff = now - v.time;
            return timeDiff >= 0 && timeDiff < 15 * 60_000;
        });

        this.textMinPrice.text('▼' + this.getPrintablePrice(minPrice));
        this.textCurrentPrice.text('▶' + this.getPrintablePrice(this.data.values[currentEntryIndex].price));
        this.textMaxPrice.text('▲' + this.getPrintablePrice(maxPrice));

        // horizontal lines
        let lineCount = this.topYPrice - this.bottomYPrice + 1
        for (let i = 0; i < lineCount; i++) {
            this.updateHorizontalLine(this.horizontalLines[i], this.bottomYPrice + i, this.gridStyle['color']);
        }
        this.setVisibility(this.horizontalLines.slice(lineCount), false);

        // price line
        for (let i = 0; i < this.data.values.length; i++) {
            this.updateHorizontalLine(this.priceLineParts[i], this.data.values[i].price);
        }
        this.currentPoint.attr({
            cx: this.getXForTime(now),
            cy: this.getY(this.data.values[currentEntryIndex].price),
        });

        // negative sell and buy zones
        const bottomY = this.getY(this.bottomYPrice);
        const zeroY = this.getY(0);
        const negativeSellZoneY = this.getY(this.data.distFee + this.data.sellFee);
        this.negativeSellZone.y(negativeSellZoneY).height(Math.min(bottomY, zeroY) - negativeSellZoneY);
        this.setVisibility(this.negativeBuyZone, bottomY > zeroY);
        if (bottomY > zeroY) {
            this.negativeBuyZone.y(zeroY).height(bottomY - zeroY);
        }
        this.updateHourLines();
    }

    getPrintablePrice(price) {
        return Math.round(price * 10) / 10;
    }
}
