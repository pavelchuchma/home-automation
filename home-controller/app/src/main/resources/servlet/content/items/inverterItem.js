'use strict';

const PHASE_WARN_RATIO = 0.8;
// used while the inverter reports no grid voltage, e.g. in Idle mode
const NOMINAL_PHASE_VOLTAGE = 230;
const MANUAL_MODE_LABELS = {Stop: 'Stop', ForceCharge: 'Force charge', ForceDischarge: 'Force discharge'};
// VPP (remote control) modes, see doc/inverter/solax_vpp_protocol.md
const VPP_MODE_LABELS = {
    1: 'Power control', 2: 'Energy target', 3: 'SoC target', 4: 'Push power', 5: 'Battery idle',
    6: 'Self-consume', 7: 'Self-consume, charge only', 8: 'PV and battery', 9: 'PV and battery to SoC',
    11: 'Max input', 12: 'Max output',
};
// features of InverterManager that can hold the battery through a VPP mode
const BATTERY_OWNER_LABELS = {FORCE_DISCHARGE: 'Force discharge'};

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

const MIN_SOC_KEY = 'inverter.manager.minimal.soc';
const RESERVE_KEY = 'inverter.manager.high.tariff.battery.reserve';

/**
 * Minimal SoC plus the reserve is the SoC kept outside high tariff, so the sum is at most 100 %:
 * raising one of them lowers the other once the sum reaches it.
 */
function limitSocSum(state, key) {
    const other = key === MIN_SOC_KEY ? RESERVE_KEY : key === RESERVE_KEY ? MIN_SOC_KEY : null;
    if (!other) {
        return [];
    }
    const max = 100 - Number(state[key]);
    if (Number(state[other]) <= max) {
        return [];
    }
    state[other] = String(max);
    return [other];
}

// needsExport: offered only where the deployment may feed the grid, see inverterSettings()
// stepper limits follow the validation in InverterManager
const INVERTER_SETTINGS = {
    title: 'Inverter',
    store: INVERTER_STORE,
    adjust: limitSocSum,
    details: {optionKeys: ['inverter.phase.breaker.amps'], render: inverterDetailsHtml},
    lanes: [
        {key: 'inverter.manager.high.tariff.times', label: 'High tariff', color: '#c0392b'},
        {key: 'inverter.manager.export.priority.times', label: 'Feed-in priority', color: '#2e6fb7', needsExport: true},
    ],
    groups: [
        {title: 'Battery', fields: [
            {type: 'stepper', key: MIN_SOC_KEY, label: 'Minimal SoC', hint: 'Battery never discharges below this',
                step: 5, min: 10, max: 100, unit: '%'},
        ]},
        {title: 'High tariff', fields: [
            {type: 'ranges', key: 'inverter.manager.high.tariff.times',
                emptyHint: 'No ranges, so the reserve below is kept all day.'},
            {type: 'stepper', key: RESERVE_KEY, label: 'Reserve',
                hint: 'Battery kept for the high tariff hours', step: 5, min: 0, max: 90, unit: '%'},
        ]},
        {title: 'Feed-in priority timer', toggle: 'inverter.manager.export.priority.timer.enabled', needsExport: true,
            hint: 'FeedInPriority in these ranges, SelfUse outside', fields: [
                {type: 'ranges', key: 'inverter.manager.export.priority.times'},
            ]},
        {title: 'Force discharge', toggle: 'inverter.manager.force.discharge', needsExport: true,
            hint: 'Sell to the grid in the most expensive segments of the day', fields: [
                {type: 'stepper', key: 'inverter.manager.force.discharge.minimal.price', label: 'Minimal net price',
                    hint: 'CZK/kWh after fees, incl. VAT', step: 0.5, min: 0, max: 50, unit: '', decimals: 1},
                {type: 'stepper', key: 'inverter.manager.force.discharge.minimal.soc', label: 'Discharge down to',
                    step: 5, min: 10, max: 100, unit: '%'},
            ]},
        {title: 'Grid', needsExport: true, fields: [
            {type: 'switch', key: 'pgridBias', label: 'Pgrid bias',
                hint: 'Keep ~40 W flowing to the grid instead of ~0 W, so the house never draws from it'},
        ]},
    ],
};

/**
 * The dialog without the settings that feed the grid, unless the server says export is allowed.
 */
function inverterSettings(item) {
    if (item.exportAllowed === true) {
        return INVERTER_SETTINGS;
    }
    return {
        ...INVERTER_SETTINGS,
        lanes: INVERTER_SETTINGS.lanes.filter(l => !l.needsExport),
        groups: INVERTER_SETTINGS.groups.filter(g => !g.needsExport),
    };
}

