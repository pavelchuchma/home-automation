'use strict';

function getBaseUrl() {
    // return 'http://localhost';
    return 'http://192.168.68.150';
}

function getFloorIds() {
    return ['1stFloor', '2ndFloor'];
}

function getComponents() {
    return [
        new PwmLightItem('pwmKuLi', 400, 650, 0),
        new PwmLightItem('pwmKch1', 410, 759, 0),
        new PwmLightItem('pwmKch2', 382, 708, 0),
        new PwmLightItem('pwmKch3', 335, 731, 0),
        new PwmLightItem('pwmKch4', 268, 686, 0),
        new PwmLightItem('pwmKch5', 285, 808, 0),
        new PwmLightItem("pwmJid1", 250, 880, 0),
        new PwmLightItem("pwmJid2", 142, 896, 0),
        new PwmLightItem("pwmJid3", 205, 962, 0),
        new PwmLightItem("pwmJdl", 382, 891, 0),
        new PwmLightItem('pwmOb1', 375, 1338, 0),
        new PwmLightItem('pwmOb2', 337, 1303, 0),
        new PwmLightItem('pwmOb3', 402, 1269, 0),
        new PwmLightItem('pwmOb4', 380, 1199, 0),
        new PwmLightItem('pwmOb5', 427, 1112, 0),
        new PwmLightItem('pwmOb6', 390, 1037, 0),
        new PwmLightItem('pwmOb7', 260, 1340, 0),
        new PwmLightItem('pwmOb8', 236, 1269, 0),
        // new PwmLightItem('pwmOb9', 217, 1217, 0),
        new PwmLightItem('pwmOb10', 160, 1135, 0),
        new PwmLightItem('pwmOb11', 270, 1157, 0),
        new PwmLightItem('pwmOb12', 185, 1276, 0),
        new PwmLightItem('pwmOb13', 189, 1327, 0),
        new PwmLightItem('pwmPrd1', 407, 408, 0),
        new PwmLightItem('pwmPrd2', 361, 283, 0),
        new PwmLightItem('pwmPrd3', 189, 158, 0),
        new PwmLightItem('pwmZadD', 390, 544, 0),
        new PwmLightItem('pwmChoD', 266, 517, 0),
        new PwmLightItem('pwmKpD', 153, 489, 0),
        new PwmLightItem('pwmKpDZrc', 189, 544, 0),
        new PwmLightItem('pwmSpjz', 206, 328, 0),
        // new PwmLightItem('sklepL', 518, 345, 0),
        // new PwmLightItem('sklepP', 567, 350, 0),
        new PwmLightItem('pwmG2', 45, 210, 1),
        new PwmLightItem('pwmG1', 65, 150, 1),
        new PwmLightItem('pwmG3', 25, 270, 1),
        new PwmLightItem('pwmTrs', 550, 595, 0),
        new PwmLightItem('pwmDrv', 130, 1459, 0),
        new PwmLightItem('pwmSchd', 57, 760, 0),

        new LouversItem('lvKuch', 510, 809, 0),
        new LouversItem('lvOb1', 510, 978, 0),
        new LouversItem('lvOb2', 510, 1121, 0),
        new LouversItem('lvOb3', 510, 1285, 0),
        new LouversItem('lvOb4', 360, 1430, 0),
        new LouversItem('lvOb5', 65, 1286, 0),
        new LouversItem('lvOb6', 65, 882, 0),
        new LouversItem('lvKoupD', 65, 510, 0),

        new StairsItem('stairsUp', 127, 690, 0, 1, '▲'),
        new StairsItem('stairsDown', 127, 690, 1, 0, '▼'),

        new PwmLightItem('pwmVchH', 167, 97, 1),
        new PwmLightItem('pwmVrt1', 342, 207, 1),
        new PwmLightItem('pwmVrt2', 436, 207, 1),
        new PwmLightItem('pwmZadH', 196, 289, 1),
        new PwmLightItem('pwmPuda', 220, 30, 1),
        new PwmLightItem('pwmChSch', 137, 564, 1),
        new PwmLightItem('pwmChP', 208, 564, 1),
        new PwmLightItem('pwmKpH', 373, 446, 1),
        new PwmLightItem('pwmKpHZrc', 349, 516, 1),
        new PwmLightItem('pwmKry', 349, 636, 1),
        new PwmLightItem('pwmPata', 349, 830, 1),
        new PwmLightItem('pwmMarek', 349, 1039, 1),
        new PwmLightItem('pwmLozM', 309, 1280, 1),
        new PwmLightItem('pwmLozV', 431, 1280, 1),
        new PwmLightItem('pwmPrac', 167, 1324, 1),
        new PwmLightItem('pwmSat', 194, 1128, 1),
        new PwmLightItem('pwmWc', 134, 953, 1),

        new LouversItem('lvVrt1', 260, 118, 1),
        new LouversItem('lvVrt2', 370, 50, 1),
        new LouversItem('lvVrt3', 530, 298, 1),
        new LouversItem('lvKoupH', 530, 473, 1),
        new LouversItem('lvKrys', 530, 640, 1),
        new LouversItem('lvPata', 530, 823, 1),
        new LouversItem('lvMarek', 530, 1006, 1),
        new LouversItem('lvLoz1', 530, 1311, 1),
        new LouversItem('lvLoz2', 397, 1440, 1),
        new LouversItem('lvPrc', 55, 1325, 1),
        new LouversItem('lvSat', 55, 1085, 1),
        new LouversItem('lvCh1', 55, 850, 1),
        new LouversItem('lvCh2', 55, 512, 1),

        new AirValveItem('vlVrt', 450, 330, 1),
        new AirValveItem('vlPrc', 132, 1254, 1),
        new AirValveItem('vlKoupD', 172, 433, 0),
        new AirValveItem('vlObyv45', 450, 1380, 0),
        new AirValveItem('vlObyv23', 450, 1193, 0),
        new AirValveItem('vlJid', 450, 980, 0),
        new AirValveItem('vlMarek', 450, 1068, 1),
        new AirValveItem('vlPata', 450, 726, 1),
        new AirValveItem('vlKoupH', 450, 515, 1),

        new PirItem('pirPrdDv', 378, 376, 0),
        new PirItem('pirPrdPr', 378, 227, 0),
        new PirItem('pirSch', 133, 797, 0),
        new PirItem('pirJid', 456, 726, 0),
        new PirItem('pirObyv', 449, 1009, 0),
        new PirItem('pirChD', 256, 552, 0),
        new PirItem('pirKoupD', 187, 493, 0),
        new PirItem('pirSpa', 229, 353, 0),
        new PirItem('pirZadD', 428, 548, 0),

        new PirItem('pirVchH', 165, 48, 1),
        new PirItem('pirChWc', 179, 794, 1),
        new PirItem('pirCh', 179, 563, 1),
        new PirItem('pirWc', 131, 919, 1),
        new PirItem('pirZadHVch', 179, 232, 1),
        new PirItem('pirZadHCh', 179, 346, 1),
        new PirItem('pirChMa', 207, 946, 1),

        new BaseItem('mgntGH', 18, 61, 1),
        new BaseItem('mgntGD', 18, 117, 1),
        new BaseItem('mgntCrpd', 438, 376, 0),

        new HvacItem('hvac', 0, 0, 0),
        new WaterPumpItem('wpump', 0, 0, 0)
    ];
}

