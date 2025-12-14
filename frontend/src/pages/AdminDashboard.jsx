import { useEffect, useState } from "react";

export default function AdminDashboard() {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchMetrics = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/admin/metrics", { credentials: "include" });
      if (!res.ok) throw new Error("Access denied or API error");
      const data = await res.json();
      setMetrics(data);
      setLastUpdated(new Date());
    } catch (e) {
      setError(e.message);
      setMetrics(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMetrics();
    const interval = setInterval(fetchMetrics, 10000); // 10s auto-refresh
    return () => clearInterval(interval);
  }, []);

  if (loading) return (
    <div className="container" style={{padding: '2rem'}}>
      <div className="sidebar-card" style={{textAlign: 'center'}}>Loading metrics...</div>
    </div>
  );

  if (error) return (
    <div className="container" style={{padding: '2rem'}}>
      <div className="sidebar-card" style={{color: '#e74c3c', textAlign: 'center'}}>
        <strong>Error:</strong> {error}
      </div>
    </div>
  );

  if (!metrics) return (
    <div className="container" style={{padding: '2rem'}}>
      <div className="sidebar-card" style={{textAlign: 'center'}}>No metrics available.</div>
    </div>
  );

  const metricCards = [
    { label: "Total Accounts", value: metrics.totalAccounts, icon: "👥" },
    { label: "Active Listings", value: metrics.activeListings, icon: "🎮" },
    { label: "Total Bookings", value: metrics.totalBookings, icon: "📦" },
    { label: "Monthly Revenue", value: `$${metrics.monthlyRevenue}`, icon: "💰" },
    { label: "Open Issues", value: metrics.openIssues, icon: "⚠️" },
  ];

  return (
    <div className="container" style={{padding: '2rem 1rem'}}>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem'}}>
        <h1 style={{fontSize: '2rem', fontWeight: 700, margin: 0}}>Admin Dashboard</h1>
        <button className="btn btn-outline" onClick={fetchMetrics} style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
          <span>🔄</span> Refresh
        </button>
      </div>
      
      {lastUpdated && (
        <div style={{color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.5rem'}}>
          Last updated: {lastUpdated.toLocaleTimeString()}
        </div>
      )}

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '20px'
      }}>
        {metricCards.map((metric) => (
          <div key={metric.label} className="sidebar-card" style={{
            textAlign: 'center',
            padding: '2rem 1.5rem',
            transition: 'transform 0.2s, box-shadow 0.2s',
            cursor: 'default'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = 'translateY(-4px)';
            e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = 'translateY(0)';
            e.currentTarget.style.boxShadow = 'none';
          }}>
            <div style={{fontSize: '3rem', marginBottom: '0.5rem'}}>{metric.icon}</div>
            <div style={{fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '0.5rem', fontWeight: 500}}>
              {metric.label}
            </div>
            <div style={{fontSize: '2rem', fontWeight: 700, color: 'var(--text-main)'}}>
              {metric.value}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
