'use strict';

const HVAC_MODE_ICONS = {AUTO: 'hvacAuto', COOL: 'hvacCool', FAN: 'hvacFan', DRY: 'hvacDry', HEAT: 'hvacHeat', NONE: 'hvacNone'};
const HVAC_MODE_LABELS = {AUTO: 'Auto', COOL: 'Cooling', DRY: 'Drying', FAN: 'Fan only', HEAT: 'Heating'};
// how long the power button waits for the unit to report the new state
const HVAC_POWER_PENDING_MS = 15_000;

/**
 * Values of the dialog are the REST action parameters, see HvacHandler.
 */
const HVAC_STORE = {
    load: (keys, item) => {
        const d = item.data;
        return item.hasData() ? Promise.resolve({
            mode: d.currentMode,
            fan: d.fanSpeed,
            temp: String(d.targetTemperature),
            sleep: String(d.sleepMode),
            quiet: String(d.quiteMode),
        }) : Promise.reject(new Error('No data from the unit yet'));
    },
    save: (changes, item) => fetch(`${getBaseUrl()}/rest/hvac/action?id=${item.id}&${new URLSearchParams(changes)}`)
        .then(SettingsSheet.checkResponse),
};

const HVAC_SETTINGS = {
    title: 'Air conditioning',
    instant: true,
    store: HVAC_STORE,
    live: {render: hvacLiveHtml, bind: bindHvacLive},
    lanes: [],
    groups: [
        {title: 'Mode', fields: [
            {type: 'choice', key: 'mode', label: 'Mode', options: ['AUTO', 'COOL', 'DRY', 'FAN', 'HEAT'].map(m => ({
                value: m, label: m.charAt(0) + m.slice(1).toLowerCase(), icon: `img/${HVAC_MODE_ICONS[m]}.svg`,
            }))},
        ]},
        // the unit has no target temperature in fan mode
        {title: 'Temperature', enabledIf: state => state.mode !== 'FAN', fields: [
            {type: 'stepper', key: 'temp', label: 'Target', hint: 'Allowed 16–30 °C', step: 1, min: 16, max: 30, unit: '°C'},
        ]},
        {title: 'Fan speed', fields: [
            {type: 'choice', key: 'fan', label: 'Fan speed', options: [
                {value: 'AUTO', label: 'Auto'}, {value: 'SPEED_1', label: '1'}, {value: 'SPEED_2', label: '2'}, {value: 'SPEED_3', label: '3'},
            ]},
        ]},
        {title: 'Options', fields: [
            {type: 'switch', key: 'sleep', label: 'Sleep', hint: 'Night mode of the unit'},
            {type: 'switch', key: 'quiet', label: 'Quiet', hint: 'Lower fan noise'},
        ]},
    ],
};

class HvacItem extends AdditionalSvgToolItem {
    data = {
        on: undefined,
        fanSpeed: undefined,
        currentMode: undefined,
        targetMode: undefined,
        autoMode: undefined,
        quiteMode: undefined,
        sleepMode: undefined,
        defrost: undefined,
        targetTemperature: undefined,
        airTemperature: undefined,
        roomTemperature: undefined,
    };

    constructor() {
        super('hvac', 42)
    }

    updateToDataProperty() {
        return true;
    }

