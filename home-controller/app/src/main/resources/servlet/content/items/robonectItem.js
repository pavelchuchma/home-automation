'use strict';


class RobonectItem extends AdditionalSvgToolItem {
    data = {
        mode: undefined,
        status: undefined,
        battery: undefined,
        stopped: undefined,
        home: undefined,
        now: undefined,
        gpsHistory: undefined,
        weatherBreak: undefined,
        weatherBreakReason: undefined
    };

    latestEntryTime;
    localGpsHistory = [];
    border = 0;
    mapMoveLeft = 12;
    mapRotate = -5.7;
    innerWH = Math.max(this.canvasWidth - 2 * this.border, this.canvasHeight - 2 * this.border) * 0.85;
    maxPathAge = 2 * 60 * 60;

    constructor() {
        super('robonect', 80)
    }

    updateToDataProperty() {
        return true;
    }

    // /**
    //  * for testing only
    //  * @param item
    //  */
    // update(item) {
    //     // Load gpsHistory from a local JSON file for testing
    //     const firstHistoryLength = 110;
    //     let fakeItem = this.loadFakeData();
    //     let refreshFromTime = this.getRefreshFromTime();
    //
    //     if (!this._testTime) {
    //         this._testTime = fakeItem.gpsHistory.at(firstHistoryLength).time;
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
    //         if (fakeItem.gpsHistory.length === 0) {
    //             fakeItem.gpsHistory = undefined;
    //         }
    //     }
    //
    //     fakeItem.now = this._testTime;
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
            points: this.calculateMovingIconPoints(5),
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
        this.goatStateIcons.push(this.goatIconTooCold = this.svg.image('img/goat-tooCold.svg'));
        this.goatStateIcons.push(this.goatIconTooRainy = this.svg.image('img/goat-tooRainy.svg'));
        this.goatStateIcons.push(this.goatIconTooDry = this.svg.image('img/goat-tooDry.svg'));
        this.goatStateIcons.push(this.goatIconTooHot = this.svg.image('img/goat-tooHot.svg'));
        this.goatStateIcons.push(this.goatIconTooWet = this.svg.image('img/goat-tooWet.svg'));
        this.goatStateIcons.push(this.goatIconWaitingForGoodWeather = this.svg.image('img/goat-waitingForGoodWeather.svg'));


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
        this.localGpsHistory = this.sliceGpsHistoryFrom(this.localGpsHistory, this.data.now - this.maxPathAge);
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
        // copy the new path to localGpsHistory
        if (this.data.gpsHistory) {
            for (let h of this.data.gpsHistory) {
                const p = this.toStringCoordinates(this.project(h));
                h.transformed = `${p.x},${p.y}`;
                this.localGpsHistory.push(h);
            }
        }

        // draw path - split points into 2-minute buckets mapped to activePaths
        const bucketArrays = Array.from({length: this.activePaths.length}, () => []);
        this.localGpsHistory.forEach(gp => {
            const ageSec = this.data.now - gp.time; // seconds
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

        const lastGpsHistoryEntry = (this.localGpsHistory && this.localGpsHistory.length > 1) ? this.localGpsHistory.at(-1) : undefined;
        this.latestEntryTime = (lastGpsHistoryEntry) ? lastGpsHistoryEntry.time : undefined;
        const latitude = (this.data.home) ? 49.0778717 : (lastGpsHistoryEntry) ? lastGpsHistoryEntry.lat : undefined;
        const longitude = (this.data.home) ? 18.0225450 : (lastGpsHistoryEntry) ? lastGpsHistoryEntry.lon : undefined;

        let showMovingIcon = false;
        let showStoppedIcon = false;
        if (latitude && longitude) {
            const {x, y} = this.project({lat: latitude, lon: longitude});
            if (this.data.stopped || this.data.home || !this.localGpsHistory || this.localGpsHistory.length < 2) {
                this.positionStopped.attr({
                    cx: x,
                    cy: y,
                });
                showStoppedIcon = true;
            } else {
                const secondLastPoint = this.project(this.localGpsHistory.at(-2));
                const angle = Math.atan2(y - secondLastPoint.y, x - secondLastPoint.x);

                this.positionMoving.transform({
                    rotate: angle * 180 / Math.PI - 90,
                    translateX: x,
                    translateY: y,
                    origin: [0, 0],
                });
                showMovingIcon = true;
            }
        }

        this.setVisibility(this.positionMoving, showMovingIcon);
        this.setVisibility(this.positionStopped, showStoppedIcon);
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
            this.setVisibility(icon, icon === this.currentStateIcon);
        })
    }

    chooseCurrentStateIcon() {
        if (this.data.home) {
            switch (this.data.status) {
                case 'CHARGING':
                    return this.goatIconCharging;
                case 'SLEEPING':
                    if (this.data.weatherBreak) {
                        switch (this.data.weatherBreakReason) {
                            case 'toorainy':
                                return this.goatIconTooRainy;
                            case 'toocold':
                                return this.goatIconTooCold;
                            case 'toohot':
                                return this.goatIconTooHot;
                            case 'toowet':
                                return this.goatIconTooWet;
                            case 'toodry':
                                return this.goatIconTooDry;
                            default:
                                return this.goatIconWaitingForGoodWeather;
                        }
                    }
                    return ("STANDBY" === this.timer) ? this.goatIconSleepingAtHomeWithAlarm : this.goatIconSleepingAtHome;
                default:
                    return this.goatIconAtHome;
            }
        } else {
            switch (this.data.status) {
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

    calculateMovingIconPoints(size) {
        const thirdSize = (size / 3).toFixed(2);
        return `-${thirdSize},-${thirdSize} ${thirdSize},-${thirdSize} 0,+${(2 * size / 3).toFixed(2)}`;
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
