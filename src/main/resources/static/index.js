class Dashboard {
    constructor() {
        this.charts = {};
        this.refreshInterval = null;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadDashboardData();
        this.startAutoRefresh();
    }

    setupEventListeners() {
        const refreshBtn = document.getElementById('refresh-all');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.loadDashboardData());
        }

        const fullscreenBtn = document.getElementById('fullscreen-btn');
        if (fullscreenBtn) {
            fullscreenBtn.addEventListener('click', () => this.toggleFullscreen());
        }

        const historyFilter = document.getElementById('history-filter');
        if (historyFilter) {
            historyFilter.addEventListener('change', (e) => {
                this.loadBuildHistory(parseInt(e.target.value));
            });
        }

        const metricsTimeframe = document.getElementById('metrics-timeframe');
        if (metricsTimeframe) {
            metricsTimeframe.addEventListener('change', (e) => {
                this.loadMetricsTimeline(parseInt(e.target.value));
            });
        }
    }

    async loadDashboardData() {
        this.showToast('info', 'Refreshing', 'Loading latest data...');

        await Promise.all([
            this.loadBuildStatistics(),
            this.loadBuildHistory(7),
            this.loadMetricsTimeline(24),
            this.loadCurrentMetrics()
        ]);

        this.showToast('success', 'Updated', 'Dashboard refreshed successfully');
    }

    async loadBuildStatistics() {
        try {
            const response = await fetch('/api/analytics/builds/statistics');
            if (!response.ok) throw new Error('Failed to fetch statistics');

            const stats = await response.json();

            // Update KPI cards
            document.getElementById('kpi-total-builds').textContent = stats.totalBuilds || 0;
            document.getElementById('kpi-success-rate').textContent =
                (stats.successRate || 0).toFixed(1) + '%';

            // Update success rate bar
            const successBar = document.getElementById('success-rate-bar');
            if (successBar) {
                successBar.style.width = (stats.successRate || 0) + '%';
            }

            // Calculate average duration
            if (stats.averageDurationByJob) {
                const durations = Object.values(stats.averageDurationByJob);
                const avgDuration = durations.length > 0
                    ? durations.reduce((a, b) => a + b, 0) / durations.length
                    : 0;
                document.getElementById('kpi-avg-duration').textContent =
                    this.formatDuration(avgDuration * 1000);
            }

            // Load charts
            await this.loadBuildTrendChart();
            await this.loadStatusDistributionChart(stats.statusBreakdown);

        } catch (error) {
            console.error('Error loading build statistics:', error);
            this.showToast('error', 'Error', 'Failed to load build statistics');
        }
    }

    async loadBuildTrendChart() {
        try {
            const response = await fetch('/api/analytics/builds/recent?limit=10');
            if (!response.ok) throw new Error('Failed to fetch recent builds');

            const builds = await response.json();
            builds.reverse(); // Show oldest to newest

            const ctx = document.getElementById('buildTrendChart');
            if (!ctx) return;

            // Destroy existing chart
            if (this.charts.buildTrend) {
                this.charts.buildTrend.destroy();
            }

            this.charts.buildTrend = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: builds.map(b => `#${b.buildNumber}`),
                    datasets: [{
                        label: 'Build Duration (seconds)',
                        data: builds.map(b => (b.durationMs || 0) / 1000),
                        borderColor: 'rgb(99, 102, 241)',
                        backgroundColor: 'rgba(99, 102, 241, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4,
                        pointBackgroundColor: builds.map(b =>
                            b.status === 'SUCCESS' ? 'rgb(34, 197, 94)' : 'rgb(239, 68, 68)'
                        ),
                        pointBorderColor: '#fff',
                        pointBorderWidth: 2,
                        pointRadius: 5,
                        pointHoverRadius: 7
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: 'rgba(15, 23, 42, 0.9)',
                            titleColor: '#fff',
                            bodyColor: '#cbd5e1',
                            borderColor: 'rgba(148, 163, 184, 0.2)',
                            borderWidth: 1,
                            padding: 12,
                            displayColors: false,
                            callbacks: {
                                title: (items) => {
                                    const build = builds[items[0].dataIndex];
                                    return `Build #${build.buildNumber}`;
                                },
                                label: (context) => {
                                    const build = builds[context.dataIndex];
                                    return [
                                        `Duration: ${this.formatDuration(build.durationMs)}`,
                                        `Status: ${build.status}`,
                                        `Job: ${build.jobName}`
                                    ];
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            grid: {
                                color: 'rgba(148, 163, 184, 0.1)'
                            },
                            ticks: {
                                color: '#94a3b8',
                                callback: (value) => value + 's'
                            }
                        },
                        x: {
                            grid: {
                                display: false
                            },
                            ticks: {
                                color: '#94a3b8'
                            }
                        }
                    }
                }
            });

        } catch (error) {
            console.error('Error loading build trend chart:', error);
        }
    }

    async loadStatusDistributionChart(statusBreakdown) {
        const ctx = document.getElementById('statusDistributionChart');
        if (!ctx) return;

        // Destroy existing chart
        if (this.charts.statusDistribution) {
            this.charts.statusDistribution.destroy();
        }

        const statuses = statusBreakdown || {};
        const labels = Object.keys(statuses);
        const data = Object.values(statuses);

        this.charts.statusDistribution = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: [
                        'rgba(34, 197, 94, 0.8)',   // SUCCESS - green
                        'rgba(239, 68, 68, 0.8)',   // FAILURE - red
                        'rgba(251, 191, 36, 0.8)',  // BUILDING - amber
                        'rgba(148, 163, 184, 0.8)'  // OTHER - gray
                    ],
                    borderColor: [
                        'rgb(34, 197, 94)',
                        'rgb(239, 68, 68)',
                        'rgb(251, 191, 36)',
                        'rgb(148, 163, 184)'
                    ],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: '#cbd5e1',
                            padding: 15,
                            font: {
                                size: 12
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(15, 23, 42, 0.9)',
                        titleColor: '#fff',
                        bodyColor: '#cbd5e1',
                        borderColor: 'rgba(148, 163, 184, 0.2)',
                        borderWidth: 1,
                        padding: 12,
                        callbacks: {
                            label: (context) => {
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((context.parsed / total) * 100).toFixed(1);
                                return `${context.label}: ${context.parsed} (${percentage}%)`;
                            }
                        }
                    }
                }
            }
        });
    }

    async loadBuildHistory(days = 7) {
        try {
            const response = await fetch(`/api/analytics/builds/history?days=${days}`);
            if (!response.ok) throw new Error('Failed to fetch build history');

            const builds = await response.json();
            const tbody = document.getElementById('build-history-table');

            if (builds.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center py-8 text-slate-500">
                            <i data-lucide="inbox" class="w-6 h-6 mx-auto mb-2"></i>
                            <p>No builds found in the last ${days} days</p>
                        </td>
                    </tr>
                `;
                lucide.createIcons();
                return;
            }

            tbody.innerHTML = builds.map(build => {
                const statusClass = build.status === 'SUCCESS'
                    ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                    : build.status === 'FAILURE'
                        ? 'bg-red-500/20 text-red-400 border-red-500/30'
                        : 'bg-amber-500/20 text-amber-400 border-amber-500/30';

                return `
                    <tr class="hover:bg-slate-800/30 transition-colors">
                        <td class="py-3 px-4">
                            <span class="font-mono text-sm text-white">#${build.buildNumber}</span>
                        </td>
                        <td class="py-3 px-4">
                            <span class="text-sm text-slate-300">${build.jobName}</span>
                        </td>
                        <td class="py-3 px-4">
                            <span class="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium border ${statusClass}">
                                ${build.status}
                            </span>
                        </td>
                        <td class="py-3 px-4">
                            <span class="text-sm text-slate-300">${this.formatDuration(build.durationMs)}</span>
                        </td>
                        <td class="py-3 px-4">
                            <span class="text-sm text-slate-400">${this.formatTimestamp(build.timestamp)}</span>
                        </td>
                    </tr>
                `;
            }).join('');

        } catch (error) {
            console.error('Error loading build history:', error);
            document.getElementById('build-history-table').innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-8 text-red-400">
                        <p>Failed to load build history</p>
                    </td>
                </tr>
            `;
        }
    }

    async loadMetricsTimeline(hours = 24) {
        try {
            const response = await fetch(`/api/analytics/metrics/history?hours=${hours}`);
            if (!response.ok) throw new Error('Failed to fetch metrics');

            const metrics = await response.json();

            const ctx = document.getElementById('metricsTimelineChart');
            if (!ctx) return;

            // Destroy existing chart
            if (this.charts.metricsTimeline) {
                this.charts.metricsTimeline.destroy();
            }

            this.charts.metricsTimeline = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: metrics.map(m => new Date(m.recordedAt).toLocaleTimeString('en-US', {
                        hour: '2-digit',
                        minute: '2-digit'
                    })),
                    datasets: [
                        {
                            label: 'CPU Usage (%)',
                            data: metrics.map(m => (m.cpuUsage * 100).toFixed(2)),
                            borderColor: 'rgb(168, 85, 247)',
                            backgroundColor: 'rgba(168, 85, 247, 0.1)',
                            borderWidth: 2,
                            fill: true,
                            tension: 0.4,
                            yAxisID: 'y'
                        },
                        {
                            label: 'Memory Usage (%)',
                            data: metrics.map(m => (m.memoryUsage * 100).toFixed(2)),
                            borderColor: 'rgb(34, 197, 94)',
                            backgroundColor: 'rgba(34, 197, 94, 0.1)',
                            borderWidth: 2,
                            fill: true,
                            tension: 0.4,
                            yAxisID: 'y'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                            labels: {
                                color: '#cbd5e1',
                                padding: 15,
                                font: {
                                    size: 12
                                }
                            }
                        },
                        tooltip: {
                            backgroundColor: 'rgba(15, 23, 42, 0.9)',
                            titleColor: '#fff',
                            bodyColor: '#cbd5e1',
                            borderColor: 'rgba(148, 163, 184, 0.2)',
                            borderWidth: 1,
                            padding: 12
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            beginAtZero: true,
                            max: 100,
                            grid: {
                                color: 'rgba(148, 163, 184, 0.1)'
                            },
                            ticks: {
                                color: '#94a3b8',
                                callback: (value) => value + '%'
                            }
                        },
                        x: {
                            grid: {
                                display: false
                            },
                            ticks: {
                                color: '#94a3b8',
                                maxRotation: 45,
                                minRotation: 45
                            }
                        }
                    }
                }
            });

        } catch (error) {
            console.error('Error loading metrics timeline:', error);
        }
    }

    async loadCurrentMetrics() {
        try {
            const response = await fetch('/actuator/prometheus');
            const text = await response.text();

            const metrics = this.parsePrometheusMetrics(text);

            // Update KPI cards
            const cpuUsage = (metrics.system_cpu_usage || 0) * 100;
            const memoryUsed = metrics.jvm_memory_used_bytes || 0;
            const memoryMax = metrics.jvm_memory_max_bytes || 1;
            const memoryUsage = (memoryUsed / memoryMax) * 100;
            const threads = metrics.jvm_threads_live || 0;

            document.getElementById('kpi-cpu-usage').textContent = cpuUsage.toFixed(1) + '%';
            document.getElementById('kpi-memory').textContent = memoryUsage.toFixed(1) + '%';
            document.getElementById('kpi-threads').textContent = Math.round(threads);

        } catch (error) {
            console.error('Error loading current metrics:', error);
        }
    }

    parsePrometheusMetrics(text) {
        const metrics = {};
        const lines = text.split('\n');

        lines.forEach(line => {
            if (line.startsWith('#') || !line.trim()) return;
            const match = line.match(/^([a-zA-Z_:][a-zA-Z0-9_:]*(?:\{[^}]*\})?) (.+)$/);
            if (match) {
                const name = match[1].split('{')[0];
                const value = parseFloat(match[2]);
                if (!isNaN(value) && value >= 0) {
                    if (!metrics[name] || value > metrics[name]) {
                        metrics[name] = value;
                    }
                }
            }
        });

        return metrics;
    }

    formatDuration(ms) {
        if (!ms || ms === 0) return '0s';
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);

        if (hours > 0) {
            return `${hours}h ${minutes % 60}m`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    formatTimestamp(timestamp) {
        if (!timestamp) return 'Unknown';
        const date = new Date(timestamp);
        return date.toLocaleString('en-IN', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
        });
    }

    toggleFullscreen() {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
        } else {
            document.exitFullscreen();
        }
    }

    startAutoRefresh() {
        // Refresh every 30 seconds
        this.refreshInterval = setInterval(() => {
            this.loadDashboardData();
        }, 30000);
    }

    showToast(type, title, message) {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');

        const colors = {
            success: 'bg-emerald-500/20 border-emerald-500/50 text-emerald-400',
            error: 'bg-red-500/20 border-red-500/50 text-red-400',
            warning: 'bg-amber-500/20 border-amber-500/50 text-amber-400',
            info: 'bg-blue-500/20 border-blue-500/50 text-blue-400'
        };

        const iconPaths = {
            success: '<path d="M9 12l2 2 4-4"></path><circle cx="12" cy="12" r="10"></circle>',
            error: '<circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line>',
            warning: '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line>',
            info: '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line>'
        };

        toast.className = `flex items-center gap-3 p-4 rounded-xl border backdrop-blur-sm shadow-lg ${colors[type] || colors.info} animate-fade-in`;

        toast.innerHTML = `
            <svg class="w-5 h-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                ${iconPaths[type] || iconPaths.info}
            </svg>
            <div class="flex-1 min-w-0">
                <p class="text-sm font-medium">${title}</p>
                <p class="text-xs opacity-80">${message}</p>
            </div>
            <button onclick="this.parentElement.remove()" class="p-1 hover:bg-white/10 rounded transition-colors">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        `;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease-out';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }
}