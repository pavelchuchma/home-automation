'use strict';

class AbstractItem {
    constructor(id) {
        this.id = id;
    }

    update(item) {
        for (let prop in item) {
            this[prop] = item[prop];
        }
    }

    doAction(action) {
    }
}

class BaseItem extends AbstractItem {
    constructor(id, x, y, floor) {
        super(id);
        this.x = x;
        this.y = y;
        this.floor = floor;
    }

    draw(ctx) {
    }

    static _send(path) {
        const request = new XMLHttpRequest();
        request.open('GET', getBaseUrl() + path, true);
        request.send();
    }

    afterAction(ctx) {
        // draw changed item as gray
        ctx.beginPath();
        ctx.arc(this.x, this.y, 15, 0, 2 * Math.PI);
        ctx.fillStyle = 'gray';
        ctx.fill();
        ctx.stroke();
    }
}

class AdditionalToolItem extends BaseItem {
    constructor(id, canvasHeight, type = 'canvas') {
        super(id, 0, 0, -1);
        this.type = type;
        this.canvasId = id + '_' + type;
        this.canvasWidth = 110;
        this.canvasHeight = canvasHeight;
    }

    /**
     * Called after canvas creation is done
     */
    onCanvasCreated() {
    }
}

class AdditionalSvgToolItem extends AdditionalToolItem {
    constructor(id, canvasHeight) {
        super(id, canvasHeight, 'svg');
    }

    onCanvasCreated() {
        // draw background
        this.svg = SVG().addTo('#td-' + this.canvasId).size(this.canvasWidth, this.canvasHeight);
        const rect = this.svg.rect(this.canvasWidth, this.canvasHeight);
        rect.fill('lightgray').stroke({width: 1, color: 'black'});

        this.onCanvasCreatedImpl();
    }

    onCanvasCreatedImpl() {
    }

    setVisibility(svgElement, value) {
        svgElement.attr('visibility', value ? 'visible' : 'hidden');
    }
}