$fn = 96;

difference() {
    union() {
        linear_extrude(1) square([55, 13.5]);
        linear_extrude(1.7) square([55, 3]);
    }
    translate([(55 - 19) / 2, 0]) linear_extrude(10) square([19, 7.5]);
    h = 10;
    w = 14;
    translate([2, (13.5 - h) / 2]) linear_extrude(10) square([w, h]);
    translate([55 - 2 - w, (13.5 - h) / 2]) linear_extrude(10) square([w, h]);

    h2 = 3;
    translate([(55 - 19) / 2, 7.5 + (13.5 - 7.5-h2)/2]) linear_extrude(10) square([19, h2]);
}