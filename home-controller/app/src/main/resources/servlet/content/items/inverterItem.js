'use strict';

class InverterItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
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
    }

    draw() {
        const ctx = getCanvasContext("inverterCanvas");

        if (this.mode !== undefined) {
            ctx.font = "12px Arial";
            ctx.fillStyle = 'black';
            let y = 0;
            const step = 17;

            let sunChar = (this.pvPwr === 0) ? '☁' : (this.pvPwr < 1000) ? '🌥' : (this.pvPwr < 3500) ? '🌤' : '😎';
            ctx.fillText(sunChar + ' ' + this.pvPwr + ' W', 5, y += step);
            if (this.feedInPwr > 0) {
                ctx.fillText('🏡 ▶ 🏭 ' + this.feedInPwr + ' W', 5, y += step);
            } else {
                ctx.fillText('🏡 ◀ 🏭 ' + -this.feedInPwr + ' W', 5, y += step);
            }
            // ctx.fillText('⚡ ' + this.acPwr + ' W', 5, y += step);
            ctx.fillText('🔋 ' + this.batSoc + '% ' + this.batPwr + ' W', 5, y += step);
            ctx.fillText('💡 ' + this.load + ' W', 5, y += step);
            y += 5;
            ctx.fillText('∑😎 ' + this.yieldToday + ' kW/h', 5, y += step);
            ctx.fillText('∑🏭 ' + this.consumedToday + ' kW/h', 5, y += step);
        } else {
            ctx.fillStyle = 'black';
            ctx.font = "30px Arial";
            ctx.fillText('?', 15, 30);
        }
    }
}
