'use strict';

class InfraHeaterItem extends OnOffItem {
    secondaryHeater;

    constructor(id, secondaryHeater) {
        super(id, secondaryHeater.x, secondaryHeater.y, secondaryHeater.floor)
        this.secondaryHeater = secondaryHeater;
    }

    draw(svg) {
        this.drawImpl(svg, false);
    }

    drawImpl(svg, onTouch) {
        this.drawTube(svg, 0, onTouch, this.secondaryHeater.val !== 0);
        this.drawTube(svg, 1, onTouch, this.val !== 0);
        this.drawTube(svg, 2, onTouch, this.secondaryHeater.val !== 0);
    }

    drawTube(svg, index, onTouch, isOn) {
        const yy = this.y + (index - 1) * 7;
        const color = (onTouch) ? 'lightgray' : (isOn) ? 'red' : 'gray';
        svg.line(this.x - 25, yy, this.x + 25, yy).stroke({width: 4, color: color});
    }

    doAction(action) {
        action = this.simplifyOnOffAction(action);
        BaseItem._send(`/rest/onOff/action?id=${this.id}&action=${action}&timeout=900`);
        BaseItem._send(`/rest/onOff/action?id=${this.secondaryHeater.id}&action=${action}&timeout=900`);
    }

    afterAction(svg) {
        this.drawImpl(svg, true)
    }
}

class SecondaryInfraHeaterItem extends OnOffItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
    }
}

