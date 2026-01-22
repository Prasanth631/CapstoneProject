// Enterprise Dashboard JavaScript
// Real-time monitoring with dark mode and animations

// ==================== CONFIGURATION ====================
const CONFIG = {
    API_BASE_URL: window.location.origin,
    REFRESH_INTERVAL: 30000,
    JENKINS_URL: '/api/jenkins'
};

// ==================== GLOBAL STATE ====================
let charts = {};
let refreshInterval = null;
let allBuilds = [];
let isDarkMode = false;

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Enterprise Dashboard Initializing...');

    // Initialize dark mode from localStorage
    initDarkMode();

    // Initialize charts
    initializeCharts();

    // Load initial data
    loadAllData();

    // Setup event listeners
    setupEventListeners();

    // Start auto-refresh
    startAutoRefresh();

    console.log('✅ Dashboard Ready!');
});

// ==================== DARK MODE ====================
function initDarkMode() {
    isDarkMode = localStorage.getItem('darkMode') === 'true';
    applyDarkMode();
}

function toggleDarkMode() {
    isDarkMode = !isDarkMode;
    localStorage.setItem('darkMode', isDarkMode);
    applyDarkMode();

    // Re-initialize charts with new colors
    setTimeout(() => {
        initializeCharts();
        loadSystemMetrics();
        loadBuildStatistics();
    }, 100);
}

function applyDarkMode() {
    const html = document.documentElement;
    const darkIcon = document.getElementById('darkIcon');
    const lightIcon = document.getElementById('lightIcon');

    if (isDarkMode) {
        html.classList.add('dark');
        darkIcon?.classList.add('hidden');
        lightIcon?.classList.remove('hidden');
    } else {
        html.classList.remove('dark');
        darkIcon?.classList.remove('hidden');
        lightIcon?.classList.add('hidden');
    }
}

// ==================== EVENT LISTENERS ====================
function setupEventListeners() {
    // Dark mode toggle
    document.getElementById('darkModeToggle')?.addEventListener('click', toggleDarkMode);

    // Refresh button
    document.getElementById('refreshBtn')?.addEventListener('click', () => {
        const icon = document.getElementById('refreshIcon');
        icon?.classList.add('animate-spin');
        loadAllData().then(() => {
            setTimeout(() => icon?.classList.remove('animate-spin'), 500);
            showToast('Dashboard refreshed!', 'success');
        });
    });

    // Time range selector
    document.getElementById('timeRange')?.addEventListener('change', loadAllData);

    // Search builds
    document.getElementById('searchBuilds')?.addEventListener('input', (e) => {
        filterBuilds(e.target.value);
    });

    // Export button
    document.getElementById('exportBtn')?.addEventListener('click', exportBuildsToCSV);
}

// ==================== DATA LOADING ====================
async function loadAllData() {
    updateLastUpdateTime();

    try {
        await Promise.all([
            loadKPIMetrics(),
            loadSystemMetrics(),
            loadBuildStatistics(),
            loadJenkinsBuilds()
        ]);
    } catch (error) {
        console.error('Error loading data:', error);
    }
}

// Load KPI Metrics (CPU, Memory, Threads)
async function loadKPIMetrics() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/metrics/realtime`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const metrics = await response.json();
        console.log('📊 Metrics received:', metrics);

        // CPU Usage
        const cpuPercent = Math.min((metrics.cpuUsage || 0) * 100, 100);
        animateNumber('cpuValue', cpuPercent.toFixed(1) + '%');
        document.getElementById('cpuBar').style.width = `${cpuPercent}%`;
        setTrendIndicator('cpuTrend', cpuPercent, 70);

        // Memory Usage
        const memoryPercent = Math.min((metrics.memoryUsage || 0) * 100, 100);
        animateNumber('memoryValue', memoryPercent.toFixed(1) + '%');
        document.getElementById('memoryBar').style.width = `${memoryPercent}%`;
        setTrendIndicator('memoryTrend', memoryPercent, 80);

        // Thread Count
        animateNumber('threadCount', metrics.threadCount || 0);

    } catch (error) {
        console.error('❌ Error loading KPI metrics:', error);
        showToast('Failed to load metrics', 'error');
    }
}

// Load Build Statistics
async function loadBuildStatistics() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/analytics/builds/statistics`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const stats = await response.json();
        console.log('📈 Build stats received:', stats);

        // Total Builds
        animateNumber('totalBuilds', stats.totalBuilds || 0);

        // Success Rate
        const successRate = stats.successRate || 0;
        animateNumber('successRate', successRate.toFixed(1) + '%');
        document.getElementById('successBar').style.width = `${successRate}%`;

        // Update status chart
        if (stats.statusBreakdown) {
            updateStatusChart(stats.statusBreakdown);
        }

    } catch (error) {
        console.error('❌ Error loading build statistics:', error);
    }
}

