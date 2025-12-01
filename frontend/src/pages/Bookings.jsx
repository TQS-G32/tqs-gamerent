import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

export default function Bookings() {
  const [bookings, setBookings] = useState([]); // My Rentals
  const [requests, setRequests] = useState([]); // Incoming Requests
  const [listings, setListings] = useState([]); // My Items
  const [activeTab, setActiveTab] = useState('listings'); // 'rentals', 'listings', 'requests'
  
  const userJson = window.localStorage.getItem('user');
  const currentUser = userJson ? JSON.parse(userJson) : null;
  const [isOwner, setIsOwner] = useState(false);

  if (!currentUser) {
    return (
      <div className="container" style={{textAlign: 'center', padding: '80px 20px'}}>
        <h2 style={{marginBottom: '16px'}}>Please Log In</h2>
        <p style={{color: '#666', marginBottom: '24px'}}>You need to be logged in to view your dashboard, manage listings, and check your rentals.</p>
        <Link to="/auth" className="btn btn-primary">Go to Login</Link>
      </div>
    );
  }

  useEffect(() => {
    // Fetch My Rentals (Buying)
    fetch('/api/bookings/my-bookings', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setBookings(data))
      .catch(err => console.error(err));

    // Fetch My Listings
    fetch('/api/items/my-items', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        setListings(data);
        // If the current user has any listings, treat them as an owner for requests
        const ownerFlag = Array.isArray(data) && data.length > 0;
        setIsOwner(ownerFlag || (currentUser && currentUser.role === 'ADMIN'));

        // If owner, fetch incoming booking requests
        if (ownerFlag || (currentUser && currentUser.role === 'ADMIN')) {
          fetch('/api/bookings/requests', { credentials: 'include' })
            .then(res => res.json())
            .then(reqs => setRequests(reqs))
            .catch(err => console.error(err));
        }
      })
      .catch(err => console.error(err));
  }, []);

  const handleStatusUpdate = (id, status) => {
    fetch(`/api/bookings/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status })
    })
    .then(res => res.json())
    .then(updated => {
        setRequests(requests.map(r => r.id === id ? updated : r));
    });
  };

  const renderStatus = (status) => {
      const colors = {
          'PENDING': '#f39c12',
          'APPROVED': '#27ae60',
          'REJECTED': '#c0392b'
      };
      return <span style={{color: colors[status] || '#333', fontWeight: 600, fontSize: '0.85rem', textTransform: 'uppercase'}}>{status}</span>;
  }

  return (
    <div className="container">
      <h2 style={{marginBottom: '24px'}}>My Dashboard</h2>
      
      <div className="tabs">
          <div className={`tab ${activeTab === 'listings' ? 'active' : ''}`} onClick={() => setActiveTab('listings')}>
            My Listings
          </div>
          <div className={`tab ${activeTab === 'rentals' ? 'active' : ''}`} onClick={() => setActiveTab('rentals')}>
            My Rentals (Buying)
          </div>
          <div className={`tab ${activeTab === 'requests' ? 'active' : ''}`} onClick={() => setActiveTab('requests')}>
            Requests ({requests.filter(r => r.status === 'PENDING').length})
          </div>
      </div>

      {/* My Listings Tab */}
      {activeTab === 'listings' && (
        <div>
             {listings.length === 0 ? <div style={{textAlign: 'center', padding: '40px', color: '#999'}}>You haven't listed any items yet.</div> : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                    {listings.map(item => (
                        <div key={item.id} className="sidebar-card" style={{display: 'flex', gap: '16px', alignItems: 'center'}}>
                             <div style={{width: '80px', height: '80px', borderRadius: '4px', overflow: 'hidden', flexShrink: 0}}>
                                <img src={item.imageUrl || "https://via.placeholder.com/150"} alt={item.name} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                             </div>
                             <div style={{flex: 1}}>
                                <div style={{fontWeight: 600, fontSize: '1.1rem'}}>{item.name}</div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>€{item.pricePerDay}/day • {item.category}</div>
                             </div>
                             <div>
                                <Link to={`/item/${item.id}`} className="btn btn-outline" style={{fontSize: '0.85rem', padding: '6px 12px'}}>View</Link>
                             </div>
                        </div>
                    ))}
                </div>
             )}
        </div>
      )}

      {/* My Rentals Tab */}
      {activeTab === 'rentals' && (
        <div>
            {bookings.length === 0 ? <div style={{textAlign: 'center', padding: '40px', color: '#999'}}>No active rentals yet. Go explore!</div> : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                    {bookings.map(b => (
                        <div key={b.id} className="sidebar-card" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                            <div>
                                <div style={{fontWeight: 600, marginBottom: '4px'}}>Item #{b.itemId}</div>
                                <div style={{fontSize: '0.9rem', color: '#666'}}>{b.startDate} — {b.endDate}</div>
                            </div>
                            <div>{renderStatus(b.status)}</div>
                        </div>
                    ))}
                </div>
            )}
        </div>
      )}

      {/* Requests Tab */}
      {activeTab === 'requests' && (
        <div>
             {requests.length === 0 ? <div style={{textAlign: 'center', padding: '40px', color: '#999'}}>No booking requests yet. List more items!</div> : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                    {requests.map(r => (
                        <div key={r.id} className="sidebar-card" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                             <div>
                                <div style={{fontWeight: 600, marginBottom: '4px'}}>Item #{r.itemId}</div>
                                <div style={{fontSize: '0.9rem', color: '#666'}}>User #{r.userId} • {r.startDate} — {r.endDate}</div>
                            </div>
                            
                            {r.status === 'PENDING' ? (
                                <div style={{display: 'flex', gap: '8px'}}>
                                    <button className="btn btn-primary" style={{padding: '6px 12px', fontSize: '0.85rem'}} onClick={() => handleStatusUpdate(r.id, 'APPROVED')}>Accept</button>
                                    <button className="btn btn-danger" style={{padding: '6px 12px', fontSize: '0.85rem'}} onClick={() => handleStatusUpdate(r.id, 'REJECTED')}>Decline</button>
                                </div>
                            ) : (
                                <div>{renderStatus(r.status)}</div>
                            )}
                        </div>
                    ))}
                </div>
             )}
        </div>
      )}
    </div>
  );
}
