import React from "react";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import Home from "./pages/Home.jsx";
import ItemDetails from "./pages/ItemDetails.jsx";
import Bookings from "./pages/Bookings.jsx";

export default function App() {
  return (
    <Router>
      <nav className="main-nav">
        <Link to="/">Home</Link>
        <Link to="/bookings">My Bookings</Link>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/item/:id" element={<ItemDetails />} />
        <Route path="/bookings" element={<Bookings />} />
      </Routes>
    </Router>
  );
}
