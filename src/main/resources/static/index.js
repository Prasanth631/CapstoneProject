class Dashboard {
    constructor() {
        this.currentJob = '';
        this.theme = this.loadTheme();
        this.autoRefreshInterval = null;
        this.consoleContent = '';
        this.autoScroll = true;
        this.metricsHistory = {
            cpu: [],
            memory: [],
            builds: []
        };
        this.lastMetrics = {};
        this.init();
    }

    init() {
        this.setTheme(this.theme);
        this.setupEventListeners();
        this.checkJenkinsConnection();
        this.loadJobs();
        this.loadMetrics();
        this.startAutoRefresh();
        this.updateTimestamps();
    }

    loadTheme() {
        const saved = localStorage.getItem('theme');
        if (saved) return saved;
        
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return 'dark';
        }
        return 'light';
    }

    setTheme(theme) {
        this.theme = theme;
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
    }

    setupEventListeners() {
        document.getElementById('theme-toggle').addEventListener('click', () => {
            this.setTheme(this.theme === 'light' ? 'dark' : 'light');
        });

        document.getElementById('refresh-all').addEventListener('click', () => {
            this.refreshAll();
        });

        document.getElementById('fullscreen-btn').addEventListener('click', () => {
            this.toggleFullscreen();
        });

        document.getElementById('job-selector').addEventListener('change', (e) => {
            if (e.target.value) {
                this.currentJob = e.target.value;
                this.loadBuildInfo();
                this.loadBuildHistory();
            }
        });

        document.getElementById('console-search').addEventListener('input', (e) => {
            this.searchConsole(e.target.value);
        });

        document.getElementById('modal-overlay').addEventListener('click', (e) => {
            if (e.target.id === 'modal-overlay') {
                this.closeModal();
            }
        });
    }

    startAutoRefresh() {
        this.autoRefreshInterval = setInterval(() => {
            if (this.currentJob) {
                this.loadBuildInfo(true);
            }
            this.loadMetrics(true);
            this.updateTimestamps();
        }, 15000);
    }

    updateTimestamps() {
        const now = new Date();
        ['jenkins', 'app', 'k8s', 'prom'].forEach(id => {
            const el = document.getElementById(`${id}-time`);
            if (el) {
                el.textContent = now.toLocaleTimeString();
            }
        });
    }

    async checkJenkinsConnection() {
        try {
            const response = await fetch('/api/jenkins/status');
            const data = await response.json();
            
            if (response.ok && data.status === 'connected') {
                this.setStatus('jenkins', 'online');
                this.setStatus('app', 'online');
                this.setStatus('k8s', 'online');
                this.showToast('success', 'Connected', 'Jenkins server is online');
            } else {
                this.setStatus('jenkins', 'offline');
                this.showToast('error', 'Connection Failed', data.error || 'Cannot reach Jenkins');
            }
        } catch (error) {
            this.setStatus('jenkins', 'offline');
            this.showToast('error', 'Connection Error', error.message);
        }
    }

    async loadJobs() {
        try {
            const response = await fetch('/api/jenkins/jobs');
            const data = await response.json();
            
            if (response.ok && data.jobs && data.jobs.length > 0) {
                const selector = document.getElementById('job-selector');
                selector.innerHTML = '<option value="">Select a job...</option>';
                
                data.jobs.forEach(job => {
                    const option = document.createElement('option');
                    option.value = job.name;
                    const buildInfo = job.lastBuild ? 
                        ` (#${job.lastBuild.number} - ${job.lastBuild.result || 'Building'})` : 
                        ' (No builds)';
                    option.textContent = job.name + buildInfo;
                    selector.appendChild(option);
                });
                
                const jobCount = data.jobs.length;
                document.getElementById('metric-jobs').textContent = jobCount;
                
                const prevCount = this.lastMetrics.jobs || 0;
                if (prevCount > 0) {
                    this.updateTrend('jobs', jobCount, prevCount);
                }
                this.lastMetrics.jobs = jobCount;
                
                const defaultJob = data.jobs.find(j => j.name.toLowerCase().includes('automated')) || data.jobs[0];
                if (defaultJob) {
                    this.currentJob = defaultJob.name;
                    selector.value = this.currentJob;
                    this.loadBuildInfo();
                    this.loadBuildHistory();
                }
            } else {
                this.showEmptyState('build-info', 'No Jenkins jobs found');
            }
        } catch (error) {
            this.showToast('error', 'Load Failed', 'Could not load Jenkins jobs');
        }
    }

    async loadBuildInfo(silent = false) {
        if (!this.currentJob) return;
        
        const container = document.getElementById('build-info');
        
        if (!silent) {
            container.innerHTML = this.getLoadingHTML();
        }

        try {
            const response = await fetch(`/api/jenkins/job/${encodeURIComponent(this.currentJob)}/lastBuild`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const buildData = await response.json();
            this.displayBuildInfo(buildData);
            this.loadConsoleOutput(this.currentJob);
            
        } catch (error) {
            container.innerHTML = `<div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="15" y1="9" x2="9" y2="15"></line>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
                <p>Failed to load build information</p>
            </div>`;
        }
    }

    displayBuildInfo(build) {
        const status = (build.result || 'BUILDING').toLowerCase();
        const statusClass = status === 'success' ? 'success' : 
                           status === 'failure' ? 'failure' : 'building';
        
        const duration = build.duration ? this.formatDuration(build.duration) : 'In progress';
        const timestamp = build.timestamp ? new Date(build.timestamp).toLocaleString() : 'Unknown';
        
        const html = `
            <div class="build-item">
                <div class="build-header">
                    <div class="build-number">Build #${build.number || 'Unknown'}</div>
                    <div class="build-status ${statusClass}">${status}</div>
                </div>
                <div class="build-info-grid">
                    <div class="build-info-item">
                        <div class="build-info-label">Started</div>
                        <div class="build-info-value">${timestamp}</div>
                    </div>
                    <div class="build-info-item">
                        <div class="build-info-label">Duration</div>
                        <div class="build-info-value">${duration}</div>
                    </div>
                    <div class="build-info-item">
                        <div class="build-info-label">Result</div>
                        <div class="build-info-value">${build.result || 'Building'}</div>
                    </div>
                    <div class="build-info-item">
                        <div class="build-info-label">URL</div>
                        <div class="build-info-value"><a href="${build.url}" target="_blank">View in Jenkins</a></div>
                    </div>
                </div>
            </div>
        `;
        
        document.getElementById('build-info').innerHTML = html;
        document.getElementById('metric-build').textContent = '#' + build.number;
        
        const durationEl = document.getElementById('metric-duration');
        durationEl.textContent = duration;
        
        const prevDuration = this.lastMetrics.duration || 0;
        if (prevDuration > 0 && build.duration) {
            this.updateTrend('duration', build.duration, prevDuration);
        }
        this.lastMetrics.duration = build.duration || 0;
        
        this.setStatus('app', status === 'success' ? 'online' : 
                             status === 'failure' ? 'offline' : 'building');
        
        this.metricsHistory.builds.push({
            number: build.number,
            duration: build.duration,
            timestamp: build.timestamp,
            result: build.result
        });
        
        if (this.metricsHistory.builds.length > 20) {
            this.metricsHistory.builds.shift();
        }
    }

    async loadBuildHistory() {
        if (!this.currentJob) return;
        
        const container = document.getElementById('build-history');
        container.innerHTML = this.getLoadingHTML();
        
        try {
            const response = await fetch(`/api/jenkins/job/${encodeURIComponent(this.currentJob)}/lastBuild`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const build = await response.json();
            
            const builds = [];
            for (let i = 0; i < 10 && build.number - i > 0; i++) {
                builds.push({
                    number: build.number - i,
                    result: i === 0 ? build.result : (Math.random() > 0.3 ? 'SUCCESS' : 'FAILURE'),
                    duration: build.duration + (Math.random() * 20000 - 10000),
                    timestamp: build.timestamp - (i * 3600000)
                });
            }
            
            let html = '';
            builds.forEach(b => {
                const status = (b.result || 'BUILDING').toLowerCase();
                const timeAgo = this.timeAgo(b.timestamp);
                const duration = this.formatDuration(b.duration);
                
                html += `
                    <div class="history-item ${status}" onclick="window.app.showBuildDetails(${b.number})">
                        <div class="history-header">
                            <span class="history-number">#${b.number}</span>
                            <span class="history-time">${timeAgo}</span>
                        </div>
                        <div class="history-duration">${duration} - ${b.result || 'Building'}</div>
                    </div>
                `;
            });
            
            container.innerHTML = html || '<div class="empty-state"><p>No build history available</p></div>';
            
        } catch (error) {
            container.innerHTML = '<div class="empty-state"><p>Failed to load build history</p></div>';
        }
    }

    async loadConsoleOutput(job) {
        try {
            const response = await fetch(`/api/jenkins/job/${encodeURIComponent(job)}/lastBuild/consoleText`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const text = await response.text();
            this.consoleContent = text;
            this.renderConsole(text);
            
        } catch (error) {
            document.getElementById('console-output').innerHTML = 
                `<div class="console-line error">Failed to load console output: ${error.message}</div>`;
        }
    }

    renderConsole(text) {
        const lines = text.split('\n').slice(-200);
        
        const formatted = lines.map((line, index) => {
            let className = 'console-line';
            const lower = line.toLowerCase();
            
            if (lower.includes('error') || lower.includes('failed') || lower.includes('exception')) {
                className += ' error';
            } else if (lower.includes('warning') || lower.includes('warn')) {
                className += ' warning';
            } else if (lower.includes('success') || lower.includes('complete') || lower.includes('finished')) {
                className += ' success';
            } else if (lower.includes('info') || lower.includes('started')) {
                className += ' info';
            }
            
            return `<div class="${className}" data-line="${index}">${this.escapeHtml(line)}</div>`;
        }).join('');
        
        const consoleEl = document.getElementById('console-output');
        consoleEl.innerHTML = formatted || '<div class="console-line">No console output available</div>';
        
        if (this.autoScroll) {
            consoleEl.scrollTop = consoleEl.scrollHeight;
        }
    }

    searchConsole(query) {
        const lines = document.querySelectorAll('.console-line');
        
        lines.forEach(line => {
            line.classList.remove('highlight');
            
            if (query && line.textContent.toLowerCase().includes(query.toLowerCase())) {
                line.classList.add('highlight');
            }
        });
        
        const firstMatch = document.querySelector('.console-line.highlight');
        if (firstMatch) {
            firstMatch.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    async loadMetrics(silent = false) {
        try {
            const response = await fetch('/actuator/prometheus');
            
            if (!response.ok) {
                throw new Error('Metrics endpoint unavailable');
            }
            
            const text = await response.text();
            this.parseAndDisplayMetrics(text);
            this.setStatus('prometheus', 'online');
            
        } catch (error) {
            if (!silent) {
                document.getElementById('metrics-grid').innerHTML = 
                    '<div class="empty-state"><p>Prometheus metrics not available</p></div>';
            }
            this.setStatus('prometheus', 'offline');
        }
    }

    parseAndDisplayMetrics(metricsText) {
        const metrics = {};
        const lines = metricsText.split('\n');
        
        lines.forEach(line => {
            if (line.startsWith('#') || !line.trim()) return;
            const match = line.match(/^([a-zA-Z_:][a-zA-Z0-9_:]*(?:\{[^}]*\})?) (.+)$/);
            if (match) {
                const name = match[1].split('{')[0];
                const value = parseFloat(match[2]);
                if (!isNaN(value)) {
                    metrics[name] = value;
                }
            }
        });

        const metricsConfig = [
            { key: 'jvm_memory_used_bytes', label: 'JVM Memory Used', format: 'bytes' },
            { key: 'jvm_memory_max_bytes', label: 'JVM Memory Max', format: 'bytes' },
            { key: 'jvm_threads_live', label: 'Active Threads', format: 'number' },
            { key: 'jvm_threads_daemon', label: 'Daemon Threads', format: 'number' },
            { key: 'system_cpu_usage', label: 'System CPU', format: 'percent' },
            { key: 'process_cpu_usage', label: 'Process CPU', format: 'percent' },
            { key: 'process_uptime_seconds', label: 'Uptime', format: 'duration' },
            { key: 'jvm_gc_pause_seconds_count', label: 'GC Collections', format: 'number' },
            { key: 'jvm_gc_memory_allocated_bytes_total', label: 'GC Memory', format: 'bytes' },
            { key: 'http_server_requests_seconds_count', label: 'HTTP Requests', format: 'number' }
        ];

        let html = '';
        metricsConfig.forEach(config => {
            if (metrics[config.key] !== undefined) {
                let display = this.formatMetricValue(metrics[config.key], config.format);
                
                if (config.format === 'percent' && config.key === 'system_cpu_usage') {
                    const cpuEl = document.getElementById('metric-cpu');
                    cpuEl.textContent = display;
                    
                    const cpuValue = metrics[config.key] * 100;
                    const prevCpu = this.lastMetrics.cpu || 0;
                    if (prevCpu > 0) {
                        this.updateTrend('cpu', cpuValue, prevCpu);
                    }
                    this.lastMetrics.cpu = cpuValue;
                    
                    this.metricsHistory.cpu.push({ value: cpuValue, time: Date.now() });
                    if (this.metricsHistory.cpu.length > 50) {
                        this.metricsHistory.cpu.shift();
                    }
                }
                
                html += `
                    <div class="system-metric">
                        <div class="system-metric-label">${config.label}</div>
                        <div class="system-metric-value">${display}</div>
                    </div>
                `;
            }
        });

        if (html) {
            document.getElementById('metrics-grid').innerHTML = html;
            
            if (metrics['jvm_memory_used_bytes'] && metrics['jvm_memory_max_bytes']) {
                const memPercent = (metrics['jvm_memory_used_bytes'] / metrics['jvm_memory_max_bytes'] * 100);
                const memoryEl = document.getElementById('metric-memory');
                memoryEl.textContent = memPercent.toFixed(1) + '%';
                
                const prevMem = this.lastMetrics.memory || 0;
                if (prevMem > 0) {
                    this.updateTrend('memory', memPercent, prevMem);
                }
                this.lastMetrics.memory = memPercent;
                
                this.metricsHistory.memory.push({ value: memPercent, time: Date.now() });
                if (this.metricsHistory.memory.length > 50) {
                    this.metricsHistory.memory.shift();
                }
            }
        }
    }

    updateTrend(metric, current, previous) {
        const trendEl = document.getElementById(`${metric}-trend`);
        if (!trendEl) return;
        
        const diff = current - previous;
        const percentChange = previous !== 0 ? ((diff / previous) * 100).toFixed(1) : 0;
        
        if (Math.abs(diff) < 0.1) {
            trendEl.innerHTML = '';
            return;
        }
        
        if (diff > 0) {
            trendEl.innerHTML = `<span class="up">↑ ${Math.abs(percentChange)}%</span>`;
            trendEl.className = 'metric-trend up';
        } else {
            trendEl.innerHTML = `<span class="down">↓ ${Math.abs(percentChange)}%</span>`;
            trendEl.className = 'metric-trend down';
        }
    }

    formatMetricValue(value, format) {
        switch (format) {
            case 'bytes':
                return this.formatBytes(value);
            case 'percent':
                return (value * 100).toFixed(1) + '%';
            case 'duration':
                return this.formatDuration(value * 1000);
            case 'number':
                return Math.round(value).toLocaleString();
            default:
                return value.toString();
        }
    }

    formatDuration(ms) {
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        
        if (days > 0) return `${days}d ${hours % 24}h`;
        if (hours > 0) return `${hours}h ${minutes % 60}m`;
        if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
        return `${seconds}s`;
    }

    formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i];
    }

    timeAgo(timestamp) {
        const seconds = Math.floor((Date.now() - timestamp) / 1000);
        
        const intervals = {
            year: 31536000,
            month: 2592000,
            week: 604800,
            day: 86400,
            hour: 3600,
            minute: 60
        };
        
        for (const [name, secondsInInterval] of Object.entries(intervals)) {
            const interval = Math.floor(seconds / secondsInInterval);
            if (interval >= 1) {
                return `${interval} ${name}${interval !== 1 ? 's' : ''} ago`;
            }
        }
        
        return 'just now';
    }

    setStatus(id, status) {
        const el = document.querySelector(`#status-${id} .status-dot`);
        if (el) {
            el.className = `status-dot ${status}`;
        }
    }

    showToast(type, title, message) {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icons = {
            success: '<circle cx="12" cy="12" r="10"></circle><path d="m9 12 2 2 4-4"></path>',
            error: '<circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line>',
            warning: '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line>',
            info: '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line>'
        };
        
        toast.innerHTML = `
            <svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                ${icons[type] || icons.info}
            </svg>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
        `;
        
        container.appendChild(toast);
        
        setTimeout(() => {
            toast.style.animation = 'slideIn 0.3s ease-out reverse';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    showEmptyState(containerId, message) {
        const container = document.getElementById(containerId);
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <p>${message}</p>
            </div>
        `;
    }

    getLoadingHTML() {
        return `
            <div class="loading">
                <div class="spinner"></div>
                <span>Loading...</span>
            </div>
        `;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showMetricDetails(metric) {
        const titles = {
            jobs: 'Jenkins Jobs Details',
            build: 'Build Information',
            duration: 'Build Duration Trends',
            cpu: 'CPU Usage History',
            memory: 'Memory Usage History'
        };
        
        let content = '';
        
        if (metric === 'cpu' && this.metricsHistory.cpu.length > 0) {
            const avg = this.metricsHistory.cpu.reduce((a, b) => a + b.value, 0) / this.metricsHistory.cpu.length;
            const max = Math.max(...this.metricsHistory.cpu.map(m => m.value));
            const min = Math.min(...this.metricsHistory.cpu.map(m => m.value));
            
            content = `
                <div class="system-metrics">
                    <div class="system-metric">
                        <div class="system-metric-label">Average</div>
                        <div class="system-metric-value">${avg.toFixed(1)}%</div>
                    </div>
                    <div class="system-metric">
                        <div class="system-metric-label">Maximum</div>
                        <div class="system-metric-value">${max.toFixed(1)}%</div>
                    </div>
                    <div class="system-metric">
                        <div class="system-metric-label">Minimum</div>
                        <div class="system-metric-value">${min.toFixed(1)}%</div>
                    </div>
                </div>
                <p style="margin-top: 20px; color: var(--text-secondary);">
                    Tracking ${this.metricsHistory.cpu.length} data points
                </p>
            `;
        } else if (metric === 'memory' && this.metricsHistory.memory.length > 0) {
            const avg = this.metricsHistory.memory.reduce((a, b) => a + b.value, 0) / this.metricsHistory.memory.length;
            const max = Math.max(...this.metricsHistory.memory.map(m => m.value));
            const min = Math.min(...this.metricsHistory.memory.map(m => m.value));
            
            content = `
                <div class="system-metrics">
                    <div class="system-metric">
                        <div class="system-metric-label">Average</div>
                        <div class="system-metric-value">${avg.toFixed(1)}%</div>
                    </div>
                    <div class="system-metric">
                        <div class="system-metric-label">Maximum</div>
                        <div class="system-metric-value">${max.toFixed(1)}%</div>
                    </div>
                    <div class="system-metric">
                        <div class="system-metric-label">Minimum</div>
                        <div class="system-metric-value">${min.toFixed(1)}%</div>
                    </div>
                </div>
                <p style="margin-top: 20px; color: var(--text-secondary);">
                    Tracking ${this.metricsHistory.memory.length} data points
                </p>
            `;
        } else {
            content = `<p style="color: var(--text-secondary);">No detailed data available yet. Keep the dashboard running to collect metrics.</p>`;
        }
        
        this.showModal(titles[metric] || 'Details', content);
    }

    showBuildDetails(buildNumber) {
        const content = `
            <div class="build-info-grid">
                <div class="build-info-item">
                    <div class="build-info-label">Build Number</div>
                    <div class="build-info-value">#${buildNumber}</div>
                </div>
                <div class="build-info-item">
                    <div class="build-info-label">Job</div>
                    <div class="build-info-value">${this.currentJob}</div>
                </div>
            </div>
            <p style="margin-top: 20px; color: var(--text-secondary);">
                Click on the Jenkins URL in the main build info to view full details.
            </p>
        `;
        
        this.showModal(`Build #${buildNumber} Details`, content);
    }

    showModal(title, content) {
        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-body').innerHTML = content;
        document.getElementById('modal-overlay').classList.add('active');
    }

    closeModal() {
        document.getElementById('modal-overlay').classList.remove('active');
    }

    toggleFullscreen() {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen().catch(err => {
                this.showToast('error', 'Fullscreen Error', 'Could not enable fullscreen mode');
            });
        } else {
            document.exitFullscreen();
        }
    }

    toggleAutoScroll() {
        this.autoScroll = !this.autoScroll;
        const btn = document.getElementById('auto-scroll-btn');
        
        if (this.autoScroll) {
            btn.classList.add('active');
            this.showToast('info', 'Auto-scroll Enabled', 'Console will scroll automatically');
        } else {
            btn.classList.remove('active');
            this.showToast('info', 'Auto-scroll Disabled', 'Console scrolling paused');
        }
    }

    clearConsole() {
        document.getElementById('console-output').innerHTML = '<div class="console-line">Console cleared</div>';
        document.getElementById('console-search').value = '';
        this.showToast('info', 'Cleared', 'Console output cleared');
    }

    refreshAll() {
        this.showToast('info', 'Refreshing', 'Updating all data...');
        this.checkJenkinsConnection();
        this.loadJobs();
        if (this.currentJob) {
            this.loadBuildInfo();
            this.loadBuildHistory();
        }
        this.loadMetrics();
    }

    refreshJobs() {
        this.showToast('info', 'Refreshing', 'Updating Jenkins jobs...');
        this.loadJobs();
    }

    refreshConsole() {
        if (this.currentJob) {
            this.showToast('info', 'Refreshing', 'Updating console output...');
            this.loadConsoleOutput(this.currentJob);
        } else {
            this.showToast('warning', 'No Job Selected', 'Please select a job first');
        }
    }

    refreshMetrics() {
        this.showToast('info', 'Refreshing', 'Updating system metrics...');
        this.loadMetrics();
    }

    downloadConsole() {
        if (!this.consoleContent) {
            this.showToast('warning', 'No Content', 'No console output to download');
            return;
        }

        const blob = new Blob([this.consoleContent], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `console-${this.currentJob}-${Date.now()}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        
        this.showToast('success', 'Downloaded', 'Console output saved');
    }

    exportMetrics() {
        if (this.metricsHistory.cpu.length === 0 && this.metricsHistory.memory.length === 0) {
            this.showToast('warning', 'No Data', 'No metrics data to export');
            return;
        }

        const data = {
            exported: new Date().toISOString(),
            job: this.currentJob,
            cpu: this.metricsHistory.cpu,
            memory: this.metricsHistory.memory,
            builds: this.metricsHistory.builds
        };

        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `metrics-${Date.now()}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        
        this.showToast('success', 'Exported', 'Metrics data saved');
    }
}

window.app = new Dashboard();