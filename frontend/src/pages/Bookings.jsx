import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import StarRating from '../components/StarRating.jsx';

export default function Bookings() {
  const [bookings, setBookings] = useState([]); // My Rentals
  const [requests, setRequests] = useState([]); // Incoming Requests
  const [listings, setListings] = useState([]); // My Items
  const [listingsPage, setListingsPage] = useState(0);
  const [listingsTotalPages, setListingsTotalPages] = useState(1);
  const [listingsTotalCount, setListingsTotalCount] = useState(0);
  const [listingsLoading, setListingsLoading] = useState(false);
  const [itemsMap, setItemsMap] = useState({}); // Map of itemId -> Item data
  const [activeTab, setActiveTab] = useState('listings'); // 'rentals', 'listings', 'requests'
  const [editingItemId, setEditingItemId] = useState(null); // For inline editing
  const [editForm, setEditForm] = useState({ available: true, minRentalDays: 1 });
  const [reviewDrafts, setReviewDrafts] = useState({});
  const [reviewMsgs, setReviewMsgs] = useState({});
  const [bookingReviews, setBookingReviews] = useState({});
  const listingsObserverRef = useRef(null);
  const listingsSentinelRef = useRef(null);
  
  const userJson = window.localStorage.getItem('user');
  const currentUser = userJson ? JSON.parse(userJson) : null;
  const [isOwner, setIsOwner] = useState(false);

  const draftKey = (bookingId, target) => `${bookingId}-${target}`;
  const getDraft = (bookingId, target) => reviewDrafts[draftKey(bookingId, target)] || { rating: 0, comment: '' };
  const setDraft = (bookingId, target, field, value) => {
    setReviewDrafts(prev => ({
      ...prev,
      [draftKey(bookingId, target)]: {
        ...(prev[draftKey(bookingId, target)] || { rating: 0, comment: '' }),
        [field]: value
      }
    }));
  };
  const setMsg = (bookingId, target, msg) => {
    setReviewMsgs(prev => ({ ...prev, [draftKey(bookingId, target)]: msg }));
  };
  const getMsg = (bookingId, target) => reviewMsgs[draftKey(bookingId, target)] || '';

  const canReviewBooking = (booking) => booking.status === 'APPROVED' && new Date(booking.endDate) <= new Date();
  const hasReview = (bookingId, targetType, targetId) => {
    const list = bookingReviews[bookingId] || [];
    return list.some(r => r.targetType === targetType && r.targetId === targetId && r.reviewerId === currentUser?.id);
  };
  const getUserReview = (bookingId, targetType, targetId) => {
    const list = bookingReviews[bookingId] || [];
    return list.find(r => r.targetType === targetType && r.targetId === targetId && r.reviewerId === currentUser?.id);
  };

  const submitReview = async ({ bookingId, targetType, targetId }, targetKey) => {
    setMsg(bookingId, targetKey, '');
    const draft = getDraft(bookingId, targetKey);
    try {
      const res = await fetch('/api/reviews', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          bookingId,
          targetType,
          targetId,
          rating: Number(draft.rating),
          comment: draft.comment
        })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to submit review');
      }
      await res.json();
      setMsg(bookingId, targetKey, '✓ Review submitted');
      // refresh reviews for this booking
      fetch(`/api/reviews/booking/${bookingId}`)
        .then(res => res.json())
        .then(data => setBookingReviews(prev => ({ ...prev, [bookingId]: Array.isArray(data) ? data : [] })))
        .catch(() => {});
    } catch (e) {
      setMsg(bookingId, targetKey, `✗ ${e.message}`);
    }
  };

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

    // Reset listings pagination
    setListings([]);
    setListingsPage(0);
    setListingsTotalPages(1);
    setListingsTotalCount(0);
    setListingsLoading(false);
  }, []);

  // Fetch listings paginated (infinite scroll)
  useEffect(() => {
    if (activeTab !== 'listings') return;
    if (listingsLoading) return;
    if (listingsPage >= listingsTotalPages) return;
    const ownerId = currentUser?.id || 1;
    setListingsLoading(true);
    fetch(`/api/items?ownerId=${ownerId}&page=${listingsPage}&pageSize=20`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        const newItems = Array.isArray(data.items) ? data.items : [];
        setListings(prev => [...prev, ...newItems]);
        const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 1;
        setListingsTotalPages(totalPages);
        if (Number.isFinite(data.totalCount)) {
          setListingsTotalCount(data.totalCount);
        }
        const ownerFlag = ((Array.isArray(data.items) && data.items.length > 0) || (data.totalCount && data.totalCount > 0)) || (currentUser && currentUser.role === 'ADMIN');
        setIsOwner(ownerFlag || (currentUser && currentUser.role === 'ADMIN'));
        if (ownerFlag || (currentUser && currentUser.role === 'ADMIN')) {
          fetch('/api/bookings/requests', { credentials: 'include' })
            .then(res => res.json())
            .then(reqs => setRequests(reqs))
            .catch(err => console.error(err));
        }
      })
      .catch(err => console.error(err))
      .finally(() => setListingsLoading(false));
  }, [activeTab, listingsPage]);

  // Reset listings when switching to listings tab or user changes
  useEffect(() => {
    if (activeTab === 'listings') {
      setListings([]);
      setListingsPage(0);
      setListingsTotalPages(1);
      setListingsTotalCount(0);
      setListingsLoading(false);
    }
  }, [activeTab, currentUser?.id]);

  // Intersection observer for listings infinite scroll
  useEffect(() => {
    if (activeTab !== 'listings') return;
    const sentinel = listingsSentinelRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver(
      (entries) => {
        const first = entries[0];
        if (first.isIntersecting && !listingsLoading && listingsPage + 1 < listingsTotalPages) {
          setListingsPage((p) => p + 1);
        }
      },
      { rootMargin: '200px' }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [activeTab, listingsLoading, listingsPage, listingsTotalPages]);

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

  useEffect(() => {
    const ids = [...new Set([...bookings.map(b => b.id), ...requests.map(r => r.id)])];
    if (ids.length === 0) return;
    Promise.all(ids.map(id =>
      fetch(`/api/reviews/booking/${id}`).then(res => res.json()).catch(() => [])
    )).then(results => {
      const map = {};
      ids.forEach((id, idx) => { map[id] = Array.isArray(results[idx]) ? results[idx] : []; });
      setBookingReviews(map);
    }).catch(() => {});
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

  const listingsCountDisplay = listingsTotalCount > 0 ? listingsTotalCount : listings.length;

  return (
    <div className="container">
      <h2 style={{marginBottom: '24px'}}>📊 My Dashboard</h2>
      
      <div className="tabs">
          <div className={`tab ${activeTab === 'listings' ? 'active' : ''}`} onClick={() => setActiveTab('listings')}>
            📦 My Listings ({listingsCountDisplay})
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
                    <div ref={listingsSentinelRef} style={{height:'1px'}} />
                    {listingsLoading && <div style={{color:'#666'}}>Loading more...</div>}
                    {!listingsLoading && listingsPage + 1 >= listingsTotalPages && listings.length > 0 && (
                      <div style={{color:'#999'}}>End of listings.</div>
                    )}
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
                        <div
                          key={booking.id}
                          className="sidebar-card"
                          style={{
                            position:'relative',
                            display: 'grid',
                            gridTemplateColumns: '240px 1fr',
                            gap: '16px',
                            alignItems: 'flex-start'
                          }}
                        >
                            {booking.status === 'APPROVED' && (
                              <span style={{position:'absolute', top:'10px', right:'10px', color:'#2ecc71', fontWeight:700}}>✓ APPROVED</span>
                            )}
                            <div style={{display:'flex', flexDirection:'column', gap:'10px', minWidth:'200px'}}>
                              <div style={{width: '100%', aspectRatio: '1 / 1', borderRadius: '8px', overflow: 'hidden', background: '#eee'}}>
                                {item && item.imageUrl ? (
                                  <img src={item.imageUrl} alt={item?.name} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                                ) : (
                                  <div style={{width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#ccc'}}>
                                    Loading...
                                  </div>
                                )}
                              </div>
                              <div style={{display:'flex', flexDirection:'column', gap:'6px'}}>
                                <div style={{fontWeight: 600, fontSize: '1.05rem', lineHeight:1.2}}>{item ? item.name : `Item #${booking.itemId}`}</div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>
                                  📅 {new Date(booking.startDate).toLocaleDateString()} → {new Date(booking.endDate).toLocaleDateString()}
                                </div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>
                                  💰 €{booking.totalPrice?.toFixed(2) || 'N/A'}
                                </div>
                                <div style={{display:'flex', gap:'8px', alignItems:'center', flexWrap:'wrap', marginTop:'4px'}}>
                                  {booking.status !== 'APPROVED' && renderStatus(booking.status)}
                                  {booking.status === 'APPROVED' && (
                                    <Link to={`/item/${booking.itemId}`} className="btn btn-outline" style={{fontSize: '0.85rem', padding: '4px 10px'}}>View item</Link>
                                  )}
                                </div>
                              </div>
                            </div>
                            {canReviewBooking(booking) && (
                              <div style={{marginTop:'12px', width:'100%', borderTop:'1px solid #eee', paddingTop:'12px'}}>
                                <div style={{fontWeight:600, marginBottom:'10px', fontSize:'1rem'}}>Leave a review</div>
                                <div style={{display:'grid', gridTemplateColumns:'1fr', gap:'12px'}}>
                                  <div style={{border:'1px solid #e6e6e6', borderRadius:'10px', padding:'12px', background:'#fafafa'}}>
                                    <div style={{fontWeight:600, marginBottom:'6px'}}>Item rating</div>
                                    {hasReview(booking.id, 'ITEM', booking.itemId) ? (
                                      <>
                                        <StarRating value={getUserReview(booking.id, 'ITEM', booking.itemId)?.rating || 0} disabled size={24} />
                                        <div style={{marginTop:'6px', color:'#333', lineHeight:1.4}}>
                                          {getUserReview(booking.id, 'ITEM', booking.itemId)?.comment || 'No comment provided.'}
                                        </div>
                                        <div style={{marginTop:'6px', color:'#155724', fontSize:'0.9rem'}}>Already submitted</div>
                                      </>
                                    ) : (
                                      <>
                                        <div style={{display:'flex', alignItems:'center', gap:'8px', marginBottom:'8px'}}>
                                        <StarRating
                                          value={Number(getDraft(booking.id, 'item').rating)}
                                          onChange={(n) => setDraft(booking.id, 'item', 'rating', n)}
                                          size={26}
                                        />
                                        </div>
                                        <textarea
                                          className="form-input"
                                          rows={3}
                                          placeholder="How was the item?"
                                          value={getDraft(booking.id, 'item').comment}
                                          onChange={e => setDraft(booking.id, 'item', 'comment', e.target.value)}
                                          style={{width:'100%'}}
                                        />
                                        <button
                                          className="btn btn-primary"
                                          style={{
                                            marginTop:'8px',
                                            opacity: getDraft(booking.id, 'item').rating ? 1 : 0.5,
                                            cursor: getDraft(booking.id, 'item').rating ? 'pointer' : 'not-allowed'
                                          }}
                                          disabled={!getDraft(booking.id, 'item').rating}
                                          onClick={() => submitReview({ bookingId: booking.id, targetType: 'ITEM', targetId: booking.itemId }, 'item')}
                                        >
                                          Submit
                                        </button>
                                        {getMsg(booking.id, 'item') && (
                                          <div style={{marginTop:'4px', color:getMsg(booking.id, 'item').startsWith('✓') ? '#155724' : '#721c24', fontSize:'0.9rem'}}>
                                            {getMsg(booking.id, 'item')}
                                          </div>
                                        )}
                                      </>
                                    )}
                                  </div>
                                  <div style={{border:'1px solid #e6e6e6', borderRadius:'10px', padding:'12px', background:'#fafafa'}}>
                                    <div style={{fontWeight:600, marginBottom:'6px'}}>Owner rating</div>
                                    {hasReview(booking.id, 'USER', item?.owner?.id) ? (
                                      <>
                                        <StarRating value={getUserReview(booking.id, 'USER', item?.owner?.id)?.rating || 0} disabled size={24} />
                                        <div style={{marginTop:'6px', color:'#333', lineHeight:1.4}}>
                                          {getUserReview(booking.id, 'USER', item?.owner?.id)?.comment || 'No comment provided.'}
                                        </div>
                                        <div style={{marginTop:'6px', color:'#155724', fontSize:'0.9rem'}}>Already submitted</div>
                                      </>
                                    ) : (
                                      <>
                                        <div style={{display:'flex', alignItems:'center', gap:'8px', marginBottom:'8px'}}>
                                          <StarRating
                                            value={Number(getDraft(booking.id, 'owner').rating)}
                                            onChange={(n) => setDraft(booking.id, 'owner', 'rating', n)}
                                            size={26}
                                          />
                                        </div>
                                        <textarea
                                          className="form-input"
                                          rows={3}
                                          placeholder="How was the owner?"
                                          value={getDraft(booking.id, 'owner').comment}
                                          onChange={e => setDraft(booking.id, 'owner', 'comment', e.target.value)}
                                          style={{width:'100%'}}
                                        />
                                        <button
                                          className="btn btn-primary"
                                          style={{
                                            marginTop:'6px',
                                            opacity: (!item || !item.owner || !getDraft(booking.id, 'owner').rating) ? 0.5 : 1,
                                            cursor: (!item || !item.owner || !getDraft(booking.id, 'owner').rating) ? 'not-allowed' : 'pointer'
                                          }}
                                          disabled={!item || !item.owner || !getDraft(booking.id, 'owner').rating}
                                          onClick={() => item && item.owner && submitReview({ bookingId: booking.id, targetType: 'USER', targetId: item.owner.id }, 'owner')}
                                        >
                                          Submit
                                        </button>
                                        {getMsg(booking.id, 'owner') && (
                                          <div style={{marginTop:'4px', color:getMsg(booking.id, 'owner').startsWith('✓') ? '#155724' : '#721c24', fontSize:'0.9rem'}}>
                                            {getMsg(booking.id, 'owner')}
                                          </div>
                                        )}
                                      </>
                                    )}
                                    {!item || !item.owner ? (
                                      <div style={{marginTop:'4px', color:'#888', fontSize:'0.85rem'}}>Loading owner info...</div>
                                    ) : null}
                                  </div>
                                </div>
                              </div>
                            )}
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
                        <div
                          key={request.id}
                          className="sidebar-card"
                          style={{
                            position:'relative',
                            display: 'grid',
                            gridTemplateColumns: '240px 1fr',
                            gap: '16px',
                            alignItems: 'flex-start'
                          }}
                        >
                             {request.status === 'APPROVED' && (
                              <span style={{position:'absolute', top:'10px', right:'10px', color:'#2ecc71', fontWeight:700}}>✓ APPROVED</span>
                            )}
                             <div style={{display:'flex', flexDirection:'column', gap:'10px', minWidth:'200px'}}>
                              <div style={{width: '100%', aspectRatio: '1 / 1', borderRadius: '8px', overflow: 'hidden', background: '#eee'}}>
                                {item && item.imageUrl ? (
                                  <img src={item.imageUrl} alt={item?.name} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                                ) : (
                                  <div style={{width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#ccc'}}>
                                    Loading...
                                  </div>
                                )}
                              </div>
                              <div style={{display:'flex', flexDirection:'column', gap:'6px'}}>
                                <div style={{fontWeight: 600, fontSize: '1.05rem', lineHeight:1.2}}>{item ? item.name : `Item #${request.itemId}`}</div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>
                                  👤 Renter ID: {request.userId}
                                </div>
                                <div style={{color: '#666', fontSize: '0.9rem'}}>
                                  📅 {new Date(request.startDate).toLocaleDateString()} → {new Date(request.endDate).toLocaleDateString()}
                                </div>
                                <div style={{color: 'var(--primary)', fontWeight: 600, fontSize: '0.95rem', marginTop: '4px'}}>
                                  💰 €{request.totalPrice?.toFixed(2) || 'N/A'}
                                </div>
                                <div style={{display:'flex', gap:'8px', alignItems:'center', flexWrap:'wrap', marginTop:'4px'}}>
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
                                    request.status !== 'APPROVED' && <div>{renderStatus(request.status)}</div>
                                  )}
                                </div>
                              </div>
                            </div>
                            {canReviewBooking(request) && (
                              <div style={{marginTop:'12px', width:'100%', borderTop:'1px solid #eee', paddingTop:'12px'}}>
                                <div style={{fontWeight:600, marginBottom:'10px', fontSize:'1rem'}}>Review renter</div>
                                <div style={{border:'1px solid #e6e6e6', borderRadius:'10px', padding:'12px', background:'#fafafa'}}>
                                  {hasReview(request.id, 'USER', request.userId) ? (
                                    <>
                                      <StarRating value={getUserReview(request.id, 'USER', request.userId)?.rating || 0} disabled size={24} />
                                      <div style={{marginTop:'6px', color:'#333', lineHeight:1.4}}>
                                        {getUserReview(request.id, 'USER', request.userId)?.comment || 'No comment provided.'}
                                      </div>
                                      <div style={{marginTop:'6px', color:'#155724', fontSize:'0.9rem'}}>Already submitted</div>
                                    </>
                                  ) : (
                                    <>
                                      <div style={{display:'flex', alignItems:'center', gap:'8px', marginBottom:'8px'}}>
                                        <span style={{fontWeight:600}}>Rating</span>
                                        <StarRating
                                          value={Number(getDraft(request.id, 'renter').rating)}
                                          onChange={(n) => setDraft(request.id, 'renter', 'rating', n)}
                                          size={26}
                                        />
                                      </div>
                                      <textarea
                                        className="form-input"
                                        rows={3}
                                        placeholder="How was the renter?"
                                        value={getDraft(request.id, 'renter').comment}
                                        onChange={e => setDraft(request.id, 'renter', 'comment', e.target.value)}
                                        style={{width:'100%'}}
                                      />
                                      <button
                                        className="btn btn-primary"
                                        style={{
                                          marginTop:'8px',
                                          opacity: getDraft(request.id, 'renter').rating ? 1 : 0.5,
                                          cursor: getDraft(request.id, 'renter').rating ? 'pointer' : 'not-allowed'
                                        }}
                                        disabled={!getDraft(request.id, 'renter').rating}
                                        onClick={() => submitReview({ bookingId: request.id, targetType: 'USER', targetId: request.userId }, 'renter')}
                                      >
                                        Submit
                                      </button>
                                      {getMsg(request.id, 'renter') && (
                                        <div style={{marginTop:'4px', color:getMsg(request.id, 'renter').startsWith('✓') ? '#155724' : '#721c24', fontSize:'0.9rem'}}>
                                          {getMsg(request.id, 'renter')}
                                        </div>
                                      )}
                                    </>
                                  )}
                                </div>
                              </div>
                            )}
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
