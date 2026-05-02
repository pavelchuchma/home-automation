'use strict';

class InverterItem extends AdditionalSvgToolItem {
    constructor() {
        super('inverter', 125)
        this.mode = undefined;
        this.pvPwr = undefined;
        this.acPwr = undefined;
        this.feedInPwr = undefined;
        this.load = undefined;
        this.diff = undefined;
        this.batPwr = undefined;
        this.batSoc = undefined;
        this.yieldToday = undefined;
        this.pvYieldToday = undefined;
        this.consumedToday = undefined;
        this.feedInToday = undefined;
    }

    onCanvasCreatedImpl() {
        this.textLines = [];
        for (let i = 0; i < 7; i++) {
            this.textLines.push(this.svg.text('?').move(5, i * 17).font(this.baseFont));
        }
        this.showOnData.push(...this.textLines);
        this.hideOnNoData.push(...this.textLines);
    }

    drawImpl() {
        this.textLines[0].text((this.mode === 'Waiting') ? "⊘"
            : (this.mode === 'Checking') ? "↻"
                : (this.mode === 'Idle') ? "💤"
                    : (this.pvPwr === 0) ? '☁'
                        : ((this.pvPwr < 1000) ? '🌥'
                            : (this.pvPwr < 3500) ? '🌤'
                                : '😎')
                        + ' ' + this.pvPwr + ' W');
        this.textLines[1].text((this.mode === 'EPSMode') ? '🏡 ❌ 🏭 ' :
            (this.feedInPwr > 0) ? '🏡 ▶ 🏭 ' + this.feedInPwr + ' W' : '🏡 ◀ 🏭 ' + -this.feedInPwr + ' W');

        let batteryText = '🔋 ' + this.batSoc + '%';
        if (this.batPwr !== 0) {
            batteryText += ((this.batPwr > 0) ? " ▲" : " ▼") + Math.abs(this.batPwr) + ' W';
        }
        this.textLines[2].text(batteryText);
        this.textLines[3].text('💡 ' + this.load + ' W');
        this.textLines[4].text('∑😎 ' + this.pvYieldToday + ' kWh');
        this.textLines[5].text('∑ ◀ 🏭 ' + this.consumedToday + ' kWh');
        this.textLines[6].text('∑ ▶ 🏭 ' + this.feedInToday + ' kWh');
    }
}
