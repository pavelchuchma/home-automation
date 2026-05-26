'use strict';

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
        air2Temperature: undefined,
        roomTemperature: undefined,
        unitTemperature: undefined,
    };

    constructor() {
        super('hvac', 60)
    }

    updateToDataProperty() {
        return true;
    }

    onCanvasCreatedImpl() {
        document.getElementById(this.canvasId).addEventListener("click", (function () {
            this.onClick();
        }).bind(this));

        const s = 14;
        const step = 19;
        const items = [];

        let y = 3;
        items.push(this.onOffIcon = this.svg.image('img/onOff.svg').size(s, s).move(1, y + 2));
        items.push(this.textTargetMode = this.svg.text('?').move(19, y).font(this.baseFont));
        items.push(this.textTargetTemp = this.svg.text('?').move(70, y).font(this.baseFont));

        y += step;
        items.push(this.fanIcon = this.svg.image('img/fanIcon.svg').size(s, s).move(4, y + 2).rotate(45));
        items.push(this.textFanSpeed = this.svg.text('?').move(19, y).font(this.baseFont));
        items.push(this.textDefrost = this.svg.text('❄ Defrost!').move(60, y).font(this.baseFont));

        y += step;
        items.push(this.textRoomAirTemp = this.svg.text('?').move(5, y).font(this.baseFont));
        items.push(this.pumpIcon = this.svg.image('img/pump.svg').size(s, s).move(70, y + 2));
        items.push(this.textUnitTemp = this.svg.text('?').move(88, y).font(this.baseFont));

        // gray overlay shown when the unit is off — inset by 1 px so the black border stays visible
        this.offOverlay = this.svg.rect(this.canvasWidth - 2, this.canvasHeight - 2).move(1, 1).fill('lightgray').opacity(.9);

        this.showOnData.push(...items);
        this.hideOnNoData.push(...items, this.offOverlay);
    }

    drawImpl() {
        const d = this.data;
        this.textTargetMode.text(d.targetMode);
        this.textTargetTemp.text('▶ ' + this.formatTemp(d.targetTemperature));
        this.textFanSpeed.text((d.fanSpeed === 'AUTO') ? 'A' : (d.fanSpeed === 'NONE') ? '?' : d.fanSpeed.replace('SPEED_', ''));
        this.textRoomAirTemp.text('🏠 ' + this.formatTemp(d.roomTemperature) + ' 🌬 ' + this.formatTemp(d.airTemperature));
        this.textUnitTemp.text(this.formatTemp(d.unitTemperature) + '');

        this.setVisibility(this.offOverlay, !d.on);
        this.setVisibility(this.textDefrost, !!d.defrost);
    }

    formatTemp(t) {
        return Math.round(t);
    }

    onClick() {
        const path = `/rest/hvac/action?id=hvac&on=${(this.data.on) ? 'false' : 'true'}`;
        BaseItem._send(path)
    }
}
