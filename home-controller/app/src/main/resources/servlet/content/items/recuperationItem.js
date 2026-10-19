'use strict';

/** Timed functions, the keys are the REST action parameters, see FuturaHandler. */
const FUTURA_FUNCTIONS = [
    {key: 'boost', label: 'Boost', durations: [15, 30, 60, 120]},
    {key: 'circulation', label: 'Circul.', title: 'Circulation', durations: [30, 60, 180, 360]},
    {key: 'overpressure', label: 'Fireplace', title: 'Overpressure', durations: [15, 30, 60, 120]},
    {key: 'night', label: 'Night', durations: [60, 240, 480]},
    {key: 'party', label: 'Party', durations: [60, 120, 240, 480]},
];
const FUTURA_FUNCTION_KEYS = FUTURA_FUNCTIONS.map(f => f.key);

/** Bits of fut_mode shown as chips; the time program and the timed functions have their own controls. */
const FUTURA_MODES = {
    4: 'Away', 9: 'Filter check', 10: 'Drying', 11: 'Bypass open', 12: 'Too low temperature', 13: 'Error shutdown',
    14: 'Starting', 15: 'Service mode', 17: 'Anti-icing', 18: 'Emergency shutdown', 19: 'Duct analysis',
    20: 'Stand-by', 23: 'Exchanger defrost',
};
const FUTURA_FAULT_MODES = new Set([12, 13, 18]);
const FUTURA_ERRORS = {
    0: 'Ambient sensor error', 1: 'Indoor sensor error', 2: 'Fresh sensor error', 3: 'Waste sensor error',
    4: 'Supply fan error', 5: 'Exhaust fan error', 6: 'Exchanger communication error', 7: 'Exchanger position error',
    8: 'IO board error', 9: 'Supply fan blocked', 10: 'Exhaust fan blocked', 13: 'Declared airflow not reached on supply',
    14: 'Declared airflow not reached on exhaust', 18: 'Excessive humidity', 20: 'Fan out of control',
    21: 'Fresh temperature too low',
};
const FUTURA_WARNINGS = {
    0: 'Uninitialized filter', 1: 'Filter too dusty', 2: 'Filter too old', 3: 'Low backup battery',
    4: 'Supply fan RPM too high', 5: 'Exhaust fan RPM too high', 8: 'Too cold outside, ventilation limited',
    11: 'Emergency shutdown', 14: 'Power supply error', 16: 'Excessive humidity', 17: 'Regular service maintenance due',
};
const FUTURA_FILTER_WARN_PCT = 80;

/**
 * Values of the dialog are the REST action parameters, see FuturaHandler. Only the values that differ from the last
 * sent ones go to the unit, so the rest of the form cannot overwrite what the unit changed by itself (time program).
 */
const FUTURA_STORE = {
    load: (keys, item) => {
        if (!item.hasData()) {
            return Promise.reject(new Error('No data from the unit yet'));
        }
        const values = {
            vent: String(item.ventSpeed),
            timeProg: String(item.timeProgramActive),
            tempSet: item.tempSet.toFixed(1),
            humiSet: String(item.humiSet),
            bypass: String(item.bypass),
        };
        item.futuraSent = {...values};
        return Promise.resolve(values);
    },
    save: (values, item) => {
        const changes = {};
        Object.entries(values).forEach(([k, v]) => {
            if (FUTURA_FUNCTION_KEYS.includes(k) || item.futuraSent[k] !== v) changes[k] = v;
        });
        return fetch(`${getBaseUrl()}/rest/futura/action?id=${item.id}&${new URLSearchParams(changes)}`)
            .then(SettingsSheet.checkResponse)
            .then(r => {
                Object.assign(item.futuraSent, values);
                return r;
            });
    },
};

