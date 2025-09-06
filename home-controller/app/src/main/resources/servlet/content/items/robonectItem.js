'use strict';


class RobonectItem extends AdditionalSvgToolItem {
    mode;
    status;
    battery;
    stopped;
    home;
    latitude;
    longitude;
    latestEntryTime;
    now;
    gpsHistory;
    localGpsHistory = [];
    weatherBreak;

    border = 0;
    mapMoveLeft = 12;
    mapRotate = -5.7;
    innerWH = Math.max(this.canvasWidth - 2 * this.border, this.canvasHeight - 2 * this.border) * 0.85;
    maxPathAge = 40 * 60;

    constructor() {
        super('robonect', 80)
    }

    // /**
    //  * for testing only
    //  * @param item
    //  */
    // update(item) {
    //     // Load gpsHistory from a local JSON file for testing
    //     let fakeItem = this.loadFakeData();
    //     let refreshFromTime = this.getRefreshFromTime();
    //
    //     if (!this._testTime) {
    //         this._testTime = fakeItem.gpsHistory.at(100).time;
    //     } else {
    //         this._testTime += 1;
    //     }
    //
    //     // take only entries before the "current" testTime
    //     for (let i = 0; i < fakeItem.gpsHistory.length; i++) {
    //         if (fakeItem.gpsHistory[i].time > this._testTime) {
    //             fakeItem.gpsHistory = fakeItem.gpsHistory.slice(0, i - 1);
    //             break;
    //         }
    //     }
    //
    //     // take only entries after the "current" testTime
    //     if (refreshFromTime > 0) {
    //         fakeItem.gpsHistory = this.sliceGpsHistoryFrom(fakeItem.gpsHistory, refreshFromTime);
    //     }
    //
    //     fakeItem.now = (fakeItem.gpsHistory && fakeItem.gpsHistory.length > 0) ? fakeItem.gpsHistory.at(-1).time : this.now;
    //     super.update(fakeItem);
    // }

    getRefreshParams() {
        return [['robonectFromTime', this.getRefreshFromTime()]];
    }

    getRefreshFromTime() {
        return (this.latestEntryTime) ? this.latestEntryTime : -this.maxPathAge;
    }

    project({lat, lon}) {
        const maxLat = 49.07847;
        const maxLon = 18.023275;
        const minLat = 49.077736;
        const minLon = 18.022145;
        const cx = this.canvasWidth / 2;
        const cy = this.canvasHeight / 2;

        // Mercator projection
        const xNorm = (lon - minLon) / (maxLon - minLon);
        const yNorm = (Math.log(Math.tan(Math.PI / 4 + (lat * Math.PI / 180) / 2)) -
                Math.log(Math.tan(Math.PI / 4 + (minLat * Math.PI / 180) / 2))) /
            (Math.log(Math.tan(Math.PI / 4 + (maxLat * Math.PI / 180) / 2)) -
                Math.log(Math.tan(Math.PI / 4 + (minLat * Math.PI / 180) / 2)));

        let x = this.border + xNorm * this.innerWH;
        let y = this.border + (1 - yNorm) * this.innerWH;

        // Adjust the rotation angle dynamically for testing
        const testTheta = (-114.6 + this.mapRotate) * Math.PI / 180; // Replace with the correct angle if needed
        const dx = x - cx;
        const dy = y - cy;
        const xr = dx * Math.cos(testTheta) - dy * Math.sin(testTheta);
        const yr = dx * Math.sin(testTheta) + dy * Math.cos(testTheta);

        return {
            x: cx + xr - 5 - this.mapMoveLeft,
            y: cy + yr
        };
    }

