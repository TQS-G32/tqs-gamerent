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
    fetch(`/api/items`)
      .then((res) => res.json())
      .then((items) => setItem(items.find((i) => i.id === Number(id))));
  }, [id]);

  function handleBook(e) {
    e.preventDefault();
    fetch("/api/bookings", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ itemId: id, userId: 1, startDate, endDate }),
    })
      .then((res) => res.json())
      .then(() => {
        setSuccess(true);
        setTimeout(() => navigate("/bookings"), 1200);
      });
  }

  if (!item) return <div className="container">Loading...</div>;

  return (
    <div className="container">
      <button onClick={() => navigate(-1)} style={{ marginBottom: 16, background: "#2d3a4b", color: "#fff", border: "none", borderRadius: 6, padding: "6px 16px", cursor: "pointer" }}>&larr; Back</button>
      <h2>{item.name}</h2>
      <img src={item.imageUrl || "https://via.placeholder.com/400x200?text=No+Image"} alt={item.name} style={{ width: 400, maxWidth: "100%", borderRadius: 8 }} />
      <p>{item.description}</p>
      <div className="price">€{item.pricePerDay}/day</div>
      <form onSubmit={handleBook}>
        <label>
          Start Date:
          <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} required />
        </label>
        <label>
          End Date:
          <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} required />
        </label>
        <button type="submit">Book</button>
      </form>
      {success && <p className="success">Booking successful!</p>}
    </div>
  );
}