function getToolbarItems() {
    return [
        new ToolBarItem('lightToggle', function (x, y, ctx) {
            PwmLightItem.drawIcon(x - 10, y, 0, ctx);
            PwmLightItem.drawIcon(x + 10, y, .75, ctx);
        }, [PwmLightItem.name, StairsItem.name], 'toggle'),

        new ToolBarItem('lightPlus', function (x, y, ctx) {
            PwmLightItem.drawIcon(x, y, .66, ctx);
            drawLightToolSign(x, y, ctx, true);
        }, [PwmLightItem.name, StairsItem.name], 'plus'),

        new ToolBarItem('lightMinus', function (x, y, ctx) {
            PwmLightItem.drawIcon(x, y, .25, ctx);
            drawLightToolSign(x, y, ctx, false);
        }, [PwmLightItem.name, StairsItem.name], 'minus'),

        new ToolBarItem('lightFull', function (x, y, ctx) {
            PwmLightItem.drawIcon(x, y, 1, ctx);
        }, [PwmLightItem.name, StairsItem.name], 'full'),

        new ToolBarItem('lightOff', function (x, y, ctx) {
            PwmLightItem.drawIcon(x, y, 0, ctx);
        }, [PwmLightItem.name, StairsItem.name], 'off'),

        new ToolBarItem('louversUp', function (x, y, ctx) {
            LouversItem.drawIcon(x, y, .3, 0, 'stopped', ctx, 50, 60)
        }, [LouversItem.name, StairsItem.name], 'up'),

        new ToolBarItem('louversOutshine', function (x, y, ctx) {
            LouversItem.drawIcon(x, y, 1, 0, 'stopped', ctx, 50, 60);
        }, [LouversItem.name, StairsItem.name], 'outshine'),

        new ToolBarItem('louversDown', function (x, y, ctx) {
            LouversItem.drawIcon(x, y, 1, 1, 'stopped', ctx, 50, 60);
        }, [LouversItem.name, StairsItem.name], 'blind'),

        new ToolBarItem('valveToggle', function (x, y, ctx) {
            AirValveItem.drawIcon(x + 10, y - 5, 1, 'stopped', 'red', ctx);
            AirValveItem.drawIcon(x - 10, y + 5, 0, 'stopped', 'green', ctx);
        }, [AirValveItem.name, StairsItem.name], 'toggle')
    ];
}