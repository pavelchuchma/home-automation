'use strict';

function getFloorImages() {
    return ['img/1stFloor-petr.jpg', 'img/2ndFloor-petr.jpg'];
}

function initConfiguration() {
    document.getElementById(new WaterPumpItem().canvasId).addEventListener("click", (function () {
        window.location = '/nodes'
    }));
}

function getLouversComponents() {
    return [
        new LouversItem('lvKoupH', 530, 473, 1),
        new LouversItem('lvKrys', 530, 640, 1),
        new LouversItem('lvPata', 530, 823, 1),
        new LouversItem('lvMarek', 530, 1006, 1),

        new LouversItem('lvLoz1', 530, 1311, 1),
        new LouversItem('lvLoz2', 397, 1440, 1),
        new LouversItem('lvSat', 55, 1085, 1),
        new LouversItem('lvPrc', 55, 1325, 1),

        new LouversItem('lvKuch', 510, 809, 0),
        new LouversItem('lvOb1', 510, 978, 0),
        new LouversItem('lvOb2', 510, 1121, 0),
        new LouversItem('lvOb3', 510, 1285, 0),

        new LouversItem('lvOb4', 360, 1430, 0),
        new LouversItem('lvOb5', 65, 1286, 0),
        new LouversItem('lvOb6', 65, 882, 0),
        new LouversItem('lvKoupD', 65, 510, 0),

        new LouversItem('lvCh1', 55, 850, 1),
        new LouversItem('lvCh2', 55, 512, 1),
        new LouversItem('lvVrt1', 260, 118, 1),
        new LouversItem('lvVrt2', 370, 50, 1),
        new LouversItem('lvVrt3', 530, 298, 1),
    ];
}


function getSensorComponents() {
    return [
        new SensorItem('pisD', 0, 0, 0),
        new SensorItem('pisH', 0, 0, 1),
    ];
}

function getComponents() {
    return [
        new StairsItem('stairsUp', 127, 690, 0, 1, '▲'),
        new StairsItem('stairsDown', 127, 690, 1, 0, '▼'),
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
    return [
        new WaterPumpItem(),
        new InverterItem(),
        new EPriceItem(),
    ]
}