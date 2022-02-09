'use strict';

class Toolbar {
    constructor() {
        this.items = getToolbarItems();
        this.selectedItemIndex = 0;
        this.ctx = getCanvasContext('toolsCanvas');
        this.itemHeight = this.ctx.canvas.height / this.items.length;
        for (let i = 0; i < this.items.length; i++) {
            this.items[i].drawFunction(this.ctx.canvas.width / 2, this.itemHeight * (i + 0.5), this.ctx);
        }
        this.#drawToolSelection();

        document.getElementById('toolsCanvas').addEventListener("click", (function (event) {
            this.onClick(event);
        }).bind(this));
    }

    onClick(event) {
        this.selectedItemIndex = Math.floor(event.offsetY / this.itemHeight);
        this.#drawToolSelection();
    }

    #drawToolSelection() {
        const itemWidth = this.ctx.canvas.width;
        for (let i = 0; i < this.items.length; i++) {
            const r = 35;

            this.ctx.beginPath();
            this.ctx.rect(itemWidth / 2 - r, (this.itemHeight * (i + 0.5)) - r, 2 * r, 2 * r);
            this.ctx.strokeStyle = (i === this.selectedItemIndex) ? 'red' : 'lightgray';
            this.ctx.lineWidth = 10;
            this.ctx.stroke();
        }
    }

    getCurrent() {
        return this.items[this.selectedItemIndex];
    }
}