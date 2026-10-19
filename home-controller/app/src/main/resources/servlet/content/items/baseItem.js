'use strict';

class AbstractItem {
    constructor(id) {
        this.id = id;
    }

    update(item) {
        this.updatedWithValidData = Object.keys(item).length > 1;
        if (this.updateToDataProperty()) {
            this.data = item;
        } else {
            for (let prop in item) {
                this[prop] = item[prop];
            }
        }
    }

    doAction(action) {
    }

    /**
     * @returns {[string,string | number | boolean][]} name/value pairs for next GET refresh call
     */
    getRefreshParams() {
    }

    updateToDataProperty() {
        return false;
    }
}

class BaseItem extends AbstractItem {
    constructor(id, x, y, floor) {
        super(id);
        this.x = x;
        this.y = y;
        this.floor = floor;
    }

    draw(svg) {
    }

    static _send(path) {
        const request = new XMLHttpRequest();
        request.open('GET', getBaseUrl() + path, true);
        request.send();
    }

    afterAction(svg) {
        // draw changed item as gray
        svg.circle(30).center(this.x, this.y).fill('gray').stroke({width: 1, color: 'black'});
    }
}

class AdditionalSvgToolItem extends BaseItem {
    constructor(id, canvasHeight) {
        super(id, 0, 0, -1);
        this.canvasId = id + '_svg';
        this.canvasWidth = 110;
        this.canvasHeight = canvasHeight;
        this.baseFont = {fill: 'black', family: 'Arial', size: 12};
        this.hideOnNoData = [];
        this.showOnData = [];
    }

    onCanvasCreated() {
        // draw background
        this.svg = SVG().addTo('#td-' + this.canvasId).size(this.canvasWidth, this.canvasHeight);
        this.svg.attr('id', this.canvasId);
        const rect = this.svg.rect(this.canvasWidth, this.canvasHeight);
        rect.fill('lightgray').stroke({width: 1, color: 'black'});
        this.textNoData = this.svg.text('⏳').move(5, 3).font(this.baseFont);
        this.onCanvasCreatedImpl();
    }

    hasData() {
        return this.updatedWithValidData;
    }

    draw() {
        if (this.hasData()) {
            this.setVisibility(this.textNoData, false);
            this.setVisibility(this.showOnData, true);
            this.drawImpl();
        } else {
            this.setVisibility(this.textNoData, true);
            this.setVisibility(this.hideOnNoData, false);
        }
        SettingsSheet.itemDrawn(this);
    }

    drawImpl() {
    }

    /**
     * A click anywhere on the widget opens the details and settings dialog, once there is data to show.
     */
    openSettingsOnClick(sheetConfig) {
        const canvas = document.getElementById(this.canvasId);
        canvas.style.cursor = 'pointer';
        canvas.addEventListener('click', () => {
            if (this.hasData()) {
                SettingsSheet.open(sheetConfig, this);
            }
        });
    }

    setVisibility(e, value) {
        if (Array.isArray(e)) {
            e.forEach(i => this.setVisibility(i, value));
        } else {
            e.attr('visibility', value ? 'visible' : 'hidden');
        }
    }

    createHideBox(img) {
        let hb = this.svg.rect(img.node.width.baseVal.value, img.node.height.baseVal.value)
            .move(img.node.x.baseVal.value, img.node.y.baseVal.value).fill('lightgray').opacity(.9);
        this.hideOnNoData.push(hb);
        img.hideBox = hb;
        return hb;
    }
}