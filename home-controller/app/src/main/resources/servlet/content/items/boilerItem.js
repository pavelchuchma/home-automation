'use strict';

// a refresh is a key sequence on the boiler display, the widget shows no data until it is done
const BOILER_REFRESH_PENDING_MS = 20_000;

const BOILER_SETTINGS = {
    title: 'Boiler',
    details: {optionKeys: [], render: boilerDetailsHtml, bind: bindBoilerDetails},
    lanes: [
        {key: 'boiler.times', label: 'Heating', color: '#c46a12'},
        {key: 'inverter.manager.high.tariff.times', label: 'High tariff', color: '#b8bfba', readOnly: true},
    ],
    groups: [
        {title: 'Temperature', fields: [
            {type: 'stepper', key: 'boiler.target.temp', label: 'Target', hint: 'Allowed 38–60 °C', step: 1, min: 38, max: 60, unit: '°C'},
        ]},
        {title: 'Operating times', hint: 'The heat pump heats water only in these ranges', fields: [
            {type: 'ranges', key: 'boiler.times'},
        ]},
        {title: 'Options', hint: 'One-shot, cleared when the boiler turns off after the operating time', fields: [
            {type: 'switch', key: 'boiler.disinfect', label: 'Disinfection', hint: 'Heat to 60 °C instead of the target'},
            {type: 'switch', key: 'boiler.eheat', label: 'Electric heater', hint: 'Also turn on the electric heater'},
        ]},
    ],
};

class BoilerItem extends AdditionalSvgToolItem {
    constructor() {
        super('boiler', 43)
        this.targetTemp = undefined;
        this.t5U = undefined;
        this.t5L = undefined;
        this.t3 = undefined;
        this.t4 = undefined;
        this.tP = undefined;
        this.th = undefined;
        this.on = undefined;
        this.hot = undefined;
        this.eHeat = undefined;
        this.pump = undefined;
        this.vacation = undefined;
    }

    onCanvasCreatedImpl() {
        this.openSettingsOnClick(BOILER_SETTINGS);

        const s = 14;
        let y = 4;
        const step = 19;
        let items = [];
        let x = 4 - step;
        items.push(this.onOffIcon = this.svg.image('img/onOff.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.onOffIcon);
        items.push(this.pumpIcon = this.svg.image('img/pump.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.pumpIcon);
        items.push(this.eHeatIcon = this.svg.image('img/eHeat.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.eHeatIcon);
        items.push(this.palmIcon = this.svg.image('img/palm.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.palmIcon);

        y += step;
        items.push(this.tempIcon = this.svg.image('img/tempIcon.svg').size(s, s).move(1, y + 2));
        items.push(this.textCurrentTemp = this.svg.text('??/?? °C').move(19, y).font(this.baseFont));
        items.push(this.textTargetTemp = this.svg.text('??').move(80, y).font(this.baseFont));

        this.showOnData.push(...items);
        this.hideOnNoData.push(...items);
    }

    drawImpl() {
        this.textCurrentTemp.text(this.t5U + '/' + this.t5L + ' °C');
        this.textTargetTemp.text('▶' + this.targetTemp);
        this.setVisibility(this.onOffIcon.hideBox, !this.on);
        this.setVisibility(this.pumpIcon.hideBox, !this.pump);
        this.setVisibility(this.eHeatIcon.hideBox, !this.eHeat);
        this.setVisibility(this.palmIcon.hideBox, !this.vacation);
    }
}

function boilerTileHtml(label, sensor, temp) {
    return `<div class="settings-tile"><span>${label}<span class="settings-hint">${sensor}</span></span>
        <span class="settings-value">${temp} °C</span></div>`;
}

/**
 * Details tab: water temperatures in the tank and the heat pump sensors.
 */
function boilerDetailsHtml(item) {
    return `<div class="settings-group">
            <span class="settings-group-title">Water</span>
            <div class="settings-water">
                <span class="settings-tank" aria-hidden="true"><i></i><i></i></span>
                <div class="settings-temps">
                    <div class="settings-row"><span class="settings-label">Upper<span class="settings-hint">T5U</span></span>
                        <span class="settings-temp-big">${item.t5U} °C</span></div>
                    <div class="settings-row"><span class="settings-label">Lower<span class="settings-hint">T5L</span></span>
                        <span class="settings-temp-big">${item.t5L} °C</span></div>
                    <div class="settings-row"><span class="settings-label settings-muted">Target</span>
                        <span class="settings-value">${item.targetTemp} °C</span></div>
                </div>
            </div>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">Heat pump</span>
            <div class="settings-tiles">
                ${boilerTileHtml('Outdoor air', 'T4', item.t4)}
                ${boilerTileHtml('Evaporator', 'T3', item.t3)}
                ${boilerTileHtml('Discharge', 'TP', item.tP)}
                ${boilerTileHtml('Suction', 'Th', item.th)}
            </div>
        </div>
        <div class="settings-group">
            <div class="settings-row"><span class="settings-label">Read the state again
                <span class="settings-hint">Goes through the boiler display, takes about 15 s</span></span>
                <button type="button" class="settings-btn settings-boiler-refresh" ${boilerRefreshPending(item) ? 'disabled' : ''}>
                    ${boilerRefreshPending(item) ? 'Refreshing…' : 'Refresh'}</button></div>
        </div>`;
}

function boilerRefreshPending(item) {
    return item.refreshRequestedAt !== undefined && Date.now() - item.refreshRequestedAt < BOILER_REFRESH_PENDING_MS;
}

function bindBoilerDetails(el, sheet) {
    const button = el.querySelector('.settings-boiler-refresh');
    button.addEventListener('click', () => {
        const item = sheet.item;
        item.refreshRequestedAt = Date.now();
        BaseItem._send(`/rest/boiler/action?id=${item.id}&action=refresh`);
        sheet.renderDetails();
        setTimeout(() => sheet.renderDetails(), BOILER_REFRESH_PENDING_MS);
    });
}
