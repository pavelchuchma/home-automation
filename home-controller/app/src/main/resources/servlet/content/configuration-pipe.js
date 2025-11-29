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
        new LouversItem('lvLoznice', 45, 116, 0),
        new LouversItem('lvPracovna', 45, 252, 0),
        new LouversItem('lvPracovnaDvere', 45, 316, 0),
        new LouversItem('lvKuchyn', 45, 547, 0),
        new LouversItem('lvObyvak1', 45, 686, 0),
        new LouversItem('lvObyvak2', 325, 842, 0),
        new LouversItem('lvObyvak3', 121, 842, 0),
        new LouversItem('lvObyvak4', 121, 774, 0),
        new LouversItem('lvObyvak5', 192, 774, 0),
        new LouversItem('lvObyvak6', 257, 774, 0),
        new LouversItem('lvObyvak7', 325, 774, 0),
        new LouversItem('lvObyvak8', 395, 709, 0),
        new LouversItem('lvSchodiste', 425, 586, 0),
        new LouversItem('lvSpajz', 422, 433, 0),
        new LouversItem('lvKoupelnaDole', 457, 176, 0),
        new LouversItem('lvDvorek', 412, 86, 0),
        new LouversItem('lvDada', 42, 367, 1),
        new LouversItem('lvOchoz', 459, 584, 1),
        new LouversItem('lvMates2', 512, 424, 1),
        new LouversItem('lvMates1', 554, 356, 1),
        new LouversItem('lvKoupelnaHore', 455, 286, 1),
        new LouversItem('lvJuju', 425, 123, 1),

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
        new StairsItem('stairsUp', 381, 518, 0, 1, '▲'),
        new StairsItem('stairsDown', 420, 518, 1, 0, '▼'),
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