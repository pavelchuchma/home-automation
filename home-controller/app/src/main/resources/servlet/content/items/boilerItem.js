'use strict';

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

    onClick() {
        const path = `/rest/boiler/action?id=${this.id}&action=refresh`;
        BaseItem._send(path)
    }

    onCanvasCreatedImpl() {
        document.getElementById(this.canvasId).addEventListener("click", (function () {
            this.onClick();
        }).bind(this));

        const s = 14;
        let y = 3;
        const step = 19;
        let items = [];
        let hidingBoxes = [];
        items.push(this.tempIcon = this.svg.image('img/tempIcon.svg').size(s, s).move(1, y + 2));
        items.push(this.textCurrentTemp = this.svg.text('??/?? °C').move(19, y).font(this.baseFont));
        items.push(this.textTargetTemp = this.svg.text('??').move(80, y).font(this.baseFont));

        y += step;
        y += 2;
        let x = 4 - step;
        items.push(this.onOffIcon = this.svg.image('img/onOff.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.onOffIcon);
        items.push(this.pumpIcon = this.svg.image('img/pump.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.pumpIcon);
        items.push(this.eHeatIcon = this.svg.image('img/eHeat.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.eHeatIcon);
        items.push(this.palmIcon = this.svg.image('img/palm.svg').size(s, s).move(x += step, y));
        this.createHideBox(this.palmIcon);

        this.showOnData.push(...items);
        this.hideOnNoData.push(...items);
    }

    hasData() {
        return this.targetTemp !== undefined;
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
