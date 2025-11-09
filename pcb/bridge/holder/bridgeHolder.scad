$fn = 32;

//connectorWidth = 37.5;
//boardWidth = 41;
connectorWidth = 56;
boardWidth = 53;

module parts() {
    translate([-0.5, 3, -0.5]) cube([40, 5, 17]);
    translate([2, 2.5, 2]) {
        // connector
        translate([0, (boardWidth-connectorWidth) / 2, 0]) cube([5.8, connectorWidth, 14.5]);
        // board
        translate([0, 0, 18.5]) {
            #cube([39, boardWidth, 1.6]);
            translate([5, 0, - 2.3]) cube([29, boardWidth, 10]);
        }
        // fix 1
        translate([8, - 5, 11.5]) cube([2, 50, 4]);
        // fix 2
        translate([- 3.5, - 5, 11.5]) cube([2, 50, 4]);
    }
}


module reduction() {
    difference() {
        rotate([90, 0]) {
            translate([0, 0, - 5])
                linear_extrude(5) {
                    hull() {
                        translate([0, 19]) square([43, 4.7]);
                        translate([0, 12]) square([25, 5]);
                    }
                    translate([0,1]) square([9.3, 11]);
                }
        }
        parts();
    }
}

difference() {
    reduction();
    translate([(39)/2+2,9,18.5+0.4]) {
            rotate([90, 0]) hull() {
                #linear_extrude(10) circle(d = 3.3);
                translate([0,10]) linear_extrude(10) circle(d = 3.3);
            }
        }
}

difference() {
    translate([- 0.5, 0])
        mirror([- 1, 0])
            reduction();
    translate([-14.8+1,0,0]) cube([5,10,2]);
}