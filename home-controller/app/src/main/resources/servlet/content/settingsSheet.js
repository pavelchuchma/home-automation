'use strict';

/**
 * Settings dialog for options of a floor plan widget: a bottom sheet on narrow screens, a centered dialog otherwise.
 * Values are loaded from /rest/options on open and only the changed ones are saved.
 *
 * Config: {title, details, live, store, lanes: [{key, label, color, readOnly}], groups: [{title, hint, toggle, enabledIf, fields: [...]}]}
 * Optional enabledIf(state) of a group disables its controls while it returns false.
 * Optional details: {optionKeys, render(item, options), bind(element, sheet)} adds a Details tab before the settings,
 * re-rendered from the widget's live data on every refresh; options holds the loaded values of optionKeys, the
 * optional bind attaches handlers after each render.
 * Optional live: {render(item, sheet), bind(element, sheet)} is a block above the settings, re-rendered on every
 * refresh and on every edit.
 * Optional store: {load(keys, item), save(changes, item)} returning promises replaces /rest/options.
 * Optional instant: true sends all values of the form after every change (a burst of edits is sent once they stop),
 * so a device that fills missing values from its last reported state cannot revert a change still in flight.
 * The foot has only a Close button.
 * The timeline is drawn only when there are lanes.
 * Field types: 'stepper' {key, label, hint, step, min, max, unit, decimals}, 'switch' {key, label, hint},
 * 'choice' {key, label, options: [{value, label, icon}]} (one of buttons),
 * 'ranges' {key} (time ranges "H:mm-H:mm;..." drawn on the timeline in the color of the lane with the same key).
 */
class SettingsSheet {
    static TIME_STEP_MIN = 15;
    static APPLY_DELAY_MS = 500;
    static current = null;
    static OPTIONS_STORE = {
        load: keys => fetch(getBaseUrl() + '/rest/options?keys=' + encodeURIComponent(keys.join(',')))
            .then(SettingsSheet.checkResponse)
            .then(r => r.json()),
        save: changes => fetch(getBaseUrl() + '/rest/options/save', {method: 'POST', body: new URLSearchParams(changes)})
            .then(SettingsSheet.checkResponse),
    };

    static checkResponse(r) {
        return r.ok ? r : r.text().then(t => Promise.reject(new Error(t)));
    }

    static open(cfg, item) {
        if (SettingsSheet.current) {
            SettingsSheet.current.close();
        }
        SettingsSheet.current = new SettingsSheet(cfg, item);
    }

    static itemDrawn(item) {
        if (SettingsSheet.current && SettingsSheet.current.item === item) {
            SettingsSheet.current.renderDetails();
            SettingsSheet.current.renderLive();
        }
    }

    constructor(cfg, item) {
        this.cfg = cfg;
        this.item = item;
        this.tab = cfg.details ? 'details' : 'settings';
        this.detailsHtml = null;
        this.liveHtml = null;
        this.applyTimer = null;
        this.applying = 0;
        this.loaded = null;
        this.state = null;
        this.confirming = false;
        this.saving = false;
        this.error = '';

        this.overlay = document.createElement('div');
        this.overlay.className = 'settings-overlay';
        const tabs = cfg.details ? `<div class="settings-tabs" role="tablist">
                <button type="button" class="settings-tab" role="tab" data-tab="details">Details</button>
                <button type="button" class="settings-tab" role="tab" data-tab="settings">Settings</button></div>` : '';
        this.overlay.innerHTML = `<div class="settings-backdrop">
            <div class="settings-sheet" role="dialog" aria-modal="true" aria-label="${cfg.title}">
                <div class="settings-grab-area"><div class="settings-grab"></div>
                    <div class="settings-head"><h3>${cfg.title}</h3>
                    <button type="button" class="settings-icon-btn settings-close" aria-label="Close">✕</button></div>${tabs}</div>
                <div class="settings-body settings-details" role="tabpanel"></div>
                <div class="settings-body settings-main" role="tabpanel"></div><div class="settings-foot"></div></div></div>`;
        document.body.appendChild(this.overlay);
        this.backdrop = this.overlay.querySelector('.settings-backdrop');
        this.sheet = this.overlay.querySelector('.settings-sheet');
        this.detailsBody = this.overlay.querySelector('.settings-details');
        this.body = this.overlay.querySelector('.settings-main');
        this.foot = this.overlay.querySelector('.settings-foot');
        this.overlay.querySelectorAll('.settings-tab').forEach(t => t.addEventListener('click', () => this.showTab(t.dataset.tab)));

        this.backdrop.addEventListener('click', e => {
            if (e.target === this.backdrop) this.requestClose();
        });
        this.overlay.querySelector('.settings-close').addEventListener('click', () => this.requestClose());
        this.onKeyDown = e => {
            if (e.key === 'Escape' && !this.closeTimePicker()) this.requestClose();
        };
        document.addEventListener('keydown', this.onKeyDown);
        this.bindDrag(this.overlay.querySelector('.settings-grab-area'));

        this.onViewportChange = () => this.fitToViewport();
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', this.onViewportChange);
            window.visualViewport.addEventListener('scroll', this.onViewportChange);
        }
        this.fitToViewport();

