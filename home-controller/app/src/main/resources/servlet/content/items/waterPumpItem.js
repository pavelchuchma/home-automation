'use strict';

class WaterPumpItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.on = undefined;
        this.recCount = undefined;
        this.lastPeriodRecCount = undefined;
        this.lastRecords = undefined;

        document.getElementById('pumpCanvas').addEventListener("click", (function () {
            this.onClick();
        }).bind(this));
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
        let y = 0;
        const step = 17;

        ctx.fillText(' ⟳ ' + this.lastPeriodRecCount + '/' + this.recCount, 5, y += step);
        let lastRecordDuration = -1;
        if (this.lastRecords !== undefined && this.lastRecords.length > 0) {
            lastRecordDuration = this.lastRecords[this.lastRecords.length - 1].duration;
        }
        let lastDuration = Math.round(lastRecordDuration * 10.0) / 10.0;
        if (lastDuration < 15) {
            ctx.fillStyle = 'red';
        }
        ctx.fillText('⏱ ' + lastDuration + ' s', 5, y += step);
    }

    onClick() {
        window.location = '/nodes'
    }
}