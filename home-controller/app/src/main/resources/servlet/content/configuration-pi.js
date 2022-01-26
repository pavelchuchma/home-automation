function getBaseUrl() {
    return 'http://192.168.68.150';
}

class CoordinateItem {
    constructor(id, x, y, floor) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.floor = floor;
    }
}

function getComponents() {
    return [
        new CoordinateItem('pwmKuLi', 400, 650, 0),
        new CoordinateItem('pwmKch1', 410, 759, 0),
        new CoordinateItem('pwmKch2', 382, 708, 0),
        new CoordinateItem('pwmKch3', 335, 731, 0),
        new CoordinateItem('pwmKch4', 268, 686, 0),
        new CoordinateItem('pwmKch5', 285, 808, 0),
        new CoordinateItem("pwmJid1", 250, 880, 0),
        new CoordinateItem("pwmJid2", 142, 896, 0),
        new CoordinateItem("pwmJid3", 205, 962, 0),
        new CoordinateItem("pwmJdl", 382, 891, 0),
        new CoordinateItem('pwmOb1', 375, 1338, 0),
        new CoordinateItem('pwmOb2', 337, 1303, 0),
        new CoordinateItem('pwmOb3', 402, 1269, 0),
        new CoordinateItem('pwmOb4', 380, 1199, 0),
        new CoordinateItem('pwmOb5', 427, 1112, 0),
        new CoordinateItem('pwmOb6', 390, 1037, 0),
        new CoordinateItem('pwmOb7', 260, 1340, 0),
        new CoordinateItem('pwmOb8', 236, 1269, 0),
        // new CoordinateItem('pwmOb9', 217, 1217, 0),
        new CoordinateItem('pwmOb10', 160, 1135, 0),
        new CoordinateItem('pwmOb11', 270, 1157, 0),
        new CoordinateItem('pwmOb12', 185, 1276, 0),
        new CoordinateItem('pwmOb13', 189, 1327, 0),
        new CoordinateItem('pwmPrd1', 407, 408, 0),
        new CoordinateItem('pwmPrd2', 361, 283, 0),
        new CoordinateItem('pwmPrd3', 189, 158, 0),
        new CoordinateItem('pwmZadD', 390, 544, 0),
        new CoordinateItem('pwmChoD', 266, 517, 0),
        new CoordinateItem('pwmKpD', 153, 489, 0),
        new CoordinateItem('pwmKpDZrc', 189, 544, 0),
        new CoordinateItem('pwmSpjz', 206, 328, 0),
        // new CoordinateItem('sklepL', 518, 345, 0),
        // new CoordinateItem('sklepP', 567, 350, 0),
        new CoordinateItem('pwmG1', 65, 150, 1),
        new CoordinateItem('pwmG2', 45, 210, 1),
        new CoordinateItem('pwmG3', 25, 270, 1),
        new CoordinateItem('pwmTrs', 550, 595, 0),
        new CoordinateItem('pwmDrv', 130, 1459, 0),
        new CoordinateItem('pwmSchd', 57, 760, 0),

        new CoordinateItem('lvKuch', 510, 809, 0),
        new CoordinateItem('lvOb1', 510, 978, 0),
        new CoordinateItem('lvOb2', 510, 1121, 0),
        new CoordinateItem('lvOb3', 510, 1285, 0),
        new CoordinateItem('lvOb4', 360, 1430, 0),
        new CoordinateItem('lvOb5', 65, 1286, 0),
        new CoordinateItem('lvOb6', 65, 882, 0),
        new CoordinateItem('lvKoupD', 65, 510, 0),

        new CoordinateItem('stairsUp', 127, 690, 0),
        new CoordinateItem('stairsDown', 127, 690, 1),

        new CoordinateItem('pwmVchH', 167, 97, 1),
        new CoordinateItem('pwmVrt1', 342, 207, 1),
        new CoordinateItem('pwmVrt2', 436, 207, 1),
        new CoordinateItem('pwmZadH', 196, 289, 1),
        new CoordinateItem('pwmPuda', 220, 30, 1),
        new CoordinateItem('pwmChSch', 137, 564, 1),
        new CoordinateItem('pwmChP', 208, 564, 1),
        new CoordinateItem('pwmKpH', 373, 446, 1),
        new CoordinateItem('pwmKpHZrc', 349, 516, 1),
        new CoordinateItem('pwmKry', 349, 636, 1),
        new CoordinateItem('pwmPata', 349, 830, 1),
        new CoordinateItem('pwmMarek', 349, 1039, 1),
        new CoordinateItem('pwmLozM', 309, 1280, 1),
        new CoordinateItem('pwmLozV', 431, 1280, 1),
        new CoordinateItem('pwmPrac', 167, 1324, 1),
        new CoordinateItem('pwmSat', 194, 1128, 1),
        new CoordinateItem('pwmWc', 134, 953, 1),

        new CoordinateItem('lvVrt1', 260, 118, 1),
        new CoordinateItem('lvVrt2', 370, 50, 1),
        new CoordinateItem('lvVrt3', 530, 298, 1),
        new CoordinateItem('lvKoupH', 530, 473, 1),
        new CoordinateItem('lvKrys', 530, 640, 1),
        new CoordinateItem('lvPata', 530, 823, 1),
        new CoordinateItem('lvMarek', 530, 1006, 1),
        new CoordinateItem('lvLoz1', 530, 1311, 1),
        new CoordinateItem('lvLoz2', 397, 1440, 1),
        new CoordinateItem('lvPrc', 55, 1325, 1),
        new CoordinateItem('lvSat', 55, 1085, 1),
        new CoordinateItem('lvCh1', 55, 850, 1),
        new CoordinateItem('lvCh2', 55, 512, 1),

        new CoordinateItem('vlVrt', 450, 330, 1),
        new CoordinateItem('vlPrc', 132, 1254, 1),
        new CoordinateItem('vlKoupD', 172, 433, 0),
        new CoordinateItem('vlObyv45', 450, 1380, 0),
        new CoordinateItem('vlObyv23', 450, 1193, 0),
        new CoordinateItem('vlJid', 450, 980, 0),
        new CoordinateItem('vlMarek', 450, 1068, 1),
        new CoordinateItem('vlPata', 450, 726, 1),
        new CoordinateItem('vlKoupH', 450, 515, 1),

        new CoordinateItem('pirPrdDv', 378, 376, 0),
        new CoordinateItem('pirPrdPr', 378, 227, 0),
        new CoordinateItem('pirSch', 133, 797, 0),
        new CoordinateItem('pirJid', 456, 726, 0),
        new CoordinateItem('pirObyv', 449, 1009, 0),
        new CoordinateItem('pirChD', 256, 552, 0),
        new CoordinateItem('pirKoupD', 187, 493, 0),
        new CoordinateItem('pirSpa', 229, 353, 0),
        new CoordinateItem('pirZadD', 428, 548, 0),

        new CoordinateItem('pirVchH', 165, 48, 1),
        new CoordinateItem('pirChWc', 179, 794, 1),
        new CoordinateItem('pirCh', 179, 563, 1),
        new CoordinateItem('pirWc', 131, 919, 1),
        new CoordinateItem('pirZadHVch', 179, 232, 1),
        new CoordinateItem('pirZadHCh', 179, 346, 1),
        new CoordinateItem('pirChMa', 207, 946, 1),
        new CoordinateItem('mgntGH', 18, 61, 1),
        new CoordinateItem('mgntGD', 18, 117, 1),

        new CoordinateItem('mgntCrpd', 438, 376, 0),

        new CoordinateItem('hvac', 0, 0, 0)
    ];
}