    onCanvasCreatedImpl() {
        this.gardenMap = this.svg.image('zahrada.svg', 0, 0, this.canvasWidth, this.canvasHeight);
        this.gardenMap.transform({
            rotate: +0.5 + this.mapRotate,
            scale: 0.20,
            translateX: -27.8 - 12.5 - this.mapMoveLeft,
            translateY: -42 + 5
        });

        this.activePaths = this.createActivePaths(20);

        this.positionMoving = this.svg.polygon().attr({
            fill: 'blue',
            visibility: 'hidden',
        });

        this.positionStopped = this.svg.circle(3).attr({
            fill: 'blue',
            visibility: 'hidden',
        });

        this.goatStateIcons = [];
        const goatIconSize = 25;

        // images converted by https://convertio.co/png-svg/
        this.goatStateIcons.push(this.goatIconAtHome = this.svg.image('img/goat-atHome.svg'));
        this.goatStateIcons.push(this.goatIconBadWeather = this.svg.image('img/goat-badWeather.svg'));
        this.goatStateIcons.push(this.goatIconCharging = this.svg.image('img/goat-charging.svg'));
        this.goatStateIcons.push(this.goatIconCutting = this.svg.image('img/goat-cutting.svg'));
        this.goatStateIcons.push(this.goatIconDead = this.svg.image('img/goat-dead.svg'));
        this.goatStateIcons.push(this.goatIconGoing = this.svg.image('img/goat-going.svg'));
        this.goatStateIcons.push(this.goatIconGoingHome = this.svg.image('img/goat-goingHome.svg'));
        this.goatStateIcons.push(this.goatIconSad = this.svg.image('img/goat-sad.svg'));
        this.goatStateIcons.push(this.goatIconSleeping = this.svg.image('img/goat-sleeping.svg'));
        this.goatStateIcons.push(this.goatIconSleepingAtHome = this.svg.image('img/goat-sleepingAtHome.svg'));
        this.goatStateIcons.push(this.goatIconSleepingAtHomeWithAlarm = this.svg.image('img/goat-sleepingAtHomeWithAlarm.svg'));
        this.goatStateIcons.push(this.goatIconUnknown = this.svg.image('img/goat-unknown.svg'));

        this.goatStateIcons.forEach(icon => {
            icon.size(goatIconSize, goatIconSize).move(this.canvasWidth - goatIconSize - 1, 5);
            icon.attr({visibility: 'hidden'});
        })
        this.currentStateIcon = this.goatIconUnknown;
    }

    createActivePaths(segmentCount) {
        const paths = [];
        const newColor = {r: 255, g: 0, b: 0}; // Red
        const oldColor = {r: 128, g: 128, b: 128}; // Gray

        for (let i = segmentCount - 1; i >= 0; i--) {
            const r = Math.round(newColor.r + (oldColor.r - newColor.r) * (i / (segmentCount - 1)));
            const g = Math.round(newColor.g + (oldColor.g - newColor.g) * (i / (segmentCount - 1)));
            const b = Math.round(newColor.b + (oldColor.b - newColor.b) * (i / (segmentCount - 1)));
            const color = `rgb(${r},${g},${b})`;

            const path = this.svg.polyline().attr({
                fill: 'none',
                stroke: color,
                'stroke-width': 1,
                'stroke-linecap': 'round',
                'stroke-linejoin': 'round'
            });

            paths.push(path);
        }

        return paths;
    }

    dropOldHistory() {
        this.localGpsHistory = this.sliceGpsHistoryFrom(this.localGpsHistory, this.now - this.maxPathAge);
    }

    sliceGpsHistoryFrom(gpsHistory, fromTime) {
        for (let i = 0; i < gpsHistory.length; i++) {
            if (gpsHistory[i].time > fromTime) {
                return gpsHistory.slice(i);
            }
        }
        return [];
    }

