import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";

export default function ItemDetails() {
  const { id } = useParams();
  const [item, setItem] = useState(null);
  const [rentals, setRentals] = useState([]); // All rental listings for this item
  const [bookings, setBookings] = useState([]);
  const [selectedRental, setSelectedRental] = useState(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [success, setSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [showLoginPrompt, setShowLoginPrompt] = useState(false);
  const [ownerMode, setOwnerMode] = useState(false);
  const [ownerAvailable, setOwnerAvailable] = useState(false);
  const [ownerMinDays, setOwnerMinDays] = useState(1);
  const [ownerMsg, setOwnerMsg] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetch(`/api/items/${id}`, { credentials: 'include' })
      .then((res) => res.json())
      .then((data) => {
        setItem(data);
        // If this item has a rental listing, select it by default
        if (data.pricePerDay != null && data.available) {
          setSelectedRental(data);
        }
        // Setup owner controls if current user is owner
        try {
          const stored = window.localStorage.getItem('user');
          if (stored) {
            const u = JSON.parse(stored);
            if (u && data.owner && u.id === data.owner.id) {
              setOwnerMode(true);
              setOwnerAvailable(!!data.available);
              setOwnerMinDays(data.minRentalDays || 1);
            }
          }
        } catch (e) {}
      })
      .catch(err => console.error(err));

    // Fetch bookings for this item to show availability
    fetch(`/api/bookings?itemId=${id}`, { credentials: 'include' })
      .then((res) => res.json())
      .then((data) => setBookings(Array.isArray(data) ? data : []))
      .catch(err => console.error(err));
  }, [id]);

  // For now, we only show the item itself as a rental listing
  // In a real app, you'd fetch all versions of this item listed by different users
  useEffect(() => {
    if (item && item.pricePerDay != null) {
      setRentals([item]);
    } else {
      setRentals([]);
    }
  }, [item]);

  function handleBook(e, rentalItem) {
    e.preventDefault();
    setErrorMsg("");
    setSuccess(false);
    
    const user = window.localStorage.getItem('user');
    if (!user) {
        setShowLoginPrompt(true);
        return;
    }

    // Validate dates
    if (!startDate || !endDate) {
      setErrorMsg("Please select both start and end dates");
      return;
    }

    if (new Date(startDate) >= new Date(endDate)) {
      setErrorMsg("Start date must be before end date");
      return;
    }

    // Enforce minimum rental days client-side
    const days = Math.ceil((new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24)) + 1;
    const minDays = rentalItem.minRentalDays || 1;
    if (days < minDays) {
      setErrorMsg(`This item requires a minimum rental period of ${minDays} day${minDays > 1 ? 's' : ''}. You selected ${days} day${days > 1 ? 's' : ''}.`);
      return;
    }

    fetch("/api/bookings", {
      method: "POST",
      credentials: 'include',
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ itemId: rentalItem.id, startDate, endDate }),
    })
      .then((res) => {
          if (res.status === 401 || res.status === 403) {
              setShowLoginPrompt(true);
              throw new Error("Unauthorized");
          }
          if (!res.ok) return res.json().then(err => { throw new Error(err.message || "Booking failed") });
          return res.json();
      })
      .then(() => {
        setSuccess(true);
        setStartDate("");
        setEndDate("");
        setTimeout(() => navigate("/bookings"), 1500);
      })
      .catch(err => {
          if (err.message !== "Unauthorized") {
              setErrorMsg(err.message);
          }
      });
  }

  // Calculate blocked dates from approved bookings
  const getBlockedDateRanges = () => {
    return bookings
      .filter(b => b.status === 'APPROVED')
      .map(b => ({ start: new Date(b.startDate), end: new Date(b.endDate) }));
  };

  const isDateAvailable = (dateStr) => {
    const date = new Date(dateStr);
    const blockedRanges = getBlockedDateRanges();
    return !blockedRanges.some(range => date >= range.start && date <= range.end);
  };

  const getMinDate = () => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  };

  if (!item) return <div className="container" style={{paddingTop: '40px'}}>Loading...</div>;

  const approvedBookingsCount = bookings.filter(b => b.status === 'APPROVED').length;
  const daysSincePosted = Math.floor((Date.now() - new Date(item.createdAt || Date.now()).getTime()) / (1000 * 60 * 60 * 24)) || 0;

  return (
    <div className="container">
      <div className="details-layout">
        <div className="details-gallery">
             <img 
                src={item.imageUrl || "https://via.placeholder.com/600x800?text=No+Image"} 
                alt={item.name} 
                style={{ width: '100%', height: 'auto', display: 'block', objectFit: 'contain', maxHeight: '600px', background: '#f9f9f9' }} 
             />
        </div>

        <div className="details-sidebar">
            <div className="sidebar-card">
                <h1 style={{fontSize: '1.5rem', margin: '0 0 8px 0', fontWeight: 600}}>{item.name}</h1>
                
                <div style={{borderTop: '1px solid #eee', borderBottom: '1px solid #eee', padding: '12px 0', margin: '12px 0', fontSize: '0.9rem', color: '#444'}}>
                    <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '6px'}}>
                        <span style={{color: '#888'}}>Category</span>
                        <span style={{fontWeight: 500}}>{item.category || 'Gaming'}</span>
                    </div>
                </div>
                
                <h4 style={{fontSize: '0.9rem', margin: '0 0 8px 0', color: '#888'}}>DESCRIPTION</h4>
                <p style={{fontSize: '0.95rem', color: '#333', lineHeight: 1.5, margin: 0}}>{item.description}</p>
            </div>

            {/* Rental Listings Section */}
            {rentals.length > 0 ? (
              <>
                <div className="sidebar-card">
                  <h3 style={{marginTop: 0, fontSize: '1.1rem', marginBottom: '16px'}}>📋 Available Rentals</h3>
                  <div style={{display: 'flex', flexDirection: 'column', gap: '12px'}}>
                    {rentals.map((rental) => (
                      <div 
                        key={rental.id}
                        onClick={() => setSelectedRental(rental)}
                        style={{
                          padding: '12px',
                          borderRadius: '6px',
                          border: selectedRental?.id === rental.id ? '2px solid var(--primary)' : '1px solid #ddd',
                          background: selectedRental?.id === rental.id ? '#f0fcfd' : '#f9f9f9',
                          cursor: 'pointer',
                          transition: 'all 0.2s'
                        }}
                      >
                        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start'}}>
                          <div style={{flex: 1}}>
                            <div style={{fontWeight: 600, fontSize: '1.1rem', color: 'var(--primary)'}}>
                              €{rental.pricePerDay.toFixed(2)}/day
                            </div>
                            <div style={{fontSize: '0.85rem', color: '#666', marginTop: '4px'}}>
                              by {rental.owner ? rental.owner.name : 'Unknown'}
                            </div>
                            {rental.minRentalDays && rental.minRentalDays > 1 && (
                              <div style={{
                                fontSize: '0.75rem', 
                                color: '#666', 
                                marginTop: '6px',
                                padding: '3px 8px',
                                background: '#fff',
                                border: '1px solid #ddd',
                                borderRadius: '4px',
                                display: 'inline-block'
                              }}>
                                Min. {rental.minRentalDays} days
                              </div>
                            )}
                          </div>
                          <div style={{fontSize: '1.2rem'}}>
                            {selectedRental?.id === rental.id ? '✓' : ''}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Booking Card */}
                {selectedRental && (
                  <div className="sidebar-card" style={{background: '#f9f9f9', borderColor: '#ddd'}}>
                    <h3 style={{marginTop: 0, fontSize: '1.1rem', marginBottom: '16px'}}>📅 Book this item</h3>
                    <form onSubmit={(e) => handleBook(e, selectedRental)}>
                        <div className="form-group">
                            <label className="form-label">Start Date</label>
                            <input 
                              className="form-input" 
                              type="date" 
                              value={startDate} 
                              onChange={e => setStartDate(e.target.value)}
                              min={getMinDate()}
                              required 
                              style={{borderColor: startDate && !isDateAvailable(startDate) ? '#e74c3c' : '#ddd'}}
                            />
                            {startDate && !isDateAvailable(startDate) && (
                              <small style={{color: '#e74c3c', fontSize: '0.8rem'}}>This date is blocked</small>
                            )}
                        </div>
                        <div className="form-group">
                            <label className="form-label">End Date</label>
                            <input 
                              className="form-input" 
                              type="date" 
                              value={endDate} 
                              onChange={e => setEndDate(e.target.value)}
                              min={startDate || getMinDate()}
                              required 
                              style={{borderColor: endDate && !isDateAvailable(endDate) ? '#e74c3c' : '#ddd'}}
                            />
                            {endDate && !isDateAvailable(endDate) && (
                              <small style={{color: '#e74c3c', fontSize: '0.8rem'}}>This date is blocked</small>
                            )}
                        </div>

                        {startDate && endDate && (
                          <div style={{background: '#f0f0f0', padding: '12px', borderRadius: '4px', marginBottom: '12px', fontSize: '0.9rem'}}>
                            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '4px'}}>
                              <span>Days:</span>
                              <strong>{Math.ceil((new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24)) + 1}</strong>
                            </div>
                            <div style={{display: 'flex', justifyContent: 'space-between', fontWeight: 600, color: 'var(--primary)', fontSize: '1rem'}}>
                              <span>Total:</span>
                              <span>€{(selectedRental.pricePerDay * (Math.ceil((new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24)) + 1)).toFixed(2)}</span>
                            </div>
                          </div>
                        )}

                        <button 
                          type="submit" 
                          className="btn btn-primary" 
                          style={{width: '100%'}}
                          disabled={!startDate || !endDate || !isDateAvailable(startDate) || !isDateAvailable(endDate)}
                        >
                          Request Booking
                        </button>
                    </form>

                    {success && (
                      <div style={{marginTop: '12px', padding: '12px', background: '#d4edda', border: '1px solid #c3e6cb', borderRadius: '4px', color: '#155724', fontSize: '0.9rem'}}>
                        ✓ Booking request sent! Redirecting to dashboard...
                      </div>
                    )}

                    {errorMsg && (
                      <div style={{marginTop: '12px', padding: '12px', background: '#f8d7da', border: '1px solid #f5c6cb', borderRadius: '4px', color: '#721c24', fontSize: '0.9rem'}}>
                        ✗ {errorMsg}
                      </div>
                    )}

                    {showLoginPrompt && (
                        <div style={{
                            marginTop: '16px', 
                            padding: '12px', 
                            background: '#fff3cd', 
                            border: '1px solid #ffeeba', 
                            borderRadius: '4px',
                            color: '#856404',
                            fontSize: '0.9rem'
                        }}>
                            <strong>🔐 Login Required</strong><br/>
                            You must be logged in to book items. <Link to="/auth" style={{color: '#856404', textDecoration: 'underline', fontWeight: 600}}>Log in here</Link>.
                        </div>
                    )}

                    {approvedBookingsCount > 0 && (
                      <div style={{marginTop: '12px', padding: '12px', background: '#e7f3ff', border: '1px solid #b3d9ff', borderRadius: '4px', color: '#004085', fontSize: '0.85rem'}}>
                        <strong>ℹ️ Note:</strong> This item has {approvedBookingsCount} active booking(s). Check availability on the calendar above.
                      </div>
                    )}
                  </div>
                )}

                {/* Owner Configuration - Only visible when viewing own item */}
                {ownerMode && (
                  <div className="sidebar-card" style={{background: '#fff9e6', borderColor: '#ffe066'}}>
                    <h3 style={{marginTop: 0, fontSize: '1.1rem', marginBottom: '16px'}}>⚙️ Configure Your Listing</h3>
                    <div style={{marginBottom: '16px'}}>
                      <label style={{display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.95rem'}}>
                        <input 
                          type="checkbox" 
                          checked={ownerAvailable}
                          onChange={(e) => setOwnerAvailable(e.target.checked)}
                          style={{width: '18px', height: '18px', cursor: 'pointer'}}
                        />
                        <span style={{fontWeight: 600}}>Available for rent</span>
                      </label>
                    </div>
                    <div style={{marginBottom: '16px'}}>
                      <label style={{display: 'block', fontSize: '0.9rem', marginBottom: '6px', fontWeight: 600}}>
                        Minimum rental period
                      </label>
                      <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <input 
                          type="number" 
                          min="1" 
                          max="30"
                          value={ownerMinDays}
                          onChange={(e) => setOwnerMinDays(parseInt(e.target.value) || 1)}
                          style={{width: '80px', padding: '8px', borderRadius: '4px', border: '1px solid #ddd', fontSize: '0.9rem'}}
                        />
                        <span style={{fontSize: '0.9rem', color: '#666'}}>day{ownerMinDays > 1 ? 's' : ''} (max 30)</span>
                      </div>
                    </div>
                    <div style={{display: 'flex', gap: '8px'}}>
                      <button 
                        className="btn btn-primary" 
                        onClick={async () => {
                          setOwnerMsg('');
                          try {
                            const res = await fetch(`/api/items/${item.id}/settings`, {
                              method: 'PUT', 
                              credentials: 'include', 
                              headers: {'Content-Type':'application/json'},
                              body: JSON.stringify({ available: ownerAvailable, minRentalDays: ownerMinDays })
                            });
                            if (!res.ok) {
                              const err = await res.json().catch(()=>({message:'Failed to update settings'}));
                              // Show the actual error message from backend
                              setOwnerMsg('✗ ' + (err.message || 'Failed to update settings'));
                            } else {
                              const body = await res.json();
                              setOwnerMsg('✓ Settings updated successfully');
                              // Refresh item data
                              const updated = body.item || (await (await fetch(`/api/items/${id}`, {credentials:'include'})).json());
                              setItem(updated);
                              setOwnerAvailable(!!updated.available);
                              setOwnerMinDays(updated.minRentalDays || 1);
                              if (updated.pricePerDay != null && updated.available) {
                                setSelectedRental(updated);
                              }
                            }
                          } catch (err) {
                            setOwnerMsg('✗ ' + (err.message || 'Failed to update settings'));
                          }
                        }}
                        style={{flex: 1}}
                      >
                        💾 Save Changes
                      </button>
                      <button 
                        className="btn btn-outline" 
                        onClick={() => { 
                          setOwnerAvailable(!!item.available); 
                          setOwnerMinDays(item.minRentalDays || 1); 
                          setOwnerMsg(''); 
                        }}
                      >
                        ↺ Reset
                      </button>
                    </div>
                    {ownerMsg && (
                      <div style={{
                        marginTop: '12px', 
                        padding: '10px', 
                        borderRadius: '4px',
                        background: ownerMsg.includes('✓') ? '#d4edda' : '#f8d7da',
                        color: ownerMsg.includes('✓') ? '#155724' : '#721c24',
                        fontSize: '0.9rem'
                      }}>
                        {ownerMsg}
                      </div>
                    )}
                  </div>
                )}
              </>
            ) : (
              <div className="sidebar-card" style={{textAlign: 'center', padding: '24px 0'}}>
                <div style={{fontSize: '3rem', marginBottom: '12px'}}>✨</div>
                <h3 style={{margin: '0 0 8px 0', fontSize: '1.1rem'}}>No rental listings found</h3>
                <p style={{color: '#666', marginTop: '8px', fontSize: '0.95rem', lineHeight: 1.5}}>
                  This item hasn't been listed for rent yet!
                </p>
                <div style={{display:'flex', gap: '8px', justifyContent:'center', marginTop:'16px', flexWrap: 'wrap'}}>
                    <Link to="/" className="btn btn-outline">← Back to browsing</Link>

                </div>
              </div>
            )}

            {/* Owner Card */}
            {selectedRental && (
              <div className="sidebar-card" style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                   <div style={{width: '48px', height: '48px', borderRadius: '50%', background: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', color: 'white', fontSize: '1.2rem', flexShrink: 0}}>
                      {selectedRental.owner ? selectedRental.owner.name.charAt(0).toUpperCase() : 'U'}
                   </div>
                   <div style={{flex: 1}}>
                      <div style={{fontWeight: 600, fontSize: '1rem'}}>{selectedRental.owner ? selectedRental.owner.name : 'Unknown Owner'}</div>
                      <div style={{fontSize: '0.8rem', color: '#888'}}>Member for {daysSincePosted > 0 ? daysSincePosted + ' days' : 'recently'}</div>
                   </div>
                   <div>
                      <button className="btn btn-outline" style={{padding: '6px 12px', fontSize: '0.85rem'}}>Message</button>
                   </div>
              </div>
            )}
        </div>
      </div>
    </div>
  );
}