class InverterItem extends AdditionalSvgToolItem {
    constructor() {
        super('inverter', 125)
        this.mode = undefined;
        this.batMode = undefined;
        this.manualMode = undefined;
        this.vppMode = undefined;
        this.exportAllowed = undefined;
        this.batteryOwner = undefined;
        this.refusedOwner = undefined;
        this.refusal = undefined;
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
        this.openSettingsOnClick(inverterSettings);
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

        // a running VPP mode overrides the battery mode
        this.batModeIcon.text(
            (this.vppMode > 0) ? '📡' :
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
 * Grid meter power with a bar of the phase current (|P| / U, power factor ignored) against the main breaker.
 * Without a measured voltage the nominal one is used and the current is marked as approximate.
 */
function gridPhaseHtml(w, voltage, breakerAmps) {
    const measured = voltage > 0;
    const amps = Math.abs(w) / (measured ? voltage : NOMINAL_PHASE_VOLTAGE);
    const ratio = breakerAmps > 0 ? amps / breakerAmps : 0;
    const title = measured ? `${voltage.toFixed(1)} V` : `no voltage reported, ${NOMINAL_PHASE_VOLTAGE} V assumed`;
    return `<div class="settings-phase-load">
            <span class="settings-bar${ratio > PHASE_WARN_RATIO ? ' warn' : ''}" aria-hidden="true"><i style="width:${Math.min(100, ratio * 100)}%"></i></span>
            <span class="settings-value settings-amps" title="${title}">${measured ? '' : '≈'}${amps.toFixed(1)} A</span>
            ${gridCellHtml(w, '')}</div>`;
}

/**
 * Rows telling which feature holds the battery through a VPP mode, and which one wants it and cannot get it.
 * Empty when the inverter is not managed.
 */
function batteryOwnerRowsHtml(item) {
    if (item.batteryOwner === undefined) {
        return '';
    }
    const label = owner => BATTERY_OWNER_LABELS[owner] || owner;
    const held = item.batteryOwner !== 'NONE';
    let html = `<div class="settings-row"><span class="settings-label">Battery held by<span class="settings-hint">The feature that runs the remote control mode</span></span>
                ${held ? `<span class="settings-chip">${label(item.batteryOwner)}</span>` : '<span class="settings-muted">Nobody</span>'}</div>`;
    if (item.refusal) {
        html += `<div class="settings-row"><span class="settings-label">${label(item.refusedOwner)} cannot run<span class="settings-hint">Wanted now, but not started</span></span>
                <span class="settings-chip settings-fault">${item.refusal}</span></div>`;
    }
    return html;
}

/**
 * Details tab: modes, the house load per phase and the grid current per phase, bars scaled to the main breaker.
 */
function inverterDetailsHtml(item, options) {
    const breakerAmps = Number(options['inverter.phase.breaker.amps']);
    const manual = item.batMode === 'Manual';
    const manualLabel = MANUAL_MODE_LABELS[item.manualMode] || item.manualMode;
    const vppLabel = VPP_MODE_LABELS[item.vppMode] || `Mode ${item.vppMode}`;
    let rows = '';
    let totalLoad = 0;
    let totalGrid = 0;
    for (let i = 1; i <= 3; i++) {
        const load = item['load' + i];
        const grid = item['feedIn' + i];
        totalLoad += load;
        totalGrid += grid;
        rows += `<div class="settings-value">L${i}</div>
            <div class="settings-value settings-num">${formatWatts(load)}</div>
            ${gridPhaseHtml(grid, item['voltage' + i], breakerAmps)}`;
    }
    const scale = breakerAmps > 0 ? `grid current |P| / U against the ${breakerAmps} A breaker` : 'loading…';
    return `<div class="settings-group">
            <span class="settings-group-title">Mode</span>
            <div class="settings-row"><span class="settings-label">Inverter</span><span class="settings-value">${item.mode}</span></div>
            <div class="settings-row"><span class="settings-label">Battery</span><span class="settings-value">${item.batMode}</span></div>
            <div class="settings-row"><span class="settings-label">Remote control<span class="settings-hint">A temporary VPP mode overrides the battery mode</span></span>
                ${item.vppMode > 0 ? `<span class="settings-chip">${vppLabel}</span>` : '<span class="settings-muted">Off</span>'}</div>
            ${batteryOwnerRowsHtml(item)}
            <div class="settings-row"><span class="settings-label">Pgrid bias</span><span class="settings-value">${item.pgridBias}</span></div>
            <div class="settings-row"><span class="settings-label">Manual mode<span class="settings-hint">Applies only while the battery mode is Manual</span></span>
                ${manual ? `<span class="settings-chip">${manualLabel}</span>` : `<span class="settings-muted">${manualLabel}</span>`}</div>
        </div>
        <div class="settings-group">
            <div class="settings-group-head"><span class="settings-group-title">Load per phase</span>
                <span><span class="settings-export">+ export</span> · <span class="settings-import">− import</span></span></div>
            <div class="settings-phases">
                <div class="settings-th"></div><div class="settings-th settings-num">House load</div><div class="settings-th settings-num">Grid</div>
                ${rows}
                <div class="settings-total">Σ</div><div class="settings-total settings-num">${formatWatts(totalLoad)}</div>
                ${gridCellHtml(totalGrid, 'settings-total')}
            </div>
            <span class="settings-hint">House load = inverter output + EPS − grid. Bars: ${scale}, orange above ${PHASE_WARN_RATIO * 100} %.</span>
        </div>`;
}
