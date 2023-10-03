'use strict';

function getFloorImages() {
    return ['1stFloor-ondra.png'];
}

function initConfiguration() {
    document.getElementById(new WaterPumpItem().canvasId).addEventListener("click", (function () {
        window.location = '/nodes'
    }));
}

function getLouversComponents() {
    return [
        new LouversItem('lvKoupelna',70, 260, 0),
        new LouversItem('lvPracovna',218, 75, 0),
        new LouversItem('lvLoznice',477, 150, 0),
        new LouversItem('lvPokojEdita',477, 333, 0),
        new LouversItem('lvPokojKluci',477, 478, 0),
        new LouversItem('lvTerasa1',477, 630, 0),
        new LouversItem('lvTerasa2',477, 720, 0),
        new LouversItem('lvJidelna',167, 760, 0),
        new LouversItem('lvKuchyn',70, 655, 0),
    ];
}

function getComponents() {
    return [
    ].concat(getLouversComponents());
}

function getToolbarItems() {
    return [
        new ToolBarItem('louversUp', function (x, y, ctx) {
            LouversItem.drawIcon(x, y, .3, 0, 'stopped', ctx, 50, 60)
        }, [LouversItem.name, StairsItem.name], 'up'),

        new ToolBarItem('louversOutshine', function (x, y, ctx) {
            LouversItem.drawIcon(x, y, 1, 0, 'stopped', ctx, 50, 60);
        }, [LouversItem.name, StairsItem.name], 'outshine'),

        new ToolBarItem('louversDown', function (x, y, ctx) {
            LouversItem.drawIcon(x, y, 1, 1, 'stopped', ctx, 50, 60);
        }, [LouversItem.name, StairsItem.name], 'blind'),
    ];
}

function getAdditionalToolbars() {
    return[
        new WaterPumpItem(),
    ]
}