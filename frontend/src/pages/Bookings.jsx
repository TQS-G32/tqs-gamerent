import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

export default function Bookings() {
  const [bookings, setBookings] = useState([]); // My Rentals
  const [requests, setRequests] = useState([]); // Incoming Requests
  const [listings, setListings] = useState([]); // My Items
  const [itemsMap, setItemsMap] = useState({}); // Map of itemId -> Item data
  const [activeTab, setActiveTab] = useState('listings'); // 'rentals', 'listings', 'requests'
  const [editingItemId, setEditingItemId] = useState(null); // For inline editing
  const [editForm, setEditForm] = useState({ available: true, minRentalDays: 1 });
  
  const userJson = window.localStorage.getItem('user');
  const currentUser = userJson ? JSON.parse(userJson) : null;
  const [isOwner, setIsOwner] = useState(false);

  if (!currentUser) {
    return (
      <div className="container" style={{textAlign: 'center', padding: '80px 20px'}}>
        <h2 style={{marginBottom: '16px'}}>🔐 Please Log In</h2>
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

  // Fetch item details for all bookings/requests to display with booking info
  useEffect(() => {
    const itemIds = new Set();
    bookings.forEach(b => itemIds.add(b.itemId));
    requests.forEach(r => itemIds.add(r.itemId));

    itemIds.forEach(itemId => {
      if (!itemsMap[itemId]) {
        fetch(`/api/items/${itemId}`, { credentials: 'include' })
          .then(res => res.json())
          .then(item => {
            setItemsMap(prev => ({ ...prev, [itemId]: item }));
          })
          .catch(err => console.error(err));
      }
    });
  }, [bookings, requests]);

  const handleStatusUpdate = (id, status) => {
    fetch(`/api/bookings/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
      credentials: 'include'
    })
    .then(res => res.json())
    .then(updated => {
        setRequests(requests.map(r => r.id === id ? updated : r));
    });
  };

  const handleEditItem = (item) => {
    setEditingItemId(item.id);
    setEditForm({ 
      available: item.available !== null ? item.available : false, 
      minRentalDays: item.minRentalDays || 1 
    });
  };

  const handleCancelEdit = () => {
    setEditingItemId(null);
    setEditForm({ available: true, minRentalDays: 1 });
  };

  const handleSaveSettings = (itemId) => {
    fetch(`/api/items/${itemId}/settings`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editForm),
      credentials: 'include'
    })
    .then(res => {
      if (!res.ok) throw new Error('Failed to update settings');
      return res.json();
    })
    .then(data => {
      // Update the item in the listings array
      setListings(listings.map(item => 
        item.id === itemId ? data.item : item
      ));
      setEditingItemId(null);
      alert(data.message || 'Settings updated successfully');
    })
    .catch(err => {
      alert(err.message || 'Error updating settings');
    });
  };

  const renderStatus = (status) => {
      const colors = {
          'PENDING': '#f39c12',
          'APPROVED': '#27ae60',
          'REJECTED': '#c0392b'
      };
      const icons = {
        'PENDING': '⏳',
        'APPROVED': '✓',
        'REJECTED': '✕'
      };
      return <span style={{color: colors[status] || '#333', fontWeight: 600, fontSize: '0.85rem', textTransform: 'uppercase'}}>{icons[status]} {status}</span>;
  }

  const getOwnerInitial = (item) => {
    return item && item.owner ? item.owner.name.charAt(0).toUpperCase() : 'U';
  };

  return (
    <div className="container">
      <h2 style={{marginBottom: '24px'}}>📊 My Dashboard</h2>
      
      <div className="tabs">
          <div className={`tab ${activeTab === 'listings' ? 'active' : ''}`} onClick={() => setActiveTab('listings')}>
            📦 My Listings ({listings.length})
          </div>
          <div className={`tab ${activeTab === 'rentals' ? 'active' : ''}`} onClick={() => setActiveTab('rentals')}>
            🛒 My Rentals ({bookings.length})
          </div>
          {isOwner && (
            <div className={`tab ${activeTab === 'requests' ? 'active' : ''}`} onClick={() => setActiveTab('requests')}>
              📬 Requests ({requests.filter(r => r.status === 'PENDING').length})
            </div>
          )}
      </div>

      {/* My Listings Tab */}
      {activeTab === 'listings' && (
        <div>
             {listings.length === 0 ? (
               <div style={{textAlign: 'center', padding: '60px 20px', color: '#999'}}>
                 <div style={{fontSize: '3rem', marginBottom: '12px'}}>📭</div>
                 <h3>No listings yet</h3>
                 <p>Start renting out your gaming gear!</p>
                 <Link to="/post-item" className="btn btn-primary" style={{marginTop: '16px'}}>Create your first listing</Link>
               </div>
             ) : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                    {listings.map(item => (
                        <div key={item.id} className="sidebar-card" style={{display: 'flex', gap: '16px', alignItems: 'flex-start', padding: '16px'}}>
                             <div style={{width: '80px', height: '80px', borderRadius: '4px', overflow: 'hidden', flexShrink: 0, background: '#eee'}}>
                                <img src={item.imageUrl || "https://via.placeholder.com/150"} alt={item.name} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                             </div>
                             <div style={{flex: 1}}>
                                <div style={{fontWeight: 600, fontSize: '1.1rem', marginBottom: '8px'}}>{item.name}</div>
                                
                                {editingItemId === item.id ? (
                                  // Edit Mode
                                  <div style={{background: '#f8f9fa', padding: '12px', borderRadius: '8px', marginBottom: '8px'}}>
                                    <div style={{marginBottom: '12px'}}>
                                      <label style={{display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.95rem'}}>
                                        <input 
                                          type="checkbox" 
                                          checked={editForm.available}
                                          onChange={(e) => setEditForm({...editForm, available: e.target.checked})}
                                          style={{width: '18px', height: '18px', cursor: 'pointer'}}
                                        />
                                        <span style={{fontWeight: 500}}>Available for rent</span>
                                      </label>
                                    </div>
                                    <div style={{marginBottom: '12px'}}>
                                      <label style={{display: 'block', fontSize: '0.9rem', marginBottom: '4px', fontWeight: 500}}>
                                        Minimum rental period (days)
                                      </label>
                                      <input 
                                        type="number" 
                                        min="1" 
                                        max="30"
                                        value={editForm.minRentalDays}
                                        onChange={(e) => setEditForm({...editForm, minRentalDays: parseInt(e.target.value) || 1})}
                                        style={{width: '100px', padding: '6px 10px', borderRadius: '4px', border: '1px solid #ddd', fontSize: '0.9rem'}}
                                      />
                                      <span style={{marginLeft: '8px', fontSize: '0.85rem', color: '#666'}}>
                                        (1-30 days)
                                      </span>
                                    </div>
                                    <div style={{display: 'flex', gap: '8px'}}>
                                      <button 
                                        className="btn btn-primary" 
                                        onClick={() => handleSaveSettings(item.id)}
                                        style={{fontSize: '0.85rem', padding: '6px 14px'}}
                                      >
                                        ✓ Save Changes
                                      </button>
                                      <button 
                                        className="btn btn-outline" 
                                        onClick={handleCancelEdit}
                                        style={{fontSize: '0.85rem', padding: '6px 14px'}}
                                      >
                                        Cancel
                                      </button>
                                    </div>
                                  </div>
                                ) : (
                                  // View Mode
                                  <>
                                    <div style={{color: '#666', fontSize: '0.9rem', marginBottom: '6px'}}>
                                      €{item.pricePerDay ? item.pricePerDay.toFixed(2) : '0.00'}/day • {item.category}
                                    </div>
                                    <div style={{display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px'}}>
                                      <span style={{
                                        fontSize: '0.85rem', 
                                        padding: '3px 10px', 
                                        borderRadius: '12px',
                                        background: item.available ? '#d4edda' : '#f8d7da',
                                        color: item.available ? '#155724' : '#721c24',
                                        fontWeight: 600
                                      }}>
                                        {item.available ? '✓ Active' : '✕ Inactive'}
                                      </span>
                                      <span style={{fontSize: '0.85rem', color: '#666'}}>
                                        Min. {item.minRentalDays || 1} day{(item.minRentalDays || 1) > 1 ? 's' : ''}
                                      </span>
                                    </div>
                                  </>
                                )}
                             </div>
                             <div style={{display: 'flex', flexDirection: 'column', gap: '8px', minWidth: '120px'}}>
                                {editingItemId !== item.id && (
                                  <Link to={`/item/${item.id}`} className="btn btn-outline" style={{fontSize: '0.85rem', padding: '6px 12px', textAlign: 'center'}}>
                                    👁️ View
                                  </Link>
                                )}
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
            {bookings.length === 0 ? (
              <div style={{textAlign: 'center', padding: '60px 20px', color: '#999'}}>
                <div style={{fontSize: '3rem', marginBottom: '12px'}}>🎮</div>
                <h3>No rentals yet</h3>
                <p>Explore and book gaming gear from other users</p>
                <Link to="/" className="btn btn-primary" style={{marginTop: '16px'}}>Browse items</Link>
              </div>
            ) : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                    {bookings.map(booking => {
                      const item = itemsMap[booking.itemId];
                      return (
                        <div key={booking.id} className="sidebar-card" style={{display: 'flex', gap: '16px', alignItems: 'center'}}>
                            <div style={{width: '80px', height: '80px', borderRadius: '4px', overflow: 'hidden', flexShrink: 0, background: '#eee'}}>
                              {item && item.imageUrl ? (
                                <img src={item.imageUrl} alt={item?.name} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                              ) : (
                                <div style={{width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#ccc'}}>
                                  Loading...
                                </div>
                              )}
                            </div>
                            
                            <div style={{flex: 1}}>
                                <div style={{fontWeight: 600, fontSize: '1.1rem'}}>{item ? item.name : `Item #${booking.itemId}`}</div>
                                <div style={{color: '#666', fontSize: '0.9rem', marginTop: '4px'}}>
                                  📅 {new Date(booking.startDate).toLocaleDateString()} → {new Date(booking.endDate).toLocaleDateString()}
                                </div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>
                                  💰 €{booking.totalPrice?.toFixed(2) || 'N/A'}
                                </div>
                            </div>

                            <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px'}}>
                              {renderStatus(booking.status)}
                              {booking.status === 'APPROVED' && (
                                <Link to={`/item/${booking.itemId}`} className="btn btn-outline" style={{fontSize: '0.85rem', padding: '4px 10px'}}>View item</Link>
                              )}
                            </div>
                        </div>
                      );
                    })}
                </div>
            )}
        </div>
      )}

      {/* Requests Tab */}
      {activeTab === 'requests' && (
        <div>
             {requests.length === 0 ? (
               <div style={{textAlign: 'center', padding: '60px 20px', color: '#999'}}>
                 <div style={{fontSize: '3rem', marginBottom: '12px'}}>📭</div>
                 <h3>No booking requests</h3>
                 <p>Requests from renters will appear here</p>
               </div>
             ) : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                    {requests.map(request => {
                      const item = itemsMap[request.itemId];
                      return (
                        <div key={request.id} className="sidebar-card" style={{display: 'flex', gap: '16px', alignItems: 'center'}}>
                             <div style={{width: '80px', height: '80px', borderRadius: '4px', overflow: 'hidden', flexShrink: 0, background: '#eee'}}>
                              {item && item.imageUrl ? (
                                <img src={item.imageUrl} alt={item?.name} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                              ) : (
                                <div style={{width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#ccc'}}>
                                  Loading...
                                </div>
                              )}
                            </div>
                            
                            <div style={{flex: 1}}>
                                <div style={{fontWeight: 600, fontSize: '1.1rem'}}>{item ? item.name : `Item #${request.itemId}`}</div>
                                <div style={{color: '#666', fontSize: '0.9rem', marginTop: '4px'}}>
                                  👤 Renter ID: {request.userId}
                                </div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>
                                  📅 {new Date(request.startDate).toLocaleDateString()} → {new Date(request.endDate).toLocaleDateString()}
                                </div>
                                <div style={{color: 'var(--primary)', fontWeight: 600, fontSize: '0.95rem', marginTop: '4px'}}>
                                  💰 €{request.totalPrice?.toFixed(2) || 'N/A'}
                                </div>
                            </div>
                            
                            <div style={{display: 'flex', flexDirection: 'column', gap: '8px', minWidth: '120px'}}>
                              {request.status === 'PENDING' ? (
                                <div style={{display: 'flex', gap: '8px'}}>
                                    <button 
                                      className="btn btn-primary" 
                                      style={{padding: '6px 12px', fontSize: '0.85rem', flex: 1}} 
                                      onClick={() => handleStatusUpdate(request.id, 'APPROVED')}
                                    >
                                      ✓ Accept
                                    </button>
                                    <button 
                                      className="btn btn-danger" 
                                      style={{padding: '6px 12px', fontSize: '0.85rem', flex: 1}} 
                                      onClick={() => handleStatusUpdate(request.id, 'REJECTED')}
                                    >
                                      ✕ Decline
                                    </button>
                                </div>
                              ) : (
                                <div>{renderStatus(request.status)}</div>
                              )}
                            </div>
                        </div>
                      );
                    })}
                </div>
             )}
        </div>
      )}
    </div>
  );
}
