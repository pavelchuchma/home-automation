'use strict';

// noinspection DuplicatedCode
class NodeInfoItem extends AbstractItem {
    constructor(id, name) {
        super(id)
        this.name = name;
        this.lastPingAge = undefined;
        this.buildTime = undefined;
        this.bootTime = undefined;
        this.testMode = undefined;
        this.messages = [];
    }

    doAction(action) {
    }
}

// noinspection JSUnusedGlobalSymbols
class NodeMessage {
    dir;
    type;
    typeName;
    data;
}