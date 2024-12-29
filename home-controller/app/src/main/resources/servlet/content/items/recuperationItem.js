'use strict';

class RecuperationItem extends AdditionalSvgToolItem {
    constructor() {
        super('futura', 65)
        this.ventSpeed = undefined;
        this.airTempAmbient = undefined;
        this.airTempFresh = undefined;
        this.airTempIndoor = undefined;
        this.airTempWaste = undefined;
        this.filterWear = undefined;
        this.powerConsumption = undefined;
        this.heatRecovering = undefined;
        this.wallControllerCo2 = undefined;
        this.wallControllerTemp = undefined;
        this.timeProgramActive = undefined;
    }

    onCanvasCreatedImpl() {
        const s = 14;
        let y = 3;
        const step = 19;
        let items = [];
        items.push(this.fanIcon = this.svg.image('img/fanIcon.svg').size(s, s).move(4, y + 2).rotate(45));
        items.push(this.textVentSpeed = this.svg.text('?').move(19, y).font(this.baseFont));
        items.push(this.clockIcon = this.svg.image('img/clockIcon.svg').size(s, s).move(43, y + 2));
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

    hasData() {
        return this.ventSpeed !== undefined;
    }

    drawImpl() {
        this.textVentSpeed.text((this.ventSpeed === 6) ? 'A' : this.ventSpeed);
        this.textCo2.text(this.wallControllerCo2);
        this.textControllerTemp.text(this.wallControllerTemp + ' °C');
        this.textFilter.text(this.filterWear + '%');
        this.textHeatRecovery.text(this.heatRecovering + ' W');
        this.textConsumption.text('💡' + this.powerConsumption + ' W');
    }
}
