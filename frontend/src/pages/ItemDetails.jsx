import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";

export default function ItemDetails() {
  const { id } = useParams();
  const [item, setItem] = useState(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetch(`/api/items/${id}`, { credentials: 'include' })
      .then((res) => res.json())
      .then((data) => setItem(data))
      .catch(err => console.error(err));
  }, [id]);

  function handleBook(e) {
    e.preventDefault();
    
    fetch("/api/bookings", {
      method: "POST",
      credentials: 'include',
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ itemId: id, startDate, endDate }),
    })
      .then((res) => {
          if (!res.ok) throw new Error("Booking failed");
          return res.json();
      })
      .then(() => {
        setSuccess(true);
        setTimeout(() => navigate("/bookings"), 1200);
      })
      .catch(err => alert(err.message));
  }

  if (!item) return <div className="container" style={{paddingTop: '40px'}}>Loading...</div>;

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
                <div style={{fontSize: '1.5rem', fontWeight: 700, marginBottom: '16px'}}>€{item.pricePerDay.toFixed(2)} <span style={{fontSize: '0.9rem', fontWeight: 400, color: '#666'}}>/ day</span></div>
                
                <div style={{borderTop: '1px solid #eee', borderBottom: '1px solid #eee', padding: '12px 0', margin: '12px 0', fontSize: '0.9rem', color: '#444'}}>
                    <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '6px'}}>
                        <span style={{color: '#888'}}>Category</span>
                        <span>{item.category || 'Gaming'}</span>
                    </div>
                    <div style={{display: 'flex', justifyContent: 'space-between'}}>
                        <span style={{color: '#888'}}>Condition</span>
                        <span>Good</span>
                    </div>
                </div>
                
                <h4 style={{fontSize: '0.9rem', margin: '0 0 8px 0', color: '#888'}}>DESCRIPTION</h4>
                <p style={{fontSize: '0.95rem', color: '#333', lineHeight: 1.5, margin: 0}}>{item.description}</p>
            </div>

            <div className="sidebar-card">
                <h3 style={{marginTop: 0, fontSize: '1.1rem'}}>Book this item</h3>
                <form onSubmit={handleBook}>
                    <div className="form-group">
                        <label className="form-label">From</label>
                        <input className="form-input" type="date" value={startDate} onChange={e => setStartDate(e.target.value)} required />
                    </div>
                    <div className="form-group">
                        <label className="form-label">To</label>
                        <input className="form-input" type="date" value={endDate} onChange={e => setEndDate(e.target.value)} required />
                    </div>
                    <button type="submit" className="btn btn-primary" style={{width: '100%'}}>Request Booking</button>
                </form>
                {success && <p style={{color: 'green', marginTop: '10px', textAlign: 'center'}}>Booking sent!</p>}
            </div>

            <div className="sidebar-card" style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                 <div style={{width: '40px', height: '40px', borderRadius: '50%', background: '#ddd', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', color: '#666'}}>
                    {item.owner ? item.owner.name.charAt(0) : 'D'}
                 </div>
                 <div>
                    <div style={{fontWeight: 600}}>{item.owner ? item.owner.name : 'Demo User'}</div>
                    <div style={{fontSize: '0.8rem', color: '#888'}}>⭐⭐⭐⭐⭐ (12)</div>
                 </div>
                 <div style={{marginLeft: 'auto'}}>
                    <button className="btn btn-outline" style={{padding: '6px 12px', fontSize: '0.8rem'}}>Contact</button>
                 </div>
            </div>
        </div>
      </div>
    </div>
  );
}
