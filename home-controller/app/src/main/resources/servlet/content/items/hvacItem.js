'use strict';

class HvacItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.on = undefined;
        this.fanSpeed = undefined;
        this.currentMode = undefined;
        this.targetMode = undefined;
        this.autoMode = undefined;
        this.quiteMode = undefined;
        this.sleepMode = undefined;
        this.defrost = undefined;
        this.targetTemperature = undefined;
        this.airTemperature = undefined;
        this.air2Temperature = undefined;
        this.roomTemperature = undefined;
        this.unitTemperature = undefined;
    }

    update(item) {
        this.on = item.on;
        this.fanSpeed = item.fanSpeed;
        this.currentMode = item.currentMode;
        this.targetMode = item.targetMode;
        this.autoMode = item.autoMode;
        this.quiteMode = item.quiteMode;
        this.sleepMode = item.sleepMode;
        this.defrost = item.defrost;
        this.targetTemperature = item.targetTemperature;
        this.airTemperature = item.airTemperature;
        this.air2Temperature = item.air2Temperature;
        this.roomTemperature = item.roomTemperature;
        this.unitTemperature = item.unitTemperature;
    }

    draw(ctx) {
        ctx.rect(0, 0, 100, 150);
        ctx.fillStyle = 'lightgray';
        ctx.fill();
        ctx.stroke();

        if (this.on) {
            ctx.font = "bold 15px Arial";
            ctx.fontWeight = "500";
            ctx.fillStyle = 'red';
            ctx.fillText(this.targetMode, 5, 20);
            ctx.fillText(this.fanSpeed, 5, 40);
            ctx.fillText('Tgt temp: ' + this.targetTemperature, 5, 60);
            ctx.font = "13px Arial";
            let y = 70;
            const step = 17;
            ctx.fillText('Air temp: ' + this.airTemperature, 5, y += step);
            ctx.fillText('Room temp: ' + this.roomTemperature, 5, y += step);
            ctx.fillText('Unit temp: ' + this.unitTemperature, 5, y + step);
            if (this.defrost) {
                ctx.font = "bold 15px Arial";
                ctx.fillText('Defrost!', 5, step + 5);
            }
        } else {
            ctx.fillStyle = 'black';
            ctx.font = "30px Arial";
            ctx.fillText('OFF', 15, 30);
        }
    }
}