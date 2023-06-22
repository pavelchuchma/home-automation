'use strict';

class ToolBarItem {
    constructor(id, drawFunction, applicableOn, onClickAction, height = 100) {
        this.id = id;
        this.drawFunction = drawFunction;
        this.applicableOn = applicableOn;
        this.onClickAction = onClickAction;
        this.height = height;
    }

    isApplicable(item) {
        return this.applicableOn.includes(item.constructor.name);
    }
}