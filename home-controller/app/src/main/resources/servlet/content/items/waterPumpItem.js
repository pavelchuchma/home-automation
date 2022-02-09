'use strict';

class WaterPumpItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.on = undefined;
        this.recCount = undefined;
        this.lastPeriodRecCount = undefined;
        this.lastRecords = undefined;

    }

    update(item) {
        this.on = item.on;
        this.recCount = item.recCount;
        this.lastPeriodRecCount = item.lastPeriodRecCount;
        this.lastRecords = item.lastRecords;
    }

    draw() {
        const ctx = getCanvasContext('pumpCanvas');

        ctx.fillStyle = 'black';
        ctx.font = "12px Arial";
        ctx.fillText('Cykly: ' + this.lastPeriodRecCount + '/' + this.recCount, 5, 20);
        let lastRecordDuration = -1;
        if (this.lastRecords !== undefined && this.lastRecords.length > 0) {
            lastRecordDuration = this.lastRecords[this.lastRecords.length - 1].duration;
        }
        ctx.fillText('Poslední: ' + Math.round(lastRecordDuration * 10.0) / 10.0 + ' s', 5, 35);
    }
}