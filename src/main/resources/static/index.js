// Enterprise Dashboard JavaScript
// Real-time monitoring and analytics

// Configuration
const CONFIG = {
    API_BASE_URL: window.location.origin,
    REFRESH_INTERVAL: 30000, // 30 seconds
    CHART_ANIMATION_DURATION: 750
};

// Global state
let charts = {};
let refreshInterval = null;
let allBuilds = [];

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Initializing Enterprise Dashboard...');
    initializeCharts();
    loadDashboardData();
    setupEventListeners();
    startAutoRefresh();
});

// Setup event listeners
function setupEventListeners() {
    document.getElementById('refreshBtn').addEventListener('click', () => {
        loadDashboardData();
        showNotification('Dashboard refreshed', 'success');
    });

    document.getElementById('timeRange').addEventListener('change', (e) => {
        loadDashboardData();
    });

    document.getElementById('searchBuilds').addEventListener('input', (e) => {
        filterBuilds(e.target.value);
    });

    document.getElementById('exportBtn').addEventListener('click', () => {
        exportBuildsToCSV();
    });
}

// Load all dashboard data
async function loadDashboardData() {
    try {
        console.log('📊 Loading dashboard data...');
        updateLastUpdateTime();

        // Load in parallel
        await Promise.all([
            loadKPIMetrics(),
            loadSystemMetrics(),
            loadBuildStatistics(),
            loadRecentBuilds()
        ]);

        console.log('✅ Dashboard data loaded successfully');
    } catch (error) {
        console.error('❌ Error loading dashboard:', error);
        showNotification('Failed to load dashboard data', 'error');
    }
}

// Load KPI metrics
async function loadKPIMetrics() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/metrics/realtime`);
        const metrics = await response.json();

        // Update CPU
        const cpuPercent = (metrics.cpuUsage * 100).toFixed(1);
        document.getElementById('cpuValue').textContent = `${cpuPercent}%`;
        document.getElementById('cpuBar').style.width = `${cpuPercent}%`;
        updateTrendIndicator('cpuTrend', cpuPercent, 70);

        // Update Memory
        const memoryPercent = (metrics.memoryUsage * 100).toFixed(1);
        document.getElementById('memoryValue').textContent = `${memoryPercent}%`;
        document.getElementById('memoryBar').style.width = `${memoryPercent}%`;
        updateTrendIndicator('memoryTrend', memoryPercent, 80);

        // Update Thread Count
        document.getElementById('threadCount').textContent = metrics.threadCount || 0;

    } catch (error) {
        console.error('Error loading KPI metrics:', error);
    }
}

// Load system metrics for charts
async function loadSystemMetrics() {
    try {
        const hours = document.getElementById('timeRange').value;
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/metrics/history?hours=${hours}`);
        const metricsHistory = await response.json();

        if (metricsHistory && metricsHistory.length > 0) {
            updateSystemChart(metricsHistory);
            updateHttpChart(metricsHistory);
        }
    } catch (error) {
        console.error('Error loading system metrics:', error);
    }
}

// Load build statistics
async function loadBuildStatistics() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/analytics/builds/statistics`);
        const stats = await response.json();

        // Update Total Builds
        document.getElementById('totalBuilds').textContent = stats.totalBuilds || 0;

        // Update Success Rate
        const successRate = stats.successRate || 0;
        document.getElementById('successRate').textContent = `${successRate.toFixed(1)}%`;
        document.getElementById('successBar').style.width = `${successRate}%`;

        // Update Status Chart
        if (stats.statusBreakdown) {
            updateStatusChart(stats.statusBreakdown);
        }

    } catch (error) {
        console.error('Error loading build statistics:', error);
    }
}

// Load recent builds
async function loadRecentBuilds() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/api/analytics/builds/recent?limit=20`);
        const builds = await response.json();

        allBuilds = builds;
        renderBuildsTable(builds);
        updateBuildTrendsChart(builds);

    } catch (error) {
        console.error('Error loading recent builds:', error);
    }
}

// Initialize all charts
function initializeCharts() {
    // System Resources Chart
    const systemCtx = document.getElementById('systemChart').getContext('2d');
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
                    fill: true
                },
                {
                    label: 'Memory Usage (%)',
                    data: [],
                    borderColor: 'rgb(168, 85, 247)',
                    backgroundColor: 'rgba(168, 85, 247, 0.1)',
                    tension: 0.4,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        callback: function (value) {
                            return value + '%';
                        }
                    }
                }
            }
        }
    });

    // HTTP Requests Chart
    const httpCtx = document.getElementById('httpChart').getContext('2d');
    charts.http = new Chart(httpCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Total Requests',
                data: [],
                borderColor: 'rgb(16, 185, 129)',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                tension: 0.4,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });

    // Build Trends Chart
    const trendsCtx = document.getElementById('buildTrendsChart').getContext('2d');
    charts.buildTrends = new Chart(trendsCtx, {
        type: 'bar',
        data: {
            labels: [],
            datasets: [{
                label: 'Builds',
                data: [],
                backgroundColor: 'rgba(59, 130, 246, 0.8)',
                borderColor: 'rgb(59, 130, 246)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1
                    }
                }
            }
        }
    });

    // Status Distribution Chart
    const statusCtx = document.getElementById('statusChart').getContext('2d');
    charts.status = new Chart(statusCtx, {
        type: 'doughnut',
        data: {
            labels: [],
            datasets: [{
                data: [],
                backgroundColor: [
                    'rgb(16, 185, 129)',
                    'rgb(239, 68, 68)',
                    'rgb(245, 158, 11)'
                ],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                }
            }
        }
    });
}

