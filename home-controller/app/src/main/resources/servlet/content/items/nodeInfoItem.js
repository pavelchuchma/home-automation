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

    update(item) {
        this.name = item.name;
        this.lastPingAge = item.lastPingAge;
        this.buildTime = item.buildTime;
        this.bootTime = item.bootTime;
        this.testMode = item.testMode;
        this.messages = item.messages;
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