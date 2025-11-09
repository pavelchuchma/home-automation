//difference(){
module parts() {
    translate([2, 1.25, 2]) {
        translate([0, 3.5 / 2, 0]) cube([6, 37.5, 14.5]);
        translate([0, 0, 18.5]) {
            cube([39, 41, 1.7]);
            translate([5, 0, - 2.3]) cube([29, 41, 10]);
        }
        translate([8, - 5, 11.5]) cube([2, 50, 4]);
        translate([- 3.5, - 5, 11.5]) cube([2, 50, 4]);
    }
}


//#cube([1,1,22.2+1.5]);
module reduction() {
    difference() {
        rotate([90, 0]) {
            translate([0, 0, - 5])
                linear_extrude(5) {
                    hull() {
                        translate([0, 19]) square([43, 4.7]);
                        translate([0, 5]) square([9.3, 5]);
                    }
                    square([9.3, 5]);
                }
        }
        parts();
//        translate([- 1, 2.25, 20.5]) cube([50, 10, 10]);
//        #translate([- 1, 2.25, 20.5]) cube([50, 10, 10]);
    }
}
//reduction();

translate([- 0.5, 0])
    mirror([- 1, 0])
        reduction();