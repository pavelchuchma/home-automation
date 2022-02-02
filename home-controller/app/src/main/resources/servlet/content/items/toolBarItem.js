'use strict';

class ToolBarItem extends BaseItem {
    constructor(id, x, y, drawFunction, applicableOn, onClickAction) {
        super(id, x, y, -1)
        this.drawFunction = drawFunction;
        this.applicableOn = applicableOn;
        this.onClickAction = onClickAction;
    }

    isApplicable(item) {
        return this.applicableOn.includes(item.constructor.name);
    }
}