'use strict';

class Toolbar {
    constructor(canvasId) {
        this.canvasId = canvasId;
        this.items = getToolbarItems();
        this.selectedItem = this.items[0];

        let totalHeight = 0;
        for (const item of this.items) {
            item.toolbarYPosition = totalHeight + 0.5 * item.height;
            totalHeight += item.height;
        }

        let canvas = document.getElementById(canvasId);
        canvas.height = totalHeight;
        canvas.width = 110;
        canvas.addEventListener("click", (function (event) {
            this.onClick(event);
        }).bind(this));

        this.ctx = prepareCanvasContext(this.canvasId);
        for (const item of this.items) {
            this.#drawItem(item, item === this.items[0])
        }
    }

    #drawItem(item, selected) {
        const border = 2;
        this.ctx.beginPath();
        this.ctx.lineWidth = 1;
        this.ctx.strokeStyle = 'lightgray';
        this.ctx.rect(border, item.toolbarYPosition - item.height/2 + border, this.ctx.canvas.width - 2*border, item.height - 2*border);
        this.ctx.fillStyle = (selected) ? '#FF7B2B' : 'lightgray';
        this.ctx.fill();
        this.ctx.stroke();

        item.drawFunction(this.ctx.canvas.width / 2, item.toolbarYPosition, this.ctx)
    }

    onClick(event) {
        for (const item of this.items) {
            if (event.offsetY > item.toolbarYPosition - item.height / 2 && event.offsetY < item.toolbarYPosition + item.height / 2) {
                if (this.selectedItem !== item) {
                    this.#drawItem(this.selectedItem, false)
                    this.selectedItem = item;
                    this.#drawItem(this.selectedItem, true)
                }
                break;
            }
        }
    }

    getCurrent() {
        return this.selectedItem;
    }
}