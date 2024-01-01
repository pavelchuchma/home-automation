'use strict';

class InverterItem extends AdditionalToolItem {
    constructor() {
        super('inverter', 135)
        this.mode = undefined;
        this.pvPwr = undefined;
        this.acPwr = undefined;
        this.feedInPwr = undefined;
        this.load = undefined;
        this.diff = undefined;
        this.batPwr = undefined;
        this.batSoc = undefined;
        this.yieldToday = undefined;
        this.consumedToday = undefined;
        this.feedInToday = undefined;
    }

    update(item) {
        this.mode = item.mode;
        this.pvPwr = item.pvPwr;
        this.acPwr = item.acPwr;
        this.feedInPwr = item.feedInPwr;
        this.load = item.load;
        this.diff = item.diff;
        this.batPwr = item.batPwr;
        this.batSoc = item.batSoc;
        this.yieldToday = item.yieldToday;
        this.consumedToday = item.consumedToday;
        this.feedInToday = item.feedInToday;
    }

    draw() {
        const ctx = prepareCanvasContext(this.canvasId);

        if (this.mode !== undefined) {
            ctx.font = "12px Arial";
            ctx.fillStyle = 'black';
            let y = 0;
            const step = 17;
            let sunText = (this.pvPwr === 0)
                ? '☁' : ((this.pvPwr < 1000) ? '🌥' : (this.pvPwr < 3500) ? '🌤' : '😎') + ' ' + this.pvPwr + ' W';
            ctx.fillText(sunText, 5, y += step);
            if (this.mode === 'EPSMode') {
                ctx.fillText('🏡 ❌ 🏭 ', 5, y += step);
            } else {
                if (this.feedInPwr > 0) {
                    ctx.fillText('🏡 ▶ 🏭 ' + this.feedInPwr + ' W', 5, y += step);
                } else {
                    ctx.fillText('🏡 ◀ 🏭 ' + -this.feedInPwr + ' W', 5, y += step);
                }
            }
            let batteryText = '🔋 ' + this.batSoc + '%';
            if (this.batPwr !== 0) {
                batteryText += ((this.batPwr > 0) ? " ▲" : " ▼") + Math.abs(this.batPwr) + ' W';
            }
            ctx.fillText(batteryText, 5, y += step);
            ctx.fillText('💡 ' + this.load + ' W', 5, y += step);
            y += 5;
            ctx.fillText('∑😎 ' + this.yieldToday + ' kWh', 5, y += step);
            ctx.fillText('∑◀🏭 ' + this.consumedToday + ' kWh', 5, y += step);
            ctx.fillText('∑▶🏭 ' + this.feedInToday + ' kWh', 5, y += step);
        } else {
            ctx.fillStyle = 'black';
            ctx.font = "30px Arial";
            ctx.fillText('?', 15, 30);
        }
    }
}
