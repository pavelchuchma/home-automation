'use strict';

class InfraHeaterItem extends OnOffItem {
    secondaryHeater;

    constructor(id, secondaryHeater) {
        super(id, secondaryHeater.x, secondaryHeater.y, secondaryHeater.floor)
        this.secondaryHeater = secondaryHeater;
    }

    draw(ctx) {
        this.drawImpl(ctx, false);
    }

    drawImpl(ctx, onTouch) {
        this.drawTube(ctx, 0, onTouch, this.secondaryHeater.val !== 0);
        this.drawTube(ctx, 1, onTouch, this.val !== 0);
        this.drawTube(ctx, 2, onTouch, this.secondaryHeater.val !== 0);
    }

    drawTube(ctx, index, onTouch, isOn) {
        let yy = this.y + (index - 1) * 7;
        ctx.lineWidth = 4;
        ctx.strokeStyle = (onTouch) ? 'lightgray' : (isOn) ? 'red' : 'gray';
        ctx.beginPath();
        ctx.moveTo(this.x - 25, yy);
        ctx.lineTo(this.x + 25, yy);
        ctx.stroke();
    }

    doAction(action) {
        action = this.simplifyOnOffAction(action);
        BaseItem._send(`/rest/onOff/action?id=${this.id}&action=${action}&timeout=900`);
        BaseItem._send(`/rest/onOff/action?id=${this.secondaryHeater.id}&action=${action}&timeout=900`);
    }

    afterAction(ctx) {
        this.drawImpl(ctx, true)
    }
}

class SecondaryInfraHeaterItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }
}

