'use strict';

/**
 * Electricity prices for the chart of the dialog, filled by the page from /rest/eprice/status.
 */
const FLOOR_HEATER_PRICES = {data: null, showCompared: false};
const FLOOR_HEATER_COLORS = {P: '#c46a12', M: '#1f6f4a'};
const FLOOR_HEATER_FILLS = {P: 'rgba(196, 106, 18, .28)', M: 'rgba(31, 111, 74, .28)'};
const SLOT_MS = 15 * 60_000;

/**
 * Values of the dialog, the same keys as the parameters of FloorHeaterHandler.
 */
function floorHeaterValues(item) {
    return {
        minutes: String(item.minutes),
        schedule: String(item.scheduleUntil > 0),
        ranges: SettingsSheet.formatDateRanges(item.ranges.map(r => ({
            from: SettingsSheet.localIso(r.from), to: SettingsSheet.localIso(r.to), kind: r.kind === 'manual' ? 'M' : 'P',
        }))),
        target: String(item.target),
        laterBonus: String(item.laterBonus),
    };
}

const FLOOR_HEATER_STORE = {
    load: (keys, item) => item.hasData() ? Promise.resolve(floorHeaterValues(item)) : Promise.reject(new Error('No data yet')),
    // instant mode sends the whole form, only the values different from the server go out
    save: (values, item) => {
        const current = floorHeaterValues(item);
        const params = new URLSearchParams();
        if (values.minutes !== current.minutes) params.set('minutes', values.minutes);
        if (values.schedule !== current.schedule) params.set('schedule', values.schedule === 'true' ? 'on' : 'off');
        // a changed schedule replaces the planned ranges, the ones in the form are stale then
        if (values.ranges !== current.ranges && values.schedule === current.schedule) params.set('ranges', values.ranges);
        if (values.target !== current.target) params.set('target', values.target);
        if (values.laterBonus !== current.laterBonus) params.set('laterBonus', values.laterBonus);
        if (!params.toString()) return Promise.resolve();
        return fetch(`${getBaseUrl()}/rest/floorHeater/action?id=${item.id}&${params}`).then(SettingsSheet.checkResponse);
    },
};

function floorHeaterSettings(item) {
    return {
        title: item.name,
        instant: true,
        store: FLOOR_HEATER_STORE,
        sync: floorHeaterValues,
        live: {render: floorHeaterLiveHtml, bind: bindFloorHeaterLive},
        lanes: [],
        groups: [
            {title: 'Morning heating', fields: [
                {type: 'stepper', key: 'minutes', label: 'Duration', hint: 'Total heating time', step: 15, min: 15, max: 480, unit: 'h',
                    format: SettingsSheet.fmtMin},
            ]},
            {title: '', enabledIf: (state, it) => it.canPlan, fields: [
                {type: 'switch', key: 'schedule', label: 'Schedule morning heating',
                    hint: 'Cheapest slots before the target time, one-shot. Needs the prices of the whole window.'},
            ]},
            {title: 'Heating ranges', hint: 'Orange planned, green by hand', fields: [
                {type: 'dateRanges', key: 'ranges', colors: FLOOR_HEATER_COLORS, labels: {M: 'by hand'}, newKind: 'M',
                    summary: floorHeaterSummaryHtml},
            ]},
            {title: 'Planner', hint: 'Shared by all rooms', fields: [
                {type: 'stepper', key: 'target', label: 'Target time', hint: 'Warm by this time', step: 15, min: 4 * 60, max: 12 * 60,
                    format: SettingsSheet.fmtMin},
                {type: 'stepper', key: 'laterBonus', label: 'Later-is-better bonus', hint: 'Kč/kWh per hour before target', step: 5, min: 0, max: 100,
                    format: v => (v / 100).toFixed(2).replace('.', ',')},
            ]},
        ],
    };
}

/**
 * Heating time, energy and estimated cost of the ranges from the known prices; the cost is left out while a slot
 * has no price yet.
 */
