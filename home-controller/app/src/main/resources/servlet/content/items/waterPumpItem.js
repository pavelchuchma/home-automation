'use strict';

class WaterPumpItem extends AdditionalSvgToolItem {
    constructor() {
        super('wpump', 40)
        this.on = undefined;
        this.recCount = undefined;
        this.lastPeriodRecCount = undefined;
        this.lastRecords = undefined;
    }

    onCanvasCreatedImpl() {
        this.textLines = [];
        for (let i = 0; i < 2; i++) {
            this.textLines.push(this.svg.text('?').move(5, i * 17).font(this.baseFont));
        }

        this.showOnData.push(...this.textLines);
        this.hideOnNoData.push(...this.textLines);
    }

    hasData() {
        return this.lastRecords !== undefined && this.lastRecords.length > 0;
    }

    drawImpl() {
        this.textLines[0].text('⟳ ' + this.lastPeriodRecCount + '/' + this.recCount);

        let lastRecordDuration = -1;
        if (this.lastRecords !== undefined && this.lastRecords.length > 0) {
            lastRecordDuration = this.lastRecords[this.lastRecords.length - 1].duration;
        }
        let lastDuration = Math.round(lastRecordDuration * 10.0) / 10.0;
        this.textLines[1].text('⏱ ' + lastDuration + ' s').attr('fill', (lastDuration < 15) ? 'red' : 'black');
    }
}