'use strict';

class OnOffItem extends BaseItem {
    constructor(id, x, y, floor) {
        super(id, x, y, floor)
        this.name = undefined;
        this.val = undefined;
    }

    update(item) {
        this.name = item.name;
        this.val = item.val;
    }

    simplifyOnOffAction(action) {
        if (action === 'plus' || action === 'full') {
            return 'on';
        } else if (action === 'minus') {
            return 'off';
        }
        return action;
    }
}