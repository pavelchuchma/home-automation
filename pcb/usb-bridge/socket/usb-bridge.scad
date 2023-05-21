$fn = 96;

module pcb() {
    translate([0, - 8, 0]) {
        // tistak
        cube([18.5, 16, 1.5]);
        // zadni kabely
        translate([0, 0.75, - 3]) cube([2.5, 16 - 1.5, 1.5 + 1.5 + 3]);
        // USB port + soucastky
        translate([0, 3, 1.5])cube([19.4, 10, 3.5]);
        // kabely spodni
        translate([- 7, 3, - 7])cube([18.5 + 7, 10, 7]);
        // ledka
        translate([- 3, 8, - 1]) {
            linear_extrude(6.7) circle(d = 3.1);
            linear_extrude(1) circle(d = 4);
        }
    }
}

module connector() {
    w = 17.4;
    translate([0, - w / 2, 0]) {
        // connector
        cube([6, 17.4, 10.7]);
        translate([0, 4, - 3]) cube([6, w - 4 - 2, 3]);
    }
}

module bok() {
    translate([- 2, 17.4 / 2 - 3]) {
        cube([10, 4, 19]);
        d1 = 3.4;
        translate([- (19.6 - 10 + d1), 0, 14]) cube([21 + d1, 4, 5]);
    }
    // kruh kolem ledky
    translate([- 13, 0, 14 + 2]) linear_extrude(3) circle(d = 10);
    // drzak kruhu kolem ledky
    translate([- 15, - 18 / 2, 14 + 2]) cube([6, 18, 3]);
}

module complete() {
    difference() {
        union() {
            bok();
            mirror([0, 1, 0]) bok();
        }
        // pcb + connector
        translate([0, 0, 2]) {
            connector();
            translate([- 10, 0, 14]) pcb();
        }
    }
}

difference() {
//intersection() {
    complete();
    translate([- 20, - 5, - 1])cube([30, 20, 30]);
}

