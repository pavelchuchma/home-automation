'use strict';

class Toolbar {
    constructor(containerId) {
        this.items = getToolbarItems();
        this.selectedItem = this.items[0];

        const width = 110;
        const totalHeight = this.items.reduce((sum, item) => sum + item.height, 0);
        const svg = SVG().addTo('#' + containerId).size(width, totalHeight);
        svg.rect(width, totalHeight).fill('lightgray').stroke({width: 1, color: 'black'});

        let top = 0;
        for (const item of this.items) {
            this.#createItem(svg, item, top, width);
            top += item.height;
        }
        this.#setSelected(this.selectedItem, true);
    }

    #createItem(svg, item, top, width) {
        const border = 2;
        const group = svg.group().css('cursor', 'pointer');
        item.toolbarBackground = group.rect(width - 2 * border, item.height - 2 * border).move(border, top + border)
            .stroke({width: 1, color: 'lightgray'});
        this.#setSelected(item, false);
        item.drawFunction(width / 2, top + item.height / 2, group);
        group.on('click', () => this.#select(item));
    }

    #select(item) {
        if (this.selectedItem !== item) {
            this.#setSelected(this.selectedItem, false);
            this.selectedItem = item;
            this.#setSelected(this.selectedItem, true);
        }
    }

    #setSelected(item, selected) {
        item.toolbarBackground.fill((selected) ? '#FF7B2B' : 'lightgray');
    }

    getCurrent() {
        return this.selectedItem;
    }
}
