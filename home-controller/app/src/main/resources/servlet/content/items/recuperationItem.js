'use strict';

class RecuperationItem extends AdditionalToolItem {
    constructor() {
        super('futura', 135)
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

        // icons from https://icons8.com/icons
        this.fanIcon = this.getImage('img/fanIcon.png')
        this.clockIcon = this.getImage('img/clockIcon.png')
        this.co2Icon = this.getImage('img/co2Icon.png')
        this.heatRecoveryIcon = this.getImage('img/heatRecoveryIcon.png')
        this.filterIcon = this.getImage('img/filterIcon.png')
    }

    getImage(src) {
        let img = new Image()
        img.src = src
        return img
    }

    draw() {
        const ctx = prepareCanvasContext(this.canvasId);

        if (this.ventSpeed !== undefined) {
            ctx.font = "12px Arial";
            ctx.fillStyle = 'black';
            let y = 3;
            const step = 17;
            ctx.drawImage(this.fanIcon, 5, y+5, 16, 16)
            ctx.fillText(this.ventSpeed, 22, y += step);
            ctx.fillText("🌡" + this.wallControllerTemp + " °C", 40, y);

            ctx.drawImage(this.co2Icon, 5, y+5, 16, 16)
            ctx.drawImage(this.filterIcon, 55, y+5, 16, 16)
            ctx.fillText(this.wallControllerCo2, 22, y += step);
            ctx.fillText(this.filterWear + "%", 72, y);


            ctx.drawImage(this.heatRecoveryIcon, 5, y+5, 16, 16)
            ctx.fillText(this.heatRecovering + "  💡" + this.powerConsumption + " W", 22, y += step);

            if (this.timeProgramActive) {
                ctx.drawImage(this.clockIcon, 5, y+5, 16, 16)
            }

        } else {
            ctx.fillStyle = 'black';
            ctx.font = "30px Arial";
            ctx.fillText('?', 15, 30);
        }
    }
}
