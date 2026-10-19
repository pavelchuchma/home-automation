'use strict';

const PHASE_VOLTAGE = 230;
const PHASE_WARN_RATIO = 0.8;
const MANUAL_MODE_LABELS = {Stop: 'Stop', ForceCharge: 'Force charge', ForceDischarge: 'Force discharge'};

// dialog keys written to the inverter by the REST action, see InverterHandler; the rest are options
const INVERTER_DEVICE_KEYS = ['pgridBias'];

const INVERTER_STORE = {
    load: (keys, item) => SettingsSheet.OPTIONS_STORE.load(keys.filter(k => !INVERTER_DEVICE_KEYS.includes(k)))
        .then(values => ({...values, pgridBias: String(item.pgridBias !== 'Disable')})),
    save: (changes, item) => {
        const device = {};
        const options = {};
        Object.entries(changes).forEach(([k, v]) => (INVERTER_DEVICE_KEYS.includes(k) ? device : options)[k] = v);
        return Promise.all([
            Object.keys(device).length ? fetch(`${getBaseUrl()}/rest/inverter/action?id=${item.id}&${new URLSearchParams(device)}`)
                .then(SettingsSheet.checkResponse) : null,
            Object.keys(options).length ? SettingsSheet.OPTIONS_STORE.save(options) : null,
        ]);
    },
};

const INVERTER_SETTINGS = {
    title: 'Inverter',
    store: INVERTER_STORE,
    details: {optionKeys: ['inverter.phase.breaker.amps'], render: inverterDetailsHtml},
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
        {title: 'Grid', fields: [
            {type: 'switch', key: 'pgridBias', label: 'Pgrid bias',
                hint: 'Keep ~40 W flowing to the grid instead of ~0 W, so the house never draws from it'},
        ]},
    ],
};

class InverterItem extends AdditionalSvgToolItem {
    constructor() {
        super('inverter', 125)
        this.mode = undefined;
        this.batMode = undefined;
        this.manualMode = undefined;
        this.pgridBias = undefined;
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
        for (let i = 1; i <= 3; i++) {
            this['load' + i] = undefined;
            this['feedIn' + i] = undefined;
        }
    }

    onCanvasCreatedImpl() {
        this.textLines = [];
        for (let i = 0; i < 7; i++) {
            this.textLines.push(this.svg.text('?').move(5, i * 17).font(this.baseFont));
        }
        this.batModeIcon = this.svg.text('?').move(this.canvasWidth - 17, 0).font(this.baseFont);
        this.openSettingsOnClick(INVERTER_SETTINGS);
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

function formatWatts(w) {
    return Math.round(w).toLocaleString('cs-CZ') + ' W';
}

/**
 * Grid meter power signed as export (+) or import (−).
 */
function gridCellHtml(w, cls) {
    const dir = w >= 0 ? 'settings-export' : 'settings-import';
    return `<div class="settings-num ${dir} ${cls}">${w >= 0 ? '+' : '−'}${formatWatts(Math.abs(w))}</div>`;
}

/**
 * Details tab: modes and the house load per phase, bars scaled to the main breaker.
 */
function inverterDetailsHtml(item, options) {
    const maxW = Number(options['inverter.phase.breaker.amps']) * PHASE_VOLTAGE;
    const manual = item.batMode === 'Manual';
    const manualLabel = MANUAL_MODE_LABELS[item.manualMode] || item.manualMode;
    let rows = '';
    let totalLoad = 0;
    let totalGrid = 0;
    for (let i = 1; i <= 3; i++) {
        const load = item['load' + i];
        const grid = item['feedIn' + i];
        totalLoad += load;
        totalGrid += grid;
        const ratio = maxW > 0 ? Math.max(0, load) / maxW : 0;
        rows += `<div class="settings-value">L${i}</div>
            <div class="settings-phase-load">
                <span class="settings-bar${ratio > PHASE_WARN_RATIO ? ' warn' : ''}" aria-hidden="true"><i style="width:${Math.min(100, ratio * 100)}%"></i></span>
                <span class="settings-value">${formatWatts(load)}</span></div>
            ${gridCellHtml(grid, '')}`;
    }
    const scale = maxW > 0 ? `${options['inverter.phase.breaker.amps']} A breaker, ${(maxW / 1000).toFixed(2)} kW per phase` : 'loading…';
    return `<div class="settings-group">
            <span class="settings-group-title">Mode</span>
            <div class="settings-row"><span class="settings-label">Inverter</span><span class="settings-value">${item.mode}</span></div>
            <div class="settings-row"><span class="settings-label">Battery</span><span class="settings-value">${item.batMode}</span></div>
            <div class="settings-row"><span class="settings-label">Pgrid bias</span><span class="settings-value">${item.pgridBias}</span></div>
            <div class="settings-row"><span class="settings-label">Manual mode<span class="settings-hint">Applies only while the battery mode is Manual</span></span>
                ${manual ? `<span class="settings-chip">${manualLabel}</span>` : `<span class="settings-muted">${manualLabel}</span>`}</div>
        </div>
        <div class="settings-group">
            <div class="settings-group-head"><span class="settings-group-title">House load per phase</span>
                <span><span class="settings-export">+ export</span> · <span class="settings-import">− import</span></span></div>
            <div class="settings-phases">
                <div class="settings-th"></div><div class="settings-th">House load</div><div class="settings-th settings-num">Grid</div>
                ${rows}
                <div class="settings-total">Σ</div><div class="settings-total settings-num">${formatWatts(totalLoad)}</div>
                ${gridCellHtml(totalGrid, 'settings-total')}
            </div>
            <span class="settings-hint">House load = inverter output + EPS − grid. Bars: ${scale}, orange above ${PHASE_WARN_RATIO * 100} %.</span>
        </div>`;
}
