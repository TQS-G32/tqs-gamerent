import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";

export default function Home() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch("/api/items")
      .then((res) => {
        if (!res.ok) throw new Error(`API error: ${res.status}`);
        return res.json();
      })
      .then((data) => {
        console.log("Items fetched:", data);
        setItems(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Error fetching items:", err);
        setError(err.message);
        setLoading(false);
      });
  }, []);

  return (
    <div className="container">
      <h1>GameRent - Browse Items</h1>
      {loading && <p>Loading items...</p>}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <p>No items available. Add some items to get started!</p>
      )}
      <div className="flex-list">
        {items.map((item) => (
          <div key={item.id} className="item-card">
            <img src={item.imageUrl || "https://via.placeholder.com/200x120?text=No+Image"} alt={item.name} />
            <h3>{item.name}</h3>
            <p>{item.description}</p>
            <div className="price">€{item.pricePerDay}/day</div>
            <Link to={`/item/${item.id}`}>View & Book</Link>
          </div>
        ))}
      </div>
    </div>
  );
}
