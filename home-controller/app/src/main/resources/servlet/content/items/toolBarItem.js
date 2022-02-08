'use strict';

class ToolBarItem {
    constructor(id, drawFunction, applicableOn, onClickAction) {
        this.id = id;
        this.drawFunction = drawFunction;
        this.applicableOn = applicableOn;
        this.onClickAction = onClickAction;
    }

    isApplicable(item) {
        return this.applicableOn.includes(item.constructor.name);
    }
}