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
        new LouversItem('lvLoznice', 38, 106, 0),
        new LouversItem('lvPracovna', 38, 290, 0),
        new LouversItem('lvPracovnaDvere', 38, 379, 0),
        new LouversItem('lvKuchyn', 38, 641, 0),
        new LouversItem('lvObyvak1', 38, 753, 0),
        new LouversItem('lvObyvak2', 67, 1050, 0),
        new LouversItem('lvObyvak3', 270, 1050, 0),
        new LouversItem('lvObyvak4', 67, 967, 0),
        new LouversItem('lvObyvak5', 137, 967, 0),
        new LouversItem('lvObyvak6', 206, 967, 0),
        new LouversItem('lvObyvak7', 270, 967, 0),
        new LouversItem('lvObyvak8', 300, 861, 0),
        new LouversItem('lvSchodiste', 351, 713, 0),
        new LouversItem('lvSpajz', 351, 473, 0),
        new LouversItem('lvKoupelnaDole', 412, 242, 0),
        new LouversItem('lvDvorek', 351, 100, 0),
        new LouversItem('lvDada', 39, 380, 1),
        new LouversItem('lvOchoz', 350, 658, 1),
        new LouversItem('lvMates2', 550, 401, 1),
        new LouversItem('lvMates1', 550, 316, 1),
        new LouversItem('lvKoupelnaHore', 418, 242, 1),
        new LouversItem('lvJuju', 350, 100, 1),

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
        new StairsItem('stairsUp', 339, 561, 0, 1, '▲'),
        new StairsItem('stairsDown', 339, 561, 1, 0, '▼'),
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