// Load System Metrics History for Charts
async function loadSystemMetrics() {
    try {
        const hours = document.getElementById('timeRange')?.value || 24;
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/metrics/history?hours=${hours}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const history = await response.json();
        console.log('📉 Metrics history received:', history.length, 'records');

        if (history.length > 0) {
            updateSystemChart(history);
        }

    } catch (error) {
        console.error('❌ Error loading system metrics:', error);
    }
}

// Load Jenkins Builds - Fetch directly from Jenkins API through backend proxy
async function loadJenkinsBuilds() {
    try {
        // Try to load from analytics first
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/analytics/builds/recent?limit=20`);

        if (response.ok) {
            const builds = await response.json();
            console.log('🔨 Builds received:', builds.length);
            allBuilds = builds;
            renderBuildsTable(builds);
        } else {
            // Fallback: try Jenkins proxy
            await loadBuildsFromJenkinsProxy();
        }

    } catch (error) {
        console.error('❌ Error loading builds:', error);
        await loadBuildsFromJenkinsProxy();
    }
}

// Load builds directly from Jenkins via proxy
async function loadBuildsFromJenkinsProxy() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/jenkins/jobs`);
        if (!response.ok) return;

        const jobs = await response.json();
        if (jobs && jobs.jobs) {
            const builds = [];
            for (const job of jobs.jobs.slice(0, 5)) {
                const lastBuild = await fetch(`${CONFIG.API_BASE_URL}/api/jenkins/job/${job.name}/lastBuild`);
                if (lastBuild.ok) {
                    const buildData = await lastBuild.json();
                    builds.push({
                        jobName: job.name,
                        buildNumber: buildData.number,
                        status: buildData.result || 'BUILDING',
                        durationMs: buildData.duration,
                        timestamp: new Date(buildData.timestamp).toISOString()
                    });
                }
            }
            allBuilds = builds;
            renderBuildsTable(builds);
        }
    } catch (error) {
        console.error('❌ Error loading from Jenkins proxy:', error);
        renderBuildsTable([]);
    }
}

// ==================== CHART INITIALIZATION ====================
function initializeCharts() {
    const textColor = isDarkMode ? '#e2e8f0' : '#374151';
    const gridColor = isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)';

    // Destroy existing charts
    Object.values(charts).forEach(chart => chart?.destroy());

    // System Resources Chart
    const systemCtx = document.getElementById('systemChart')?.getContext('2d');
    if (systemCtx) {
        charts.system = new Chart(systemCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    {
                        label: 'CPU Usage (%)',
                        data: [],
                        borderColor: 'rgb(59, 130, 246)',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        tension: 0.4,
                        fill: true,
                        pointRadius: 2
                    },
                    {
                        label: 'Memory Usage (%)',
                        data: [],
                        borderColor: 'rgb(168, 85, 247)',
                        backgroundColor: 'rgba(168, 85, 247, 0.1)',
                        tension: 0.4,
                        fill: true,
                        pointRadius: 2
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: { color: textColor }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        grid: { color: gridColor },
                        ticks: { color: textColor, callback: v => v + '%' }
                    },
                    x: {
                        grid: { color: gridColor },
                        ticks: { color: textColor }
                    }
                }
            }
        });
    }

    // Status Distribution Chart
    const statusCtx = document.getElementById('statusChart')?.getContext('2d');
    if (statusCtx) {
        charts.status = new Chart(statusCtx, {
            type: 'doughnut',
            data: {
                labels: ['Success', 'Failure', 'Unstable'],
                datasets: [{
                    data: [0, 0, 0],
                    backgroundColor: [
                        'rgb(16, 185, 129)',
                        'rgb(239, 68, 68)',
                        'rgb(245, 158, 11)'
                    ],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: { color: textColor }
                    }
                }
            }
        });
    }
}

// ==================== CHART UPDATES ====================
function updateSystemChart(history) {
    if (!charts.system || !history.length) return;

    const sortedHistory = [...history].sort((a, b) =>
        new Date(a.recordedAt) - new Date(b.recordedAt)
    );

    const labels = sortedHistory.map(m => formatTime(m.recordedAt));
    const cpuData = sortedHistory.map(m => Math.min((m.cpuUsage || 0) * 100, 100).toFixed(1));
    const memoryData = sortedHistory.map(m => Math.min((m.memoryUsage || 0) * 100, 100).toFixed(1));

    charts.system.data.labels = labels;
    charts.system.data.datasets[0].data = cpuData;
    charts.system.data.datasets[1].data = memoryData;
    charts.system.update('none');
}

function updateStatusChart(breakdown) {
    if (!charts.status) return;

    const labels = [];
    const data = [];
    const colors = {
        'SUCCESS': 'rgb(16, 185, 129)',
        'FAILURE': 'rgb(239, 68, 68)',
        'UNSTABLE': 'rgb(245, 158, 11)',
        'ABORTED': 'rgb(107, 114, 128)'
    };
    const chartColors = [];

    for (const [status, count] of Object.entries(breakdown)) {
        labels.push(status);
        data.push(count);
        chartColors.push(colors[status] || 'rgb(107, 114, 128)');
    }

    charts.status.data.labels = labels;
    charts.status.data.datasets[0].data = data;
    charts.status.data.datasets[0].backgroundColor = chartColors;
    charts.status.update('none');
}