const FUTURA_SETTINGS = {
    title: 'Recuperation',
    instant: true,
    store: FUTURA_STORE,
    details: {optionKeys: [], render: futuraDetailsHtml},
    live: {render: futuraLiveHtml, bind: bindFuturaLive},
    lanes: [],
    groups: [
        {title: 'Ventilation', fields: [
            {type: 'choice', key: 'vent', label: 'Ventilation', options: [
                {value: '0', label: 'Off'}, {value: '1', label: '1'}, {value: '2', label: '2'}, {value: '3', label: '3'},
                {value: '4', label: '4'}, {value: '5', label: '5'}, {value: '6', label: 'Auto'},
            ]},
            {type: 'switch', key: 'timeProg', label: 'Time program', hint: 'Follow the week schedule stored in the unit'},
        ]},
        {title: 'Comfort', hint: 'Targets of the automatic mode', fields: [
            {type: 'stepper', key: 'tempSet', label: 'Preferred temperature', step: 0.5, min: 15, max: 30, unit: '°C', decimals: 1},
            {type: 'choice', key: 'humiSet', label: 'Preferred humidity', options: [
                {value: '25', label: 'Dry'}, {value: '50', label: 'Comfort'}, {value: '75', label: 'Humid'},
            ]},
        ]},
        {title: 'Automatic functions', fields: [
            {type: 'switch', key: 'bypass', label: 'Bypass', hint: 'Open the exchanger bypass for free cooling'},
        ]},
    ],
};

class RecuperationItem extends AdditionalSvgToolItem {
    constructor() {
        super('futura', 65)
        this.ventSpeed = undefined;
        this.airTempAmbient = undefined;
        this.airTempFresh = undefined;
        this.airTempIndoor = undefined;
        this.airTempWaste = undefined;
        this.humiAmbient = undefined;
        this.humiFresh = undefined;
        this.humiIndoor = undefined;
        this.humiWaste = undefined;
        this.modeFlags = undefined;
        this.errorFlags = undefined;
        this.warningFlags = undefined;
        this.filterWear = undefined;
        this.powerConsumption = undefined;
        this.heatRecovering = undefined;
        this.airFlow = undefined;
        this.fanPowerSupply = undefined;
        this.fanPowerExhaust = undefined;
        this.fanRpmSupply = undefined;
        this.fanRpmExhaust = undefined;
        this.wallControllerCo2 = undefined;
        this.wallControllerTemp = undefined;
        this.wallControllerHumi = undefined;
        this.timeProgramActive = undefined;
        this.tempSet = undefined;
        this.humiSet = undefined;
        this.bypass = undefined;
        FUTURA_FUNCTION_KEYS.forEach(k => this[k] = undefined);
    }

    onCanvasCreatedImpl() {
        this.openSettingsOnClick(FUTURA_SETTINGS);

        const s = 14;
        let y = 3;
        const step = 19;
        let items = [];
        items.push(this.fanIcon = this.svg.image('img/fanIcon.svg').size(s, s).move(4, y + 2).rotate(45));
        items.push(this.textVentSpeed = this.svg.text('?').move(19, y).font(this.baseFont));
        items.push(this.clockIcon = this.svg.image('img/clockIcon.svg').size(s, s).move(43, y + 2));
        this.createHideBox(this.clockIcon);
        items.push(this.co2Icon = this.svg.image('img/co2Icon.svg').size(s, s).move(60, y + 2));
        items.push(this.textCo2 = this.svg.text('????').move(76, y).font(this.baseFont));

        items.push(this.tempIcon = this.svg.image('img/tempIcon.svg').size(s, s).move(1, (y += step) + 2));
        items.push(this.textControllerTemp = this.svg.text('?? °C').move(19, y).font(this.baseFont));
        items.push(this.filterIcon = this.svg.image('img/filterIcon.svg').size(s, s).move(60, y + 2));
        items.push(this.textFilter = this.svg.text('??%').move(76, y).font(this.baseFont));

        items.push(this.heatRecoveryIcon = this.svg.image('img/heatRecoveryIcon.svg').size(s, s).move(4, (y += step) + 2));
        items.push(this.textHeatRecovery = this.svg.text('??? W').move(19, y).font(this.baseFont));
        items.push(this.textConsumption = this.svg.text('💡?? W').move(60, y).font(this.baseFont));

        this.showOnData.push(...items);
        this.hideOnNoData.push(...items);
    }