function floorHeaterSummaryHtml(value, item) {
    const prices = FLOOR_HEATER_PRICES.data;
    let minutes = 0, kwh = 0, cost = 0, unknown = false;
    for (const r of SettingsSheet.parseDateRanges(value)) {
        const from = SettingsSheet.localMillis(r.from), to = SettingsSheet.localMillis(r.to);
        minutes += (to - from) / 60_000;
        for (let t = Math.floor(from / SLOT_MS) * SLOT_MS; t < to; t += SLOT_MS) {
            const hours = (Math.min(to, t + SLOT_MS) - Math.max(from, t)) / 3_600_000;
            const p = prices ? prices.values.find(v => v.time === t) : null;
            if (!p) {
                unknown = true;
                continue;
            }
            kwh += item.powerW / 1000 * hours;
            cost += item.powerW / 1000 * hours * p.price;
        }
    }
    const kc = v => v.toFixed(2).replace('.', ',');
    return `<div class="settings-summary">
        <div><span class="settings-value">${SettingsSheet.fmtMin(minutes)} h</span></div>
        <div><span class="settings-value">${kc(kwh)} kWh</span></div>
        <div><span class="settings-value">${unknown ? '–' : '≈ ' + kc(cost) + ' Kč'}</span></div></div>`;
}

class FloorHeaterItem extends OnOffItem {
    constructor(id) {
        super(id, 0, 0, -1);
        this.powerW = undefined;
        this.minutes = undefined;
        this.scheduleUntil = undefined;
        this.canPlan = undefined;
        this.target = undefined;
        this.laterBonus = undefined;
        this.manualMinutes = undefined;
        this.ranges = [];
    }

    hasData() {
        return this.updatedWithValidData;
    }

    running(now) {
        return this.ranges.find(r => now >= r.from && now < r.to) || null;
    }

    /**
     * Badge of the tile: total heating time and the span of the ranges, e.g. "3:00 h in 2:15–10:00".
     */
    badgeText() {
        if (!this.ranges.length) return '';
        const total = this.ranges.reduce((s, r) => s + r.to - r.from, 0) / 60_000;
        return `${SettingsSheet.fmtMin(total)} h in ${fmtClock(this.ranges[0].from)}–${fmtClock(this.ranges[this.ranges.length - 1].to)}`;
    }
}

function fmtClock(millis) {
    const d = new Date(millis);
    return `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function fmtWhen(millis) {
    return `${fmtClock(millis)} ${SettingsSheet.fmtDay(millis)}`;
}

/**
 * Next target time after now; a day later when fewer than the heating minutes remain until it
 * (the same rule as FloorHeaterPlanner.nextDeadline).
 */
function floorHeaterDeadline(now, targetMinutes, minutes) {
    const d = new Date(now);
    d.setHours(0, 0, 0, 0);
    let deadline = d.getTime() + targetMinutes * 60_000;
    if (deadline <= now) deadline += 24 * 3_600_000;
    const firstSlot = Math.ceil(now / SLOT_MS) * SLOT_MS;
    if (deadline - firstSlot < minutes * 60_000) deadline += 24 * 3_600_000;
    return deadline;
}

function floorHeaterLiveHtml(item) {
    const now = Date.now();
    const run = item.running(now);
    let why;
    if (run) {
        why = `${run.kind === 'manual' ? 'Switched on by hand' : 'Planned range'}, from ${fmtClock(run.from)} until ${fmtWhen(run.to)}`;
    } else {
        const next = item.ranges.find(r => r.from > now);
        why = next ? `Next range ${fmtClock(next.from)}–${fmtClock(next.to)} ${SettingsSheet.fmtDay(next.from)}` : 'No heating range';
    }
    return `<div class="settings-group">
            <div class="settings-row">
                <span class="settings-label"><span class="settings-state ${run ? 'on' : ''}"><i></i>${run ? 'Heating' : 'Off'}</span>
                    <span class="settings-hint">${why} · ${item.powerW} W</span></span>
                <span class="settings-switch"><input type="checkbox" role="switch" class="settings-fh-switch" ${run ? 'checked' : ''}
                    aria-label="Heating on"><span></span></span></div>
        </div>
        <div class="settings-group">
            <span class="settings-group-title">Electricity price</span>
            <div class="settings-chart">${floorHeaterChartSvg(item, now)}<div class="settings-chart-tip" hidden></div></div>
            <div class="settings-legend"><span><i style="background:#3b5f8f;height:3px"></i>spot price</span>
                <span><i style="background:${FLOOR_HEATER_FILLS.P};border:1px solid ${FLOOR_HEATER_COLORS.P}"></i>planned</span>
                <span><i style="background:${FLOOR_HEATER_FILLS.M};border:1px solid ${FLOOR_HEATER_COLORS.M}"></i>by hand</span>
                <label class="settings-legend-toggle"><input type="checkbox" class="settings-fh-compared" ${FLOOR_HEATER_PRICES.showCompared ? 'checked' : ''}>
                    <i class="settings-legend-dash"></i>price the planner compares</label></div>
        </div>`;
}

function bindFloorHeaterLive(el, sheet) {
    const item = sheet.item;
    el.querySelector('.settings-fh-switch').addEventListener('change', e => {
        // the switch changes the ranges on the server; an unsent (invalid) edit of them would overwrite that later
        if (sheet.state && sheet.state.ranges !== sheet.loaded.ranges) {
            sheet.state.ranges = sheet.loaded.ranges;
            sheet.updateFields(['ranges']);
        }
        fetch(`${getBaseUrl()}/rest/floorHeater/action?id=${item.id}&switch=${e.target.checked ? 'on' : 'off'}`)
            .then(SettingsSheet.checkResponse)
            .catch(err => SettingsSheet.showToast(err.message));
    });
    el.querySelector('.settings-fh-compared').addEventListener('change', e => {
        FLOOR_HEATER_PRICES.showCompared = e.target.checked;
        sheet.renderLive();
    });
    bindFloorHeaterChart(el, item);
}

/**
 * Price steps from the current hour to the end of the target day, heating slots filled in the color of their kind.
 */
function floorHeaterChartSvg(item, now) {
    const W = 360, H = 170, L = 30, R = 8, T = 10, B = 26;
    const prices = FLOOR_HEATER_PRICES.data;
    if (!prices) {
        return `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Electricity price"><text x="${W / 2}" y="${H / 2}" font-size="11"
            fill="#5b665f" text-anchor="middle">Waiting for the prices…</text></svg>`;
    }
    const priceAt = t => {
        const v = prices.values.find(p => p.time <= t && t < p.time + SLOT_MS);
        return v ? v.price : null;
    };
    const deadline = item.scheduleUntil > 0 ? item.scheduleUntil : floorHeaterDeadline(now, item.target, item.minutes);
    const dayStart = new Date(deadline);
    dayStart.setHours(0, 0, 0, 0);
    const x0 = new Date(now).setMinutes(0, 0, 0), x1 = dayStart.getTime() + 24 * 3_600_000;
    const x = t => L + (W - L - R) * (t - x0) / (x1 - x0);
    const vals = [];
    for (let t = x0; t < x1; t += SLOT_MS) {
        const p = priceAt(t);
        if (p !== null) vals.push(p);
    }
    const yMax = Math.ceil((vals.length ? Math.max(...vals) : 6) + 0.4);
    const y = p => T + (H - T - B) * (1 - p / yMax);
    const bonus = item.laterBonus / 100;
    let s = '';
    for (let p = 0; p <= yMax; p++) {
        s += `<line x1="${L}" x2="${W - R}" y1="${y(p)}" y2="${y(p)}" stroke="#c9cfca" stroke-width="${p === 0 ? 1 : .5}"></line>`;
        if (p % 2 === 0) s += `<text x="${L - 5}" y="${y(p) + 3}" font-size="9" fill="#5b665f" text-anchor="end">${p}</text>`;
    }
    s += `<text x="1" y="${T - 2}" font-size="8" fill="#5b665f">Kč/kWh</text>`;
    let unknown = null;
    for (let t = x0; t < x1; t += SLOT_MS) {
        if (priceAt(t) === null) {
            unknown = t;
            break;
        }
    }
    if (unknown !== null) {
        s += `<rect x="${x(unknown)}" y="${T}" width="${x(x1) - x(unknown)}" height="${H - T - B}" fill="#eef0ee"></rect>
            <text x="${(x(unknown) + x(x1)) / 2}" y="${(T + H - B) / 2}" font-size="9" fill="#5b665f" text-anchor="middle">prices not published yet</text>`;
    }
    for (const r of item.ranges) {
        const kind = r.kind === 'manual' ? 'M' : 'P';
        for (let t = Math.floor(r.from / SLOT_MS) * SLOT_MS; t < r.to && t < x1; t += SLOT_MS) {
            const p = priceAt(t);
            const a = Math.max(r.from, t, x0), b = Math.min(r.to, t + SLOT_MS);
            if (b <= a) continue;
            const top = p === null ? y(yMax * 0.15) : y(p);
            s += `<rect x="${x(a) + .5}" y="${top}" width="${Math.max(0, x(b) - x(a) - 1)}" height="${y(0) - top}"
                fill="${FLOOR_HEATER_FILLS[kind]}" stroke="${FLOOR_HEATER_COLORS[kind]}" stroke-width=".6"></rect>`;
        }
    }
    let d = '', dc = '';
    for (let t = x0; t < x1; t += SLOT_MS) {
        const p = priceAt(t);
        if (p === null) break;
        d += `${d ? 'L' : 'M'}${x(t)} ${y(p)} L${x(t + SLOT_MS)} ${y(p)} `;
        if (t < deadline) {
            const compared = Math.min(yMax, p + bonus * (deadline - (t + SLOT_MS)) / 3_600_000);
            dc += `${dc ? 'L' : 'M'}${x(t)} ${y(compared)} L${x(t + SLOT_MS)} ${y(compared)} `;
        }
    }
    s += `<path d="${d}" fill="none" stroke="#3b5f8f" stroke-width="1.8" stroke-linejoin="round"></path>`;
    if (FLOOR_HEATER_PRICES.showCompared) {
        s += `<path d="${dc}" fill="none" stroke="#5b665f" stroke-width="1" stroke-dasharray="3 3"></path>`;
    }
    // ticks every 4 hours from the local midnight of the first day
    const firstDay = new Date(x0).setHours(0, 0, 0, 0);
    for (let t = firstDay; t <= x1; t += 14_400_000) {
        if (t < x0) continue;
        const midnight = new Date(t).getHours() === 0;
        s += `<line x1="${x(t)}" x2="${x(t)}" y1="${y(0)}" y2="${y(0) + (midnight ? 6 : 3)}" stroke="#5b665f"></line>
            <text x="${x(t)}" y="${H - 12}" font-size="9" fill="#5b665f" text-anchor="middle">${fmtClock(t)}</text>`;
        if (midnight) s += `<text x="${x(t)}" y="${H - 2}" font-size="8" fill="#5b665f" text-anchor="middle">${SettingsSheet.fmtDay(t)}</text>`;
    }
    s += `<line x1="${x(now)}" x2="${x(now)}" y1="${T}" y2="${y(0) + 2}" stroke="#1d2420" stroke-width="1.5"></line>
        <text x="${x(now) + 3}" y="${T + 8}" font-size="8" fill="#1d2420">now</text>
        <line x1="${x(deadline)}" x2="${x(deadline)}" y1="${T}" y2="${y(0) + 2}" stroke="#c46a12" stroke-width="1.2" stroke-dasharray="4 3"></line>
        <text x="${x(deadline) + 3}" y="${T + 8}" font-size="8" fill="#c46a12">target ${fmtClock(deadline)}</text>`;
    return `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Electricity price with the heating ranges" data-x0="${x0}" data-x1="${x1}"
        data-deadline="${deadline}" data-ymax="${yMax}">${s}</svg>`;
}

function bindFloorHeaterChart(el, item) {
    const svg = el.querySelector('.settings-chart svg');
    const tip = el.querySelector('.settings-chart-tip');
    const prices = FLOOR_HEATER_PRICES.data;
    if (!svg || !prices || !svg.dataset.x0) return;
    const W = 360, H = 170, L = 30, R = 8, T = 10, B = 26;
    const x0 = Number(svg.dataset.x0), x1 = Number(svg.dataset.x1), deadline = Number(svg.dataset.deadline), yMax = Number(svg.dataset.ymax);
    const fmtKc = v => v.toFixed(2).replace('.', ',');
    const show = ev => {
        const rect = svg.getBoundingClientRect();
        const t = Math.floor((x0 + ((ev.clientX - rect.left) / rect.width * W - L) * (x1 - x0) / (W - L - R)) / SLOT_MS) * SLOT_MS;
        const v = prices.values.find(p => p.time === t);
        if (!v || t < x0 || t >= x1) {
            tip.hidden = true;
            return;
        }
        const compared = v.price + item.laterBonus / 100 * (deadline - (t + SLOT_MS)) / 3_600_000;
        const heating = item.ranges.some(r => t < r.to && t + SLOT_MS > r.from);
        tip.textContent = `${fmtClock(t)}–${fmtClock(t + SLOT_MS)} · ${fmtKc(v.price)} Kč/kWh`
            + (t < deadline ? ` · compared as ${fmtKc(compared)}` : '') + (heating ? ' · heating' : '');
        tip.hidden = false;
        const xMid = (L + (W - L - R) * (t + SLOT_MS / 2 - x0) / (x1 - x0)) / W * rect.width;
        tip.style.left = Math.max(90, Math.min(rect.width - 90, xMid)) + 'px';
        tip.style.top = ((T + (H - T - B) * (1 - v.price / yMax)) / H * rect.height) + 'px';
    };
    svg.addEventListener('pointermove', show);
    svg.addEventListener('pointerdown', show);
    svg.addEventListener('pointerleave', () => {
        tip.hidden = true;
    });
}
