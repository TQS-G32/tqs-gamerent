import React, { useEffect, useState } from "react";

export default function Bookings() {
  const [bookings, setBookings] = useState([]);

  useEffect(() => {
    fetch("/api/bookings")
      .then((res) => res.json())
      .then(setBookings);
  }, []);

  return (
    <div className="container">
      <h1>My Bookings</h1>
      <table className="table">
        <thead>
          <tr>
            <th>Booking ID</th>
            <th>Item ID</th>
            <th>User ID</th>
            <th>Start Date</th>
            <th>End Date</th>
          </tr>
        </thead>
        <tbody>
          {bookings.map((b) => (
            <tr key={b.id}>
              <td>{b.id}</td>
              <td>{b.itemId}</td>
              <td>{b.userId}</td>
              <td>{b.startDate}</td>
              <td>{b.endDate}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