    drawImpl() {
        this.textVentSpeed.text((this.ventSpeed === 6) ? 'A' : this.ventSpeed);
        this.textCo2.text(this.wallControllerCo2);
        this.textControllerTemp.text(this.wallControllerTemp + ' °C');
        this.textFilter.text(this.filterWear + '%');
        this.textHeatRecovery.text(this.heatRecovering + ' W');
        this.textConsumption.text('💡' + this.powerConsumption + ' W');

        this.setVisibility(this.clockIcon.hideBox, !this.timeProgramActive);
    }
}

function futuraDuration(seconds) {
    if (seconds >= 3600) {
        return Math.floor(seconds / 3600) + ':' + String(Math.floor(seconds % 3600 / 60)).padStart(2, '0') + ' h';
    }
    return Math.ceil(seconds / 60) + ' min';
}

/**
 * @returns {[number, string][]} set bits of the mask that have a label
 */
function futuraBits(mask, labels) {
    const out = [];
    for (let bit = 0; bit < 32; bit++) {
        if (labels[bit] && Math.floor(mask / 2 ** bit) % 2 === 1) out.push([bit, labels[bit]]);
    }
    return out;
}

function futuraFlowTileHtml(label, sensor, temp, humi, side) {
    return `<div class="settings-tile settings-futura-${side}"><span>${label}<span class="settings-hint">${sensor}</span></span>
        <span class="settings-value">${temp} °C</span><span class="settings-hint">${Math.round(humi)} % RH</span></div>`;
}

/**
 * Details tab: air flows through the exchanger, power, fans, filter and the state reported by the unit.
 */
function futuraDetailsHtml(item) {
    const range = item.airTempIndoor - item.airTempAmbient;
    const efficiency = Math.abs(range) >= 1 ? Math.round((item.airTempFresh - item.airTempAmbient) / range * 100) + ' %' : '–';
    const fan = (label, power, rpm) => `<span>${label}</span>
        <span class="settings-bar" aria-hidden="true"><i style="width:${Math.min(100, power)}%"></i></span>
        <span class="settings-value">${power} %</span><span class="settings-muted">${rpm} rpm</span>`;
    const chips = futuraBits(item.modeFlags, FUTURA_MODES)
        .map(([bit, label]) => `<span class="settings-chip${FUTURA_FAULT_MODES.has(bit) ? ' settings-fault' : ''}">${label}</span>`)
        .join('');
    const problems = futuraBits(item.errorFlags, FUTURA_ERRORS)
        .map(([, label]) => `<li class="settings-fault"><b>Error</b> · ${label}</li>`)
        .concat(futuraBits(item.warningFlags, FUTURA_WARNINGS).map(([, label]) => `<li class="settings-warn"><b>Warning</b> · ${label}</li>`))
        .join('');
    return `<div class="settings-group">
            <div class="settings-group-head"><span class="settings-group-title">Air flows</span>
                <span>exchanger efficiency <b>${efficiency}</b></span></div>
            <div class="settings-futura-flow">
                <span class="settings-hint">Outside</span><span></span><span class="settings-hint">House</span>
                ${futuraFlowTileHtml('Intake', 'ambient', item.airTempAmbient, item.humiAmbient, 'in')}
                <span class="settings-futura-arrow settings-futura-in" aria-hidden="true">→</span>
                ${futuraFlowTileHtml('Supply', 'fresh', item.airTempFresh, item.humiFresh, 'in')}
                ${futuraFlowTileHtml('Exhaust', 'waste', item.airTempWaste, item.humiWaste, 'out')}
                <span class="settings-futura-arrow settings-futura-out" aria-hidden="true">←</span>
                ${futuraFlowTileHtml('Extract', 'indoor', item.airTempIndoor, item.humiIndoor, 'out')}
            </div>
            <span class="settings-hint">Blue: outdoor air into the house, orange: house air out.
                Efficiency = (supply − intake) / (extract − intake).</span>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">Power</span>
            <div class="settings-tiles settings-futura-power">
                <div class="settings-tile"><span>Heat recovered</span><span class="settings-value">${item.heatRecovering} W</span></div>
                <div class="settings-tile"><span>Consumption</span><span class="settings-value">${item.powerConsumption} W</span></div>
                <div class="settings-tile"><span>Air flow</span><span class="settings-value">${item.airFlow} m³/h</span></div>
            </div>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">Fans</span>
            <div class="settings-futura-fans">
                ${fan('Supply', item.fanPowerSupply, item.fanRpmSupply)}
                ${fan('Exhaust', item.fanPowerExhaust, item.fanRpmExhaust)}
            </div>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">Filter</span>
            <div class="settings-phase-load">
                <span class="settings-bar${item.filterWear > FUTURA_FILTER_WARN_PCT ? ' warn' : ''}" aria-hidden="true"><i style="width:${Math.min(100, item.filterWear)}%"></i></span>
                <span class="settings-value">${item.filterWear} %</span></div>
            <span class="settings-hint">Wear level, replace at 100 %. Orange above ${FUTURA_FILTER_WARN_PCT} %.</span>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">State</span>
            ${chips ? `<div class="settings-futura-chips">${chips}</div>` : ''}
            ${problems ? `<ul class="settings-futura-problems">${problems}</ul>` : '<span class="settings-muted">No errors or warnings</span>'}
        </div>`;
}

/**
 * Above the settings: room climate, current ventilation, the running timed functions and their buttons.
 */
function futuraLiveHtml(item, sheet) {
    let chips = item.ventSpeed === 0 ? '<span class="settings-chip settings-off">Ventilation off</span>'
        : `<span class="settings-chip">${item.ventSpeed === 6 ? 'Auto' : 'Level ' + item.ventSpeed} · ${item.airFlow} m³/h</span>`;
    FUTURA_FUNCTIONS.filter(f => item[f.key] > 0).forEach(f => {
        chips += `<span class="settings-chip">${f.title || f.label} · ${futuraDuration(item[f.key])}</span>`;
    });

    const buttons = FUTURA_FUNCTIONS.map(f => `<button type="button" data-func="${f.key}" aria-pressed="${item[f.key] > 0}"
            aria-expanded="${sheet.futuraOpenFunc === f.key}" title="${f.title || f.label}"><span>${f.label}</span>
            <span class="settings-hint">${item[f.key] > 0 ? futuraDuration(item[f.key]) : '–'}</span></button>`).join('');
    let durations = '';
    const open = FUTURA_FUNCTIONS.find(f => f.key === sheet.futuraOpenFunc);
    if (open) {
        const running = item[open.key] > 0;
        durations = `<div class="settings-futura-durations">
            <span class="settings-hint">${open.title || open.label} ${running ? 'for another' : 'for'}</span>
            ${open.durations.map(m => `<button type="button" data-sec="${m * 60}">${futuraDuration(m * 60)}</button>`).join('')}
            ${running ? '<button type="button" class="settings-futura-stop" data-sec="0">Stop</button>' : ''}</div>`;
    }

    return `<div class="settings-group">
            <div class="settings-futura-room">
                <span><span class="settings-hint">Room</span><span class="settings-temp-big">${item.wallControllerTemp} °C</span></span>
                <span><span class="settings-hint">CO₂</span><span class="settings-value">${item.wallControllerCo2} ppm</span></span>
                <span><span class="settings-hint">Humidity</span><span class="settings-value">${Math.round(item.wallControllerHumi)} %</span></span>
            </div>
            <div class="settings-futura-chips">${chips}</div>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">Timed functions</span>
            <div class="settings-futura-funcs">${buttons}</div>
            ${durations}
            <span class="settings-hint">Pick a duration, the unit ends the function by itself. Fireplace = overpressure.</span>
        </div>`;
}

function bindFuturaLive(el, sheet) {
    el.querySelectorAll('[data-func]').forEach(b => b.addEventListener('click', () => {
        sheet.futuraOpenFunc = sheet.futuraOpenFunc === b.dataset.func ? undefined : b.dataset.func;
        sheet.renderLive();
    }));
    el.querySelectorAll('[data-sec]').forEach(b => b.addEventListener('click', () => {
        const func = sheet.futuraOpenFunc;
        sheet.futuraOpenFunc = undefined;
        sheet.renderLive();
        sheet.apply({[func]: b.dataset.sec});
    }));
}