// Update System Chart
function updateSystemChart(metricsHistory) {
    const labels = metricsHistory.map(m => formatTime(m.recordedAt)).reverse();
    const cpuData = metricsHistory.map(m => (m.cpuUsage * 100).toFixed(2)).reverse();
    const memoryData = metricsHistory.map(m => (m.memoryUsage * 100).toFixed(2)).reverse();

    charts.system.data.labels = labels;
    charts.system.data.datasets[0].data = cpuData;
    charts.system.data.datasets[1].data = memoryData;
    charts.system.update('none');
}

// Update HTTP Chart
function updateHttpChart(metricsHistory) {
    const labels = metricsHistory.map(m => formatTime(m.recordedAt)).reverse();
    const httpData = metricsHistory.map(m => m.httpRequestsTotal || 0).reverse();

    charts.http.data.labels = labels;
    charts.http.data.datasets[0].data = httpData;
    charts.http.update('none');
}

// Update Build Trends Chart
function updateBuildTrendsChart(builds) {
    // Group builds by date
    const buildsByDate = {};
    builds.forEach(build => {
        const date = new Date(build.timestamp).toLocaleDateString();
        buildsByDate[date] = (buildsByDate[date] || 0) + 1;
    });

    const labels = Object.keys(buildsByDate).slice(-10);
    const data = labels.map(date => buildsByDate[date]);

    charts.buildTrends.data.labels = labels;
    charts.buildTrends.data.datasets[0].data = data;
    charts.buildTrends.update('none');
}

// Update Status Chart
function updateStatusChart(statusBreakdown) {
    const labels = Object.keys(statusBreakdown);
    const data = Object.values(statusBreakdown);

    charts.status.data.labels = labels;
    charts.status.data.datasets[0].data = data;
    charts.status.update('none');
}

// Render builds table
function renderBuildsTable(builds) {
    const tbody = document.getElementById('buildsTableBody');
    tbody.innerHTML = '';

    if (builds.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center text-gray-500">No builds found</td></tr>';
        return;
    }

    builds.forEach(build => {
        const row = document.createElement('tr');
        row.className = 'hover:bg-gray-50 transition';

        const statusClass = getStatusClass(build.status);
        const statusBadge = `<span class="px-3 py-1 rounded-full text-xs font-semibold ${statusClass}">${build.status}</span>`;

        row.innerHTML = `
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${build.jobName}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">#${build.buildNumber}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm">${statusBadge}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatDuration(build.durationMs)}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatDateTime(build.timestamp)}</td>
        `;

        tbody.appendChild(row);
    });
}

// Filter builds
function filterBuilds(searchTerm) {
    const filtered = allBuilds.filter(build =>
        build.jobName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        build.buildNumber.toString().includes(searchTerm) ||
        build.status.toLowerCase().includes(searchTerm.toLowerCase())
    );
    renderBuildsTable(filtered);
}

// Export builds to CSV
function exportBuildsToCSV() {
    const headers = ['Job Name', 'Build Number', 'Status', 'Duration (ms)', 'Timestamp'];
    const rows = allBuilds.map(build => [
        build.jobName,
        build.buildNumber,
        build.status,
        build.durationMs,
        build.timestamp
    ]);

    let csv = headers.join(',') + '\n';
    rows.forEach(row => {
        csv += row.join(',') + '\n';
    });

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `builds_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();

    showNotification('Builds exported successfully', 'success');
}

// Utility functions
function getStatusClass(status) {
    const statusMap = {
        'SUCCESS': 'bg-green-100 text-green-800',
        'FAILURE': 'bg-red-100 text-red-800',
        'UNSTABLE': 'bg-yellow-100 text-yellow-800',
        'ABORTED': 'bg-gray-100 text-gray-800'
    };
    return statusMap[status] || 'bg-gray-100 text-gray-800';
}

function formatDuration(ms) {
    if (!ms) return '--';
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes > 0 ? `${minutes}m ${remainingSeconds}s` : `${seconds}s`;
}

function formatDateTime(timestamp) {
    if (!timestamp) return '--';
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatTime(timestamp) {
    if (!timestamp) return '--';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function updateTrendIndicator(elementId, value, threshold) {
    const element = document.getElementById(elementId);
    if (value > threshold) {
        element.innerHTML = '<span class="text-red-500">↑</span>';
    } else {
        element.innerHTML = '<span class="text-green-500">↓</span>';
    }
}

function updateLastUpdateTime() {
    const now = new Date();
    document.getElementById('lastUpdate').textContent = now.toLocaleTimeString();
}

function showNotification(message, type = 'info') {
    // Simple notification - can be enhanced with a library
    console.log(`[${type.toUpperCase()}] ${message}`);
}

// Auto-refresh
function startAutoRefresh() {
    refreshInterval = setInterval(() => {
        console.log('🔄 Auto-refreshing dashboard...');
        loadDashboardData();
    }, CONFIG.REFRESH_INTERVAL);
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    stopAutoRefresh();
});