import React, { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

export default function Home() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchParams] = useSearchParams();
  const q = searchParams.get('q');

  useEffect(() => {
    setLoading(true);
    let url = "/api/items";
    if (q) {
        url = `/api/items/search?q=${q}`;
    }

    fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error(`API error: ${res.status}`);
        return res.json();
      })
      .then((data) => {
        setItems(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [q]);

  return (
    <div className="container">
      {loading && <div style={{padding: '40px', textAlign: 'center'}}>Loading items...</div>}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      
      {!loading && !error && items.length === 0 && (
        <div style={{textAlign: 'center', padding: '60px', color: '#666'}}>
            <h2>No items found</h2>
            <p>Try searching for something else or be the first to list it!</p>
        </div>
      )}

      <div className="item-grid">
        {items.map((item) => (
          <Link to={`/item/${item.id}`} key={item.id} className="item-card">
            <div className="card-image">
                <img src={item.imageUrl || "https://via.placeholder.com/300x400?text=No+Image"} alt={item.name} />
            </div>
            <div className="card-price">€{item.pricePerDay.toFixed(2)}</div>
            <div className="card-info" style={{fontSize: '0.85rem', marginTop: '2px'}}>{item.name}</div>
            <div className="card-info" style={{fontSize: '0.75rem', color: '#999'}}>{item.category || 'Game'}</div>
          </Link>
        ))}
      </div>
    </div>
  );
}
