'use strict';

const INVERTER_SETTINGS = {
    title: 'Inverter settings',
    lanes: [
        {key: 'inverter.manager.high.tariff.times', label: 'High tariff', color: '#c0392b'},
        {key: 'inverter.manager.export.priority.times', label: 'Feed-in priority', color: '#2e6fb7'},
    ],
    groups: [
        {title: 'Battery', fields: [
            {type: 'stepper', key: 'inverter.manager.minimal.soc', label: 'Minimal SoC', hint: 'Battery never discharges below this',
                step: 5, min: 0, max: 100, unit: '%'},
        ]},
        {title: 'High tariff', fields: [
            {type: 'ranges', key: 'inverter.manager.high.tariff.times'},
            {type: 'stepper', key: 'inverter.manager.high.tariff.battery.reserve', label: 'Reserve',
                hint: 'Battery kept for the high tariff hours', step: 5, min: 0, max: 100, unit: '%'},
        ]},
        {title: 'Feed-in priority timer', toggle: 'inverter.manager.export.priority.timer.enabled',
            hint: 'FeedInPriority in these ranges, SelfUse outside', fields: [
                {type: 'ranges', key: 'inverter.manager.export.priority.times'},
            ]},
        {title: 'Force discharge', toggle: 'inverter.manager.force.discharge',
            hint: 'Sell to the grid in the most expensive segments of the day', fields: [
                {type: 'stepper', key: 'inverter.manager.force.discharge.minimal.price', label: 'Minimal net price',
                    hint: 'CZK/kWh after fees, incl. VAT', step: 0.5, min: 0, max: 50, unit: '', decimals: 1},
                {type: 'stepper', key: 'inverter.manager.force.discharge.minimal.soc', label: 'Discharge down to',
                    step: 5, min: 0, max: 100, unit: '%'},
            ]},
    ],
};

class InverterItem extends AdditionalSvgToolItem {
    constructor() {
        super('inverter', 125)
        this.mode = undefined;
        this.batMode = undefined;
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
        this.batModeIcon = this.svg.text('?').move(74, 0).font(this.baseFont);
        this.createSettingsIcon(INVERTER_SETTINGS);
        this.showOnData.push(...this.textLines, this.batModeIcon);
        this.hideOnNoData.push(...this.textLines, this.batModeIcon);
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

        this.batModeIcon.text(
            (this.batMode === 'SelfUse') ? '🏡' :
            (this.batMode === 'FeedInPriority') ? '🏭' :
            (this.batMode === 'BackUp') ? '🔋' :
            (this.batMode === 'Manual') ? '✋' : '?');
    }
}