// ==================== BUILDS TABLE ====================
function renderBuildsTable(builds) {
    const tbody = document.getElementById('buildsTableBody');
    if (!tbody) return;

    if (!builds || builds.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-12 text-center">
                    <div class="flex flex-col items-center text-gray-500 dark:text-gray-400">
                        <i data-lucide="inbox" class="w-12 h-12 mb-4 opacity-50"></i>
                        <p class="text-lg font-medium">No builds found</p>
                        <p class="text-sm">Builds will appear here when Jenkins runs</p>
                    </div>
                </td>
            </tr>
        `;
        lucide.createIcons();
        return;
    }

    tbody.innerHTML = builds.map((build, index) => `
        <tr class="hover:bg-gray-50 dark:hover:bg-slate-800/50 transition-colors animate-fade-in" style="animation-delay: ${index * 0.05}s">
            <td class="px-6 py-4">
                <div class="flex items-center space-x-3">
                    <div class="p-2 bg-indigo-100 dark:bg-indigo-900/30 rounded-lg">
                        <i data-lucide="git-branch" class="w-4 h-4 text-indigo-600 dark:text-indigo-400"></i>
                    </div>
                    <span class="font-medium text-gray-900 dark:text-white">${escapeHtml(build.jobName)}</span>
                </div>
            </td>
            <td class="px-6 py-4">
                <span class="px-3 py-1 bg-gray-100 dark:bg-gray-800 rounded-full text-sm font-mono text-gray-700 dark:text-gray-300">#${build.buildNumber}</span>
            </td>
            <td class="px-6 py-4">${getStatusBadge(build.status)}</td>
            <td class="px-6 py-4 text-gray-600 dark:text-gray-400">${formatDuration(build.durationMs)}</td>
            <td class="px-6 py-4 text-gray-600 dark:text-gray-400">${formatDateTime(build.timestamp)}</td>
        </tr>
    `).join('');

    lucide.createIcons();
}

function filterBuilds(searchTerm) {
    const filtered = allBuilds.filter(build =>
        build.jobName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        build.buildNumber.toString().includes(searchTerm) ||
        build.status.toLowerCase().includes(searchTerm.toLowerCase())
    );
    renderBuildsTable(filtered);
}

// ==================== UTILITY FUNCTIONS ====================
function getStatusBadge(status) {
    const badges = {
        'SUCCESS': '<span class="px-3 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">✓ SUCCESS</span>',
        'FAILURE': '<span class="px-3 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400">✗ FAILURE</span>',
        'UNSTABLE': '<span class="px-3 py-1 text-xs font-semibold rounded-full bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">⚠ UNSTABLE</span>',
        'ABORTED': '<span class="px-3 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400">⊘ ABORTED</span>',
        'BUILDING': '<span class="px-3 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400 animate-pulse">⟳ BUILDING</span>'
    };
    return badges[status] || badges['ABORTED'];
}

function formatDuration(ms) {
    if (!ms || ms === 0) return '--';
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) return `${hours}h ${minutes % 60}m`;
    if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
    return `${seconds}s`;
}

function formatDateTime(timestamp) {
    if (!timestamp) return '--';
    try {
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', {
            month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    } catch { return '--'; }
}

function formatTime(timestamp) {
    if (!timestamp) return '--';
    try {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } catch { return '--'; }
}

function animateNumber(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) {
        element.style.transform = 'scale(1.1)';
        element.textContent = value;
        setTimeout(() => element.style.transform = 'scale(1)', 200);
    }
}

function setTrendIndicator(elementId, value, threshold) {
    const element = document.getElementById(elementId);
    if (!element) return;

    if (value > threshold) {
        element.innerHTML = '↑ High';
        element.className = 'text-sm font-semibold px-2 py-1 rounded-full bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400';
    } else if (value > threshold * 0.7) {
        element.innerHTML = '→ Normal';
        element.className = 'text-sm font-semibold px-2 py-1 rounded-full bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400';
    } else {
        element.innerHTML = '↓ Low';
        element.className = 'text-sm font-semibold px-2 py-1 rounded-full bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400';
    }
}

function updateLastUpdateTime() {
    const element = document.getElementById('lastUpdate');
    if (element) {
        element.textContent = new Date().toLocaleTimeString();
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const colors = {
        success: 'bg-green-500',
        error: 'bg-red-500',
        warning: 'bg-yellow-500',
        info: 'bg-blue-500'
    };

    const toast = document.createElement('div');
    toast.className = `${colors[type]} text-white px-6 py-3 rounded-xl shadow-lg transform translate-x-full animate-fade-in`;
    toast.textContent = message;

    container.appendChild(toast);
    setTimeout(() => toast.classList.remove('translate-x-full'), 10);

    setTimeout(() => {
        toast.classList.add('opacity-0');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function exportBuildsToCSV() {
    if (!allBuilds.length) {
        showToast('No builds to export', 'warning');
        return;
    }

    const headers = ['Job Name', 'Build Number', 'Status', 'Duration (ms)', 'Timestamp'];
    const rows = allBuilds.map(b => [b.jobName, b.buildNumber, b.status, b.durationMs || '', b.timestamp]);

    let csv = headers.join(',') + '\n';
    rows.forEach(row => csv += row.map(v => `"${v}"`).join(',') + '\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `builds_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);

    showToast('Builds exported!', 'success');
}

// ==================== AUTO REFRESH ====================
function startAutoRefresh() {
    refreshInterval = setInterval(() => {
        console.log('🔄 Auto-refreshing...');
        loadAllData();
    }, CONFIG.REFRESH_INTERVAL);
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// Cleanup
window.addEventListener('beforeunload', stopAutoRefresh);