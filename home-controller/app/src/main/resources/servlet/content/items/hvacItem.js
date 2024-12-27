'use strict';

class HvacItem extends AdditionalToolItem {
    constructor() {
        super('hvac', 150)
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

    onCanvasCreated() {
        document.getElementById(this.canvasId).addEventListener("click", (function () {
            this.onClick();
        }).bind(this));
    }

    draw() {
        const ctx = prepareCanvasContext(this.canvasId);

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

    onClick() {
        const path = `/rest/hvac/action?id=hvac&on=${(this.on) ? 'false' : 'true'}`;
        BaseItem._send(path)
    }
}