    onCanvasCreatedImpl() {
        this.openSettingsOnClick(HVAC_SETTINGS);

        const s = 14;
        const step = 19;
        const items = [];

        let y = 3;
        items.push(this.onOffIcon = this.svg.image('img/onOff.svg').size(s, s).move(1, y + 2));
        // one icon per OperatingMode value — stacked at the same spot, drawImpl toggles visibility
        this.modeIcons = {};
        Object.entries(HVAC_MODE_ICONS).forEach(([mode, fname]) => {
            this.modeIcons[mode] = this.svg.image(`img/${fname}.svg`).size(s, s).move(19, y + 2).attr({visibility: 'hidden'});
        });
        items.push(this.textTargetTemp = this.svg.text('?').move(38, y).font(this.baseFont));
        items.push(this.fanIcon = this.svg.image('img/fanIcon.svg').size(s, s).move(72, y + 2).rotate(45));
        items.push(this.textFanSpeed = this.svg.text('?').move(90, y).font(this.baseFont));

        y += step;
        items.push(this.textRoomAirTemp = this.svg.text('?').move(5, y).font(this.baseFont));

        // gray overlay shown when the unit is off — inset by 1 px so the black border stays visible
        this.offOverlay = this.svg.rect(this.canvasWidth - 2, this.canvasHeight - 2).move(1, 1).fill('lightgray').opacity(.9);

        this.showOnData.push(...items);
        this.hideOnNoData.push(...items, this.offOverlay, ...Object.values(this.modeIcons));
    }

    drawImpl() {
        const d = this.data;
        Object.entries(this.modeIcons).forEach(([mode, icon]) => this.setVisibility(icon, d.currentMode === mode));
        this.textTargetTemp.text('▶ ' + this.formatTemp(d.targetTemperature));
        this.setVisibility(this.textTargetTemp, d.currentMode !== 'FAN');
        this.textFanSpeed.text((d.fanSpeed === 'AUTO') ? 'A' : (d.fanSpeed === 'NONE') ? '?' : d.fanSpeed.replace('SPEED_', ''));
        this.textRoomAirTemp.text('🏠 ' + this.formatTemp(d.roomTemperature) + ' 🌬 ' + this.formatTemp(d.airTemperature));

        this.setVisibility(this.offOverlay, !d.on);
    }

    formatTemp(t) {
        return Math.round(t);
    }
}

/**
 * Status above the settings: temperatures, state chips and the power button.
 */
function hvacLiveHtml(item, sheet) {
    const d = item.data;
    if (sheet.hvacPowerPending === d.on) {
        sheet.hvacPowerPending = undefined;
    }
    const pending = sheet.hvacPowerPending;
    let chips;
    if (d.on) {
        chips = `<span class="settings-chip"><img src="img/${HVAC_MODE_ICONS[d.currentMode]}.svg" alt="">${HVAC_MODE_LABELS[d.currentMode] || d.currentMode}</span>`;
        if (d.targetMode !== d.currentMode && d.targetMode !== 'NONE') {
            chips += `<span class="settings-chip settings-off">requested ${HVAC_MODE_LABELS[d.targetMode] || d.targetMode}</span>`;
        }
        if (d.defrost) {
            chips += '<span class="settings-chip">Defrost</span>';
        }
    } else {
        chips = '<span class="settings-chip settings-off">Off</span>';
    }

    let label = d.on ? 'Turn off' : 'Turn on';
    if (pending !== undefined) {
        label = pending ? 'Turning on…' : 'Turning off…';
    }
    return `<div class="settings-group settings-hvac">
            <div>
                <div class="settings-hvac-temps">
                    <span><span class="settings-hint">Room</span><span class="settings-temp-big">${d.roomTemperature} °C</span></span>
                    <span><span class="settings-hint">Output air</span><span class="settings-value">${d.on ? d.airTemperature + ' °C' : '–'}</span></span>
                </div>
                <div class="settings-hvac-chips">${chips}</div>
            </div>
            <button type="button" class="settings-power" aria-pressed="${d.on}" ${pending !== undefined ? 'disabled' : ''}>
                <img src="img/onOff.svg" alt=""><span>${label}</span></button>
        </div>`;
}

function bindHvacLive(el, sheet) {
    el.querySelector('.settings-power').addEventListener('click', () => {
        const on = !sheet.item.data.on;
        sheet.hvacPowerPending = on;
        sheet.renderLive();
        const clearPending = () => {
            sheet.hvacPowerPending = undefined;
            sheet.renderLive();
        };
        setTimeout(clearPending, HVAC_POWER_PENDING_MS);
        // the form values go along, a separate request could revert an edit still in flight
        sheet.apply({on: String(on)}).then(ok => ok || clearPending());
    });
}