        this.body.innerHTML = '<div class="settings-skeleton"><div></div><div></div><div></div></div>';
        this.showTab(this.tab);
        this.load();
    }

    showTab(tab) {
        this.closeTimePicker();
        this.tab = tab;
        this.overlay.querySelectorAll('.settings-tab').forEach(t => t.setAttribute('aria-selected', String(t.dataset.tab === tab)));
        this.detailsBody.hidden = tab !== 'details';
        this.body.hidden = tab !== 'settings';
        this.renderDetails();
        this.renderFoot();
    }

    renderDetails() {
        if (this.tab !== 'details') return;
        const html = this.item.hasData() ? this.cfg.details.render(this.item, this.loaded || {})
            : '<p class="settings-hint">Waiting for data…</p>';
        // redrawn on every refresh, so the DOM is touched only when something changed
        if (html !== this.detailsHtml) {
            this.detailsHtml = html;
            this.detailsBody.innerHTML = html;
            if (this.item.hasData() && this.cfg.details.bind) this.cfg.details.bind(this.detailsBody, this);
        }
    }

    /**
     * The floor plan has no viewport meta, so on phones it is zoomed out. The overlay covers the visual viewport
     * and is scaled back so that the dialog keeps its real size.
     */
    fitToViewport() {
        const vv = window.visualViewport;
        const scale = vv ? vv.scale : 1;
        const width = vv ? vv.width : window.innerWidth;
        const height = vv ? vv.height : window.innerHeight;
        Object.assign(this.overlay.style, {
            left: (vv ? vv.offsetLeft : 0) + 'px',
            top: (vv ? vv.offsetTop : 0) + 'px',
            width: width + 'px',
            height: height + 'px',
        });
        Object.assign(this.backdrop.style, {
            width: (width * scale) + 'px',
            height: (height * scale) + 'px',
            transform: `scale(${1 / scale})`,
        });
        this.sheet.classList.toggle('narrow', width * scale < 600);
    }

    allKeys() {
        const keys = this.cfg.lanes.filter(l => l.readOnly).map(l => l.key);
        for (const g of this.cfg.groups) {
            if (g.toggle) keys.push(g.toggle);
            g.fields.forEach(f => keys.push(f.key));
        }
        return keys;
    }

    store() {
        return this.cfg.store || SettingsSheet.OPTIONS_STORE;
    }

    load() {
        const keys = this.allKeys().concat(this.cfg.details ? this.cfg.details.optionKeys : []);
        this.store().load(keys, this.item)
            .then(values => {
                this.loaded = values;
                this.state = {...values};
                this.renderBody();
                this.renderDetails();
            })
            .catch(e => {
                this.loadFailed = true;
                this.body.innerHTML = `<p class="settings-err">Failed to load settings: ${e.message}</p>`;
                this.renderFoot();
            });
    }

    close() {
        if (this.applyTimer) this.apply();
        document.removeEventListener('keydown', this.onKeyDown);
        if (window.visualViewport) {
            window.visualViewport.removeEventListener('resize', this.onViewportChange);
            window.visualViewport.removeEventListener('scroll', this.onViewportChange);
        }
        this.overlay.remove();
        if (SettingsSheet.current === this) {
            SettingsSheet.current = null;
        }
    }

    requestClose() {
        if (this.cfg.instant || this.dirtyKeys().length === 0) {
            this.close();
        } else {
            this.confirming = true;
            this.renderFoot();
        }
    }

    dirtyKeys() {
        if (!this.state) return [];
        return Object.keys(this.state).filter(k => this.state[k] !== this.loaded[k]);
    }

    errors() {
        const errs = {};
        if (!this.state) return errs;
        for (const g of this.cfg.groups) {
            if (g.toggle && this.state[g.toggle] !== 'true') continue;
            for (const f of g.fields) {
                if (f.type !== 'ranges') continue;
                const e = SettingsSheet.validateRanges(this.state[f.key]);
                if (e) errs[f.key] = e;
            }
        }
        return errs;
    }

    save() {
        const changes = {};
        this.dirtyKeys().forEach(k => changes[k] = this.state[k]);
        this.saving = true;
        this.error = '';
        this.renderFoot();
        this.store().save(changes, this.item)
            .then(() => {
                this.close();
                SettingsSheet.showToast('Saved');
            })
            .catch(e => {
                this.saving = false;
                this.error = e.message || 'Saving failed';
                this.renderFoot();
            });
    }

    scheduleApply() {
        clearTimeout(this.applyTimer);
        this.applyTimer = null;
        if (this.dirtyKeys().length === 0 || Object.keys(this.errors()).length > 0) return;
        this.applyTimer = setTimeout(() => this.apply(), SettingsSheet.APPLY_DELAY_MS);
    }

    /**
     * Instant mode: sends all values of the form together with the extra ones.
     * @returns {Promise<boolean>} whether sending succeeded
     */
    apply(extra = {}) {
        clearTimeout(this.applyTimer);
        this.applyTimer = null;
        if (!this.state || (this.dirtyKeys().length === 0 && Object.keys(extra).length === 0)) {
            return Promise.resolve(true);
        }
        const values = {...this.state, ...extra};
        Object.assign(this.loaded, this.state);
        this.applying++;
        this.error = '';
        this.renderFoot();
        return this.store().save(values, this.item)
            .then(() => true)
            .catch(e => {
                this.error = e.message || 'Sending failed';
                return false;
            })
            .finally(() => {
                this.applying--;
                this.renderFoot();
            });
    }

    static showToast(text) {
        const toast = document.createElement('div');
        toast.className = 'settings-toast';
        toast.textContent = text;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 1800);
    }

    bindDrag(handle) {
        let startY = null;
        let dy = 0;
        handle.addEventListener('pointerdown', e => {
            if (e.target.closest('button') || !this.sheet.classList.contains('narrow')) return;
            startY = e.clientY;
            dy = 0;
            handle.setPointerCapture(e.pointerId);
            this.sheet.style.transition = 'none';
        });
        handle.addEventListener('pointermove', e => {
            if (startY === null) return;
            dy = Math.max(0, e.clientY - startY);
            this.sheet.style.transform = `translateY(${dy}px)`;
        });
        const end = () => {
            if (startY === null) return;
            startY = null;
            this.sheet.style.transition = '';
            this.sheet.style.transform = '';
            if (dy > 80) this.requestClose();
        };
        handle.addEventListener('pointerup', end);
        handle.addEventListener('pointercancel', end);
    }

    renderBody() {
        let html = this.cfg.live ? '<div class="settings-live"></div>' : '';
        if (this.cfg.lanes.length) {
            const legend = this.cfg.lanes
                .map(l => `<span><i style="background:${l.color}"></i>${l.label}${l.readOnly ? ' (read-only)' : ''}</span>`)
                .join('');
            html += `<div class="settings-group settings-timeline"><div class="settings-tl"></div>
                <div class="settings-legend">${legend}<span>│ now</span></div></div>`;
        }
        this.cfg.groups.forEach((g, i) => {
            const hint = g.hint ? `<span class="settings-hint">${g.hint}</span>` : '';
            html += `<div class="settings-group" data-group="${i}">`;
            if (g.toggle) {
                html += `<div class="settings-row"><span class="settings-label"><span class="settings-group-title">${g.title}</span>${hint}</span>
                    ${this.switchHtml(g.toggle, g.title)}</div><div class="settings-dep" data-dep="${g.toggle}">`;
            } else {
                html += `<span class="settings-group-title">${g.title}</span>${hint}`;
            }
            html += g.fields.map(f => this.fieldHtml(f)).join('');
            if (g.toggle) html += '</div>';
            html += '</div>';
        });
        this.body.innerHTML = html;
        this.liveHtml = null;
        this.bindBody();
        this.refresh();
    }

    renderLive() {
        const el = this.body.querySelector('.settings-live');
        if (!el) return;
        const html = this.item.hasData() ? this.cfg.live.render(this.item, this)
            : '<p class="settings-hint">Waiting for data…</p>';
        if (html !== this.liveHtml) {
            this.liveHtml = html;
            el.innerHTML = html;
            this.cfg.live.bind(el, this);
        }
    }

    switchHtml(key, label) {
        return `<span class="settings-switch"><input type="checkbox" role="switch" data-key="${key}"
            ${this.state[key] === 'true' ? 'checked' : ''} aria-label="${label}"><span></span></span>`;
    }

    fieldHtml(f) {
        if (f.type === 'switch') {
            return `<div class="settings-row"><span class="settings-label">${f.label}<span class="settings-hint">${f.hint}</span></span>
                ${this.switchHtml(f.key, f.label)}</div>`;
        }
        if (f.type === 'choice') {
            const buttons = f.options.map(o => `<button type="button" data-v="${o.value}" aria-pressed="${this.state[f.key] === o.value}">
                ${o.icon ? `<img src="${o.icon}" alt="">` : ''}<span>${o.label}</span></button>`).join('');
            return `<div class="settings-choice" role="group" aria-label="${f.label}" data-key="${f.key}"
                style="grid-template-columns: repeat(${f.options.length}, minmax(0, 1fr))">${buttons}</div>`;
        }
        if (f.type === 'stepper') {
            const hint = f.hint ? `<span class="settings-hint">${f.hint}</span>` : '';
            return `<div class="settings-row"><span class="settings-label">${f.label}${hint}</span>
                <span class="settings-stepper" data-key="${f.key}">
                    <button type="button" data-d="-1" aria-label="Decrease">−</button>
                    <input inputmode="decimal" value="${this.state[f.key]}" aria-label="${f.label}">
                    <span class="settings-unit">${f.unit}</span>
                    <button type="button" data-d="1" aria-label="Increase">+</button></span></div>`;
        }
        return `<div class="settings-ranges-box" data-key="${f.key}">${this.rangesHtml(f.key)}</div>`;
    }

    rangesHtml(key) {
        const lane = this.cfg.lanes.find(l => l.key === key);
        const rows = SettingsSheet.parseRanges(this.state[key]).map(([a, b], i) => `<div class="settings-range-row">
                <span class="settings-swatch" style="background:${lane.color}"></span>
                ${SettingsSheet.timeFieldHtml(a, i, 0)}<span>–</span>${SettingsSheet.timeFieldHtml(b, i, 1)}
                <button type="button" class="settings-icon-btn" data-remove="${i}" aria-label="Remove range ${i + 1}">✕</button></div>`)
            .join('');
        return `<div class="settings-ranges">${rows || '<span class="settings-hint">No ranges, never active.</span>'}
            <span class="settings-err" hidden></span>
            <button type="button" class="settings-add-range">+ Add range</button></div>`;
    }

    static timeFieldHtml(value, index, part) {
        const label = `Range ${index + 1} ${part === 0 ? 'from' : 'to'}`;
        return `<button type="button" class="settings-time" data-i="${index}" data-p="${part}" aria-label="${label}"
            aria-haspopup="listbox">${value}<span class="settings-clock" aria-hidden="true"></span></button>`;
    }

    fieldDef(key) {
        for (const g of this.cfg.groups) {
            const f = g.fields.find(x => x.key === key);
            if (f) return f;
        }
        return null;
    }

    bindBody() {
        this.body.querySelectorAll('input[type="checkbox"]').forEach(el => el.addEventListener('change', () => {
            this.state[el.dataset.key] = String(el.checked);
            this.refresh();
        }));
        this.body.querySelectorAll('.settings-choice').forEach(ch => ch.addEventListener('click', e => {
            const btn = e.target.closest('button');
            if (!btn) return;
            this.state[ch.dataset.key] = btn.dataset.v;
            this.refresh();
        }));
        this.body.querySelectorAll('.settings-stepper').forEach(st => this.bindStepper(st));
        this.body.querySelectorAll('.settings-ranges-box').forEach(box => this.bindRanges(box));
    }

    bindStepper(st) {
        const key = st.dataset.key;
        const f = this.fieldDef(key);
        const input = st.querySelector('input');
        const set = v => {
            const clamped = Math.min(f.max, Math.max(f.min, Math.round(v / f.step) * f.step));
            this.state[key] = f.decimals ? clamped.toFixed(f.decimals) : String(Math.round(clamped));
            input.value = this.state[key];
            this.refresh();
        };
        input.addEventListener('change', () => {
            const v = parseFloat(input.value.replace(',', '.'));
            set(Number.isNaN(v) ? Number(this.state[key]) : v);
        });
        st.querySelectorAll('button').forEach(btn => {
            let timer = null;
            const stepOnce = () => set(Number(this.state[key]) + f.step * Number(btn.dataset.d));
            const stop = () => {
                clearTimeout(timer);
                clearInterval(timer);
                timer = null;
            };
            btn.addEventListener('pointerdown', e => {
                e.preventDefault();
                stepOnce();
                timer = setTimeout(() => {
                    timer = setInterval(stepOnce, 90);
                }, 400);
            });
            ['pointerup', 'pointerleave', 'pointercancel'].forEach(t => btn.addEventListener(t, stop));
            btn.addEventListener('keydown', e => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    stepOnce();
                }
            });
        });
    }

    bindRanges(box) {
        const key = box.dataset.key;
        box.addEventListener('click', e => {
            const ranges = SettingsSheet.parseRanges(this.state[key]);
            const time = e.target.closest('.settings-time');
            if (time) {
                this.openTimePicker(key, time);
                return;
            }
            const rm = e.target.closest('[data-remove]');
            if (rm) {
                ranges.splice(Number(rm.dataset.remove), 1);
            } else if (e.target.closest('.settings-add-range')) {
                // new range starts one hour after the last one ends
                const last = ranges.length ? SettingsSheet.toMin(ranges[ranges.length - 1][1]) : 11 * 60;
                const start = (last + 60) % 1440;
                ranges.push([SettingsSheet.fmtMin(start), SettingsSheet.fmtMin((start + 60) % 1440)]);
            } else {
                return;
            }
            this.state[key] = SettingsSheet.formatRanges(ranges);
            box.innerHTML = this.rangesHtml(key);
            this.refresh();
        });
    }

    /**
     * Popup with an hour and a minute column, minutes in TIME_STEP_MIN steps. A stored minute off the grid is listed
     * too, so the value stays unchanged until edited.
     */
    openTimePicker(key, button) {
        this.closeTimePicker();
        const i = Number(button.dataset.i);
        const part = Number(button.dataset.p);
        const value = () => SettingsSheet.parseRanges(this.state[key])[i][part].split(':').map(Number);
        const [hour, minute] = value();
        const minutes = [];
        for (let m = 0; m < 60; m += SettingsSheet.TIME_STEP_MIN) {
            minutes.push(m);
        }
        if (!minutes.includes(minute)) {
            minutes.push(minute);
            minutes.sort((x, y) => x - y);
        }
        const column = (unit, values, selected, fmt) => `<div class="settings-picker-col" role="listbox">${values
            .map(v => `<button type="button" role="option" data-u="${unit}" data-v="${v}" aria-selected="${v === selected}">${fmt(v)}</button>`)
            .join('')}</div>`;

        const picker = document.createElement('div');
        picker.className = 'settings-picker';
        picker.innerHTML = column('h', Array.from({length: 24}, (_, h) => h), hour, String)
            + column('m', minutes, minute, m => String(m).padStart(2, '0'));
        this.backdrop.appendChild(picker);
        this.picker = picker;

        // the backdrop may be scaled, see fitToViewport()
        const area = this.backdrop.getBoundingClientRect();
        const factor = this.backdrop.offsetWidth / area.width;
        const r = button.getBoundingClientRect();
        const left = (r.left - area.left) * factor;
        const top = (r.bottom - area.top) * factor + 2;
        picker.style.left = Math.min(left, this.backdrop.offsetWidth - picker.offsetWidth - 4) + 'px';
        picker.style.minWidth = (r.width * factor) + 'px';
        picker.style.top = (top + picker.offsetHeight > this.backdrop.offsetHeight
            ? (r.top - area.top) * factor - picker.offsetHeight - 2 : top) + 'px';
        picker.querySelectorAll('[aria-selected="true"]').forEach(b => {
            b.parentElement.scrollTop = b.offsetTop - b.parentElement.offsetTop;
        });

        picker.addEventListener('click', e => {
            const opt = e.target.closest('[data-u]');
            if (!opt) return;
            let [h, m] = value();
            if (opt.dataset.u === 'h') h = Number(opt.dataset.v);
            else m = Number(opt.dataset.v);
            const ranges = SettingsSheet.parseRanges(this.state[key]);
            ranges[i][part] = SettingsSheet.fmtMin(h * 60 + m);
            this.state[key] = SettingsSheet.formatRanges(ranges);
            const box = this.body.querySelector(`.settings-ranges-box[data-key="${key}"]`);
            box.innerHTML = this.rangesHtml(key);
            this.refresh();
            if (opt.dataset.u === 'm') {
                this.closeTimePicker();
                box.querySelector(`.settings-time[data-i="${i}"][data-p="${part}"]`).focus();
            } else {
                picker.querySelectorAll('[data-u="h"]').forEach(b => b.setAttribute('aria-selected', String(b === opt)));
            }
        });
        this.onPickerOutside = e => {
            if (!picker.contains(e.target) && e.target !== button) this.closeTimePicker();
        };
        this.backdrop.addEventListener('pointerdown', this.onPickerOutside, true);
        this.body.addEventListener('scroll', this.onPickerScroll = () => this.closeTimePicker(), {once: true});
    }

    closeTimePicker() {
        if (!this.picker) return false;
        this.picker.remove();
        this.picker = null;
        this.backdrop.removeEventListener('pointerdown', this.onPickerOutside, true);
        this.body.removeEventListener('scroll', this.onPickerScroll);
        return true;
    }

    refresh() {
        this.body.querySelectorAll('.settings-dep').forEach(d => {
            d.hidden = this.state[d.dataset.dep] !== 'true';
        });
        const tl = this.body.querySelector('.settings-tl');
        if (tl) tl.innerHTML = this.timelineSvg();
        this.body.querySelectorAll('.settings-choice').forEach(ch => ch.querySelectorAll('button')
            .forEach(b => b.setAttribute('aria-pressed', String(this.state[ch.dataset.key] === b.dataset.v))));
        this.body.querySelectorAll('.settings-stepper').forEach(st => {
            const f = this.fieldDef(st.dataset.key);
            const v = Number(this.state[f.key]);
            st.querySelector('[data-d="-1"]').disabled = v <= f.min;
            st.querySelector('[data-d="1"]').disabled = v >= f.max;
        });
        // after the steppers, so an enabled group keeps their min/max state
        this.body.querySelectorAll('[data-group]').forEach(el => {
            const g = this.cfg.groups[Number(el.dataset.group)];
            if (!g.enabledIf) return;
            const enabled = g.enabledIf(this.state);
            el.classList.toggle('settings-disabled', !enabled);
            el.querySelectorAll('button, input').forEach(c => {
                if (!enabled) c.disabled = true;
                else if (!c.matches('.settings-stepper button')) c.disabled = false;
            });
        });
        const errs = this.errors();
        this.body.querySelectorAll('.settings-ranges-box').forEach(box => {
            const e = errs[box.dataset.key];
            const msg = box.querySelector('.settings-err');
            msg.hidden = !e;
            msg.textContent = e ? e.msg : '';
            box.querySelectorAll('.settings-time').forEach(t => t.classList.toggle('bad', !!e && e.bad.has(Number(t.dataset.i))));
        });
        this.confirming = false;
        this.error = '';
        if (this.cfg.instant) this.scheduleApply();
        this.renderFoot();
        if (this.cfg.live) this.renderLive();
    }

    renderFoot() {
        if (this.cfg.instant) {
            this.renderInstantFoot();
            return;
        }
        // the details tab has nothing to save, the foot shows up there only to confirm discarding changes
        this.foot.hidden = this.tab === 'details' && !this.confirming;
        if (this.confirming) {
            this.foot.innerHTML = `<div class="settings-confirm"><span>Discard changes?</span>
                <button type="button" class="settings-btn settings-keep">Keep editing</button>
                <button type="button" class="settings-btn settings-btn-danger settings-discard">Discard</button></div>`;
            this.foot.querySelector('.settings-keep').addEventListener('click', () => {
                this.confirming = false;
                this.renderFoot();
            });
            this.foot.querySelector('.settings-discard').addEventListener('click', () => this.close());
            return;
        }
        const dirty = this.dirtyKeys().length;
        const invalid = Object.keys(this.errors()).length > 0;
        let status = '';
        let isError = false;
        if (this.loadFailed) status = '';
        else if (!this.state) status = 'Loading…';
        else if (this.saving) status = 'Saving…';
        else if (this.error) [status, isError] = [this.error, true];
        else if (invalid) [status, isError] = ['Fix the highlighted ranges', true];
        else if (dirty) status = dirty === 1 ? '1 change' : `${dirty} changes`;
        const statusSpan = document.createElement('span');
        statusSpan.className = 'settings-status' + (isError ? ' settings-err' : '');
        statusSpan.setAttribute('role', 'status');
        statusSpan.textContent = status;
        this.foot.innerHTML = `<button type="button" class="settings-btn settings-cancel">Cancel</button>
            <button type="button" class="settings-btn settings-btn-primary settings-save"
                ${!dirty || invalid || !this.state || this.saving ? 'disabled' : ''}>Save</button>`;
        this.foot.prepend(statusSpan);
        this.foot.querySelector('.settings-cancel').addEventListener('click', () => this.requestClose());
        this.foot.querySelector('.settings-save').addEventListener('click', () => this.save());
    }

    renderInstantFoot() {
        let status = '';
        if (this.error) status = this.error;
        else if (this.applying || this.applyTimer) status = 'Sending…';
        const statusSpan = document.createElement('span');
        statusSpan.className = 'settings-status' + (this.error ? ' settings-err' : '');
        statusSpan.setAttribute('role', 'status');
        statusSpan.textContent = status;
        this.foot.innerHTML = '<button type="button" class="settings-btn settings-cancel">Close</button>';
        this.foot.prepend(statusSpan);
        this.foot.querySelector('.settings-cancel').addEventListener('click', () => this.requestClose());
    }

    timelineSvg() {
        const W = 300, laneH = 12, gap = 4, top = 4;
        const lanes = this.cfg.lanes;
        const bottom = top + lanes.length * (laneH + gap) - gap;
        const H = bottom + 14;
        const x = m => (m / 1440) * W;
        let out = '';
        lanes.forEach((lane, li) => {
            const y = top + li * (laneH + gap);
            out += `<rect x="0" y="${y}" width="${W}" height="${laneH}" rx="2" fill="#eef0ee"></rect>`;
            const toggle = this.cfg.groups.find(g => g.toggle && g.fields.some(f => f.key === lane.key));
            const active = !toggle || this.state[toggle.toggle] === 'true';
            const value = this.state[lane.key];
            if (active && !SettingsSheet.validateRanges(value)) {
                SettingsSheet.parseRanges(value).flatMap(SettingsSheet.segments).forEach(([s, e]) => {
                    out += `<rect x="${x(s)}" y="${y}" width="${Math.max(1, x(e) - x(s))}" height="${laneH}" rx="2" fill="${lane.color}"></rect>`;
                });
            }
        });
        for (let h = 0; h <= 24; h += 6) {
            const anchor = h === 0 ? 'start' : h === 24 ? 'end' : 'middle';
            out += `<line x1="${x(h * 60)}" x2="${x(h * 60)}" y1="${bottom}" y2="${bottom + 3}" stroke="#5b665f"></line>`;
            out += `<text x="${x(h * 60)}" y="${H - 1}" font-size="9" fill="#5b665f" text-anchor="${anchor}">${h}:00</text>`;
        }
        const now = new Date();
        const nowX = x(now.getHours() * 60 + now.getMinutes());
        out += `<line x1="${nowX}" x2="${nowX}" y1="0" y2="${bottom + 2}" stroke="#1d2420" stroke-width="1.5"></line>`;
        return `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Day timeline">${out}</svg>`;
    }

    static parseRanges(str) {
        if (!str) return [];
        return str.split(';').map(s => s.trim().split('-'));
    }

    static formatRanges(ranges) {
        return ranges.map(([a, b]) => `${a}-${b}`).join(';');
    }

    static toMin(t) {
        const [h, m] = t.split(':').map(Number);
        return h * 60 + m;
    }

    static fmtMin(m) {
        return `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')}`;
    }

    static segments([a, b]) {
        const s = SettingsSheet.toMin(a), e = SettingsSheet.toMin(b);
        return e > s ? [[s, e]] : [[s, 1440], [0, e]];
    }

    /**
     * IntervalScheduler treats a range with equal start and end as almost the whole day, and with overlapping
     * ranges the end of one would switch the mode off inside the other one.
     * @returns {{msg: string, bad: Set<number>} | null}
     */
    static validateRanges(str) {
        const ranges = SettingsSheet.parseRanges(str);
        if (ranges.some(r => r.length !== 2 || !/^\d{1,2}:\d{2}$/.test(r[0]) || !/^\d{1,2}:\d{2}$/.test(r[1]))) {
            return {msg: `Invalid time ranges '${str}'.`, bad: new Set()};
        }
        for (let i = 0; i < ranges.length; i++) {
            if (SettingsSheet.toMin(ranges[i][0]) === SettingsSheet.toMin(ranges[i][1])) {
                return {msg: 'A range must end at a different time than it starts.', bad: new Set([i])};
            }
        }
        for (let i = 0; i < ranges.length; i++) {
            for (let j = i + 1; j < ranges.length; j++) {
                for (const [s1, e1] of SettingsSheet.segments(ranges[i])) {
                    for (const [s2, e2] of SettingsSheet.segments(ranges[j])) {
                        if (s1 < e2 && s2 < e1) {
                            return {msg: `Ranges ${i + 1} and ${j + 1} overlap. Merge them or move one.`, bad: new Set([i, j])};
                        }
                    }
                }
            }
        }
        return null;
    }
}
