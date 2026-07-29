class KineticGrid {
    constructor(options = {}) {
        this.canvas = document.getElementById(options.canvasId || 'kinetic-canvas');
        if (!this.canvas) return;
        
        this.ctx = this.canvas.getContext('2d');
        
        this.spacing = options.spacing || 40;
        this.radius = options.radius || 400;
        this.strength = options.strength || 4;
        this.trail = options.trail !== undefined ? options.trail : true;
        
        this.theme = {
            dotColor: '#374151',
            lineColor: '#B6FF2E',
            trailColor: '#B6FF2E'
        };
        
        this.mouse = { x: -9999, y: -9999, active: false };
        this.trailRef = [];
        
        this.W = 1;
        this.H = 1;
        this.cols = [];
        this.dots = [];
        
        this.GAP = Math.max(8, this.spacing);
        this.R = Math.max(1, this.radius);
        this.PULL = (Math.max(1, Math.min(10, this.strength)) / 10) * 4;
        
        this.raf = 0;
        
        this.build();
        this.bindEvents();
        this.observeTheme();
        this.updateThemeColors();
        
        this.frame = this.frame.bind(this);
        this.raf = requestAnimationFrame(this.frame);
    }
    
    updateThemeColors() {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        if (isDark) {
            this.theme.dotColor = '#374151';
            this.theme.lineColor = '#B6FF2E';
            this.theme.trailColor = '#B6FF2E';
        } else {
            this.theme.dotColor = '#E5E7EB';
            this.theme.lineColor = '#064E3B';
            this.theme.trailColor = '#064E3B';
        }
    }

    observeTheme() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.attributeName === 'data-theme') {
                    this.updateThemeColors();
                }
            });
        });
        observer.observe(document.documentElement, { attributes: true });
    }

    build() {
        this.W = window.innerWidth;
        this.H = window.innerHeight;
        const dpr = window.devicePixelRatio || 1;
        
        this.canvas.width = Math.floor(this.W * dpr);
        this.canvas.height = Math.floor(this.H * dpr);
        this.canvas.style.width = this.W + "px";
        this.canvas.style.height = this.H + "px";
        this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

        this.cols = [];
        this.dots = [];
        const nCols = Math.floor(this.W / this.GAP) + 2;
        const nRows = Math.floor(this.H / this.GAP) + 2;
        
        for (let c = 0; c < nCols; c++) {
            const col = [];
            for (let rIdx = 0; rIdx < nRows; rIdx++) {
                const hx = c * this.GAP;
                const hy = rIdx * this.GAP;
                const d = { hx, hy, x: hx, y: hy, vx: 0, vy: 0 };
                col.push(d);
                this.dots.push(d);
            }
            this.cols.push(col);
        }
    }

    setMouse(clientX, clientY) {
        const r = this.canvas.getBoundingClientRect();
        const mx = clientX - r.left;
        const my = clientY - r.top;
        this.mouse.x = mx;
        this.mouse.y = my;
        this.mouse.active = true;
        const now = performance.now();
        this.trailRef.push({ x: mx, y: my, t: now });
        if (this.trailRef.length > 80) this.trailRef.shift();
    }

    bindEvents() {
        window.addEventListener('resize', () => {
            this.build();
        });

        document.body.addEventListener("mousemove", (e) => this.setMouse(e.clientX, e.clientY));
        document.body.addEventListener("mouseleave", () => {
            this.mouse.active = false;
            this.mouse.x = -9999;
            this.mouse.y = -9999;
        });
        document.body.addEventListener("touchmove", (e) => {
            const t = e.touches[0];
            if (t) this.setMouse(t.clientX, t.clientY);
        }, { passive: true });
        document.body.addEventListener("touchend", () => {
            this.mouse.active = false;
            this.mouse.x = -9999;
            this.mouse.y = -9999;
        });
    }

    frame() {
        const m = this.mouse;
        this.ctx.clearRect(0, 0, this.W, this.H);

        for (const d of this.dots) {
            let ax = (d.hx - d.x) * 0.08;
            let ay = (d.hy - d.y) * 0.08;
            if (m.active) {
                const dx = m.x - d.x;
                const dy = m.y - d.y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < this.R && dist > 0.001) {
                    const f = (1 - dist / this.R) * this.PULL;
                    ax += (dx / dist) * f;
                    ay += (dy / dist) * f;
                }
            }
            d.vx = (d.vx + ax) * 0.82;
            d.vy = (d.vy + ay) * 0.82;
            d.x += d.vx;
            d.y += d.vy;
        }

        for (let c = 0; c < this.cols.length; c++) {
            for (let rIdx = 0; rIdx < this.cols[c].length; rIdx++) {
                const d = this.cols[c][rIdx];
                const right = this.cols[c + 1]?.[rIdx];
                const down = this.cols[c]?.[rIdx + 1];
                const prox = m.active
                    ? Math.max(0, 1 - Math.sqrt((m.x - d.x) ** 2 + (m.y - d.y) ** 2) / this.R)
                    : 0;
                
                if (right) {
                    this.ctx.globalAlpha = 0.06 + prox * 0.7;
                    this.ctx.strokeStyle = this.theme.lineColor;
                    this.ctx.lineWidth = 0.5 + prox * 1.5;
                    this.ctx.beginPath();
                    this.ctx.moveTo(d.x, d.y);
                    this.ctx.lineTo(right.x, right.y);
                    this.ctx.stroke();
                }
                if (down) {
                    this.ctx.globalAlpha = 0.06 + prox * 0.7;
                    this.ctx.strokeStyle = this.theme.lineColor;
                    this.ctx.lineWidth = 0.5 + prox * 1.5;
                    this.ctx.beginPath();
                    this.ctx.moveTo(d.x, d.y);
                    this.ctx.lineTo(down.x, down.y);
                    this.ctx.stroke();
                }
            }
        }

        for (const d of this.dots) {
            const prox = m.active
                ? Math.max(0, 1 - Math.sqrt((m.x - d.x) ** 2 + (m.y - d.y) ** 2) / this.R)
                : 0;
            this.ctx.globalAlpha = 0.22 + prox * 0.78;
            this.ctx.fillStyle = this.theme.dotColor;
            this.ctx.beginPath();
            this.ctx.arc(d.x, d.y, 0.8 + prox * 2.2, 0, 2 * Math.PI);
            this.ctx.fill();
        }

        if (this.trail) {
            const now = performance.now();
            const tr = this.trailRef;
            this.ctx.lineCap = "round";
            this.ctx.lineJoin = "round";
            for (let i = 1; i < tr.length; i++) {
                const a = tr[i - 1];
                const b = tr[i];
                const age = now - b.t;
                if (age > 260) continue;
                this.ctx.globalAlpha = Math.max(0, 1 - age / 260) * 0.85;
                this.ctx.strokeStyle = this.theme.trailColor;
                this.ctx.lineWidth = 2;
                this.ctx.beginPath();
                this.ctx.moveTo(a.x, a.y);
                this.ctx.lineTo(b.x, b.y);
                this.ctx.stroke();
            }
        }

        this.ctx.globalAlpha = 1;
        this.raf = requestAnimationFrame(this.frame);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if(document.getElementById('kinetic-canvas')) {
        new KineticGrid({
            canvasId: 'kinetic-canvas',
            spacing: 35,
            radius: 350,
            strength: 4,
            trail: true
        });
    }
});
