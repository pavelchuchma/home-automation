window.onload = function () {
    try {
        itemCoordinates.forEach(function (item) {
            itemCoordinateMap[item.id] = item;
        });

        toolsCoordinates.forEach(function (tc) {
            toolCoordinateMap[tc.id] = tc;
        });

        drawMainCanvas();
        drawToolsCanvas();
        drawHvacCanvas();
        drawPumpCanvas();
        setInterval(onTimer, 750);
        onTimer();

        // assign types to coordinate CoordinateItem instance
        updateImpl('/rest/all/status', function (request) {
            parseJsonStatusResponse(request, itemStatusMap);
            itemCoordinates.forEach(function (item) {
                const statusItem = itemStatusMap[item.id];
                if (statusItem !== undefined && item.type === undefined) {
                    item.type = statusItem.type;
                }
            });
        });
    } catch (e) {
        printException(e);
    }
};