    drawImpl() {
        this.dropOldHistory();
        if (this.gpsHistory) {
            for (let h of this.gpsHistory) {
                const p = this.toStringCoordinates(this.project(h));
                h.transformed = `${p.x},${p.y}`;
                this.localGpsHistory.push(h);
            }
        }

        const lastGpsHistoryEntry = (this.localGpsHistory && this.localGpsHistory.length > 0) ? this.localGpsHistory.at(-1) : undefined;
        this.latestEntryTime = (lastGpsHistoryEntry) ? lastGpsHistoryEntry.time : undefined;
        this.latitude = (this.home) ? 49.0778717 : (lastGpsHistoryEntry) ? lastGpsHistoryEntry.lat : undefined;
        this.longitude = (this.home) ? 18.0225450 : (lastGpsHistoryEntry) ? lastGpsHistoryEntry.lon : undefined;

        // draw path - split points into 2-minute buckets mapped to activePaths
        const bucketArrays = Array.from({length: this.activePaths.length}, () => []);
        this.localGpsHistory.forEach(gp => {
            const ageSec = this.now - gp.time; // seconds
            const bucketIndex = Math.floor(ageSec / (this.maxPathAge / this.activePaths.length));
            // Map newest (bucketIndex 0) to the last active path; older to earlier paths
            if (bucketIndex < this.activePaths.length) {
                const pathIndex = Math.max(0, this.activePaths.length - 1 - bucketIndex);
                bucketArrays[pathIndex].push(gp.transformed);
            }
        });

        // join path segments - add the first item at the end of the previous bucket
        for (let i = 1; i < this.activePaths.length; i++) {
            if (bucketArrays[i].length > 0) {
                bucketArrays[i - 1].push(bucketArrays[i][0]);
            }
        }

        this.activePaths.forEach((path, idx) => {
            const pts = bucketArrays[idx].join(' ');
            path.attr({points: pts});
        });

        // draw position
        if (this.latitude !== undefined && this.longitude !== undefined) {
            if (this.stopped || !this.localGpsHistory || this.localGpsHistory.length < 2) {
                const {x, y} = this.project({lat: this.latitude, lon: this.longitude});
                // Display fallback circle
                this.positionStopped.attr({
                    cx: x,
                    cy: y,
                    visibility: 'visible',
                });
                this.positionMoving.attr({visibility: 'hidden'});
            } else {
                const lastPoint = this.project(this.localGpsHistory.at(-1));
                const secondLastPoint = this.project(this.localGpsHistory.at(-2));

                const dx = lastPoint.x - secondLastPoint.x;
                const dy = lastPoint.y - secondLastPoint.y;
                const angle = Math.atan2(dy, dx);

                const size = 5; // Size of the triangle

                this.positionMoving.attr({
                    points: this.calculateMovingIconPoints(lastPoint, angle, size),
                    visibility: 'visible',
                });
                this.positionStopped.attr({visibility: 'hidden'});
            }
        } else {
            this.positionMoving.attr({visibility: 'hidden'});
            this.positionStopped.attr({visibility: 'hidden'});
        }
        this.currentStateIcon = this.chooseCurrentStateIcon();
        this.showCurrentStateIcon();
    }

    toStringCoordinates({x, y}) {
        return {
            x: x.toFixed(2),
            y: y.toFixed(2),
        }
    }

    showCurrentStateIcon() {
        this.goatStateIcons.forEach(icon => {
            icon.attr({visibility: (icon === this.currentStateIcon) ? 'visible' : 'hidden'});
        })
    }

    chooseCurrentStateIcon() {
        if (this.home) {
            switch (this.status) {
                case 'CHARGING':
                    return this.goatIconCharging;
                case 'SLEEPING':
                    return (this.weatherBreak) ? this.goatIconBadWeather :
                        ("STANDBY" === this.timer) ? this.goatIconSleepingAtHomeWithAlarm : this.goatIconSleepingAtHome;
                default:
                    return this.goatIconAtHome;
            }
        } else {
            switch (this.status) {
                case 'MOWING':
                    return this.goatIconCutting;
                case 'SEARCH_CHARGING_STATION':
                case 'PARKING':
                    return this.goatIconGoingHome;
                case 'SEARCHING':
                    return this.goatIconGoing;
                case 'ERROR_STATUS':
                    return this.goatIconSad;
                case 'SLEEPING':
                    return this.goatIconSleeping;
            }
            return this.goatIconUnknown;
        }
    }

    calculateMovingIconPoints({x, y}, angle, size) {
        const halfSize = size / 3;
        const x1 = x + size * Math.cos(angle);
        const y1 = y + size * Math.sin(angle);
        const x2 = x - halfSize * Math.cos(angle - Math.PI / 2);
        const y2 = y - halfSize * Math.sin(angle - Math.PI / 2);
        const x3 = x - halfSize * Math.cos(angle + Math.PI / 2);
        const y3 = y - halfSize * Math.sin(angle + Math.PI / 2);

        return `${x1.toFixed(2)},${y1.toFixed(2)} ${x2.toFixed(2)},${y2.toFixed(2)} ${x3.toFixed(2)},${y3.toFixed(2)}`;
    }

    loadFakeData() {
        try {
            const url = 'items/testRobonectStatus.json';
            const xhr = new XMLHttpRequest();
            xhr.open('GET', url, false);
            xhr.setRequestHeader('Cache-Control', 'no-cache');
            xhr.send();

            if (xhr.status === 200) {
                const data = JSON.parse(xhr.responseText);
                if (data && Array.isArray(data.robonect) && data.robonect.length > 0) {
                    return data.robonect[0];
                }
            }
            return null;
        } catch (e) {
            return null;
        }
    }
}
