function getBaseUrl() {
    return 'http://10.0.0.150';
}

function getComponents() {
    return [
        //id, x, y, floor
        ['pwmVrt1', 342, 207, 1],
        ['pwmVrt2', 436, 207, 1],

        ['stairsUp', 127, 690, 0],
        ['stairsDown', 127, 690, 1],

        ['lvVrt2', 370, 50, 1],

        ['vlVrt', 450, 330, 1]
        //
        //['pirPrdDv', 378, 376, 0],
        //['pirPrdPr', 378, 227, 0],
        //['pirSch', 133, 797, 0],
        //['pirJid', 456, 726, 0],
        //['pirObyv', 449, 1009, 0],
        //['pirChD', 256, 552, 0],
        //['pirKoupD', 187, 493, 0],
        //['pirSpa', 229, 353, 0],
        //['pirZadD', 428, 548, 0],

    ];
}
