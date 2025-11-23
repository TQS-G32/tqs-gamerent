import React, { useState } from "react";
import { BrowserRouter as Router, Routes, Route, Link, useNavigate } from "react-router-dom";
import Home from "./pages/Home.jsx";
import ItemDetails from "./pages/ItemDetails.jsx";
import Bookings from "./pages/Bookings.jsx";
import PostItem from "./pages/PostItem.jsx";

function NavBar() {
  const [query, setQuery] = useState("");
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    navigate(`/?q=${query}`);
  };

  return (
    <nav className="main-nav">
      <div className="nav-container">
        <Link to="/" className="nav-brand">GameRent</Link>
        
        <form className="nav-search" onSubmit={handleSearch}>
          <input 
            type="text" 
            placeholder="Search for items..." 
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </form>

        <div className="nav-actions">
            <Link to="/bookings" className="nav-link">My Items</Link>
            <Link to="/post-item" className="btn btn-primary">Rent your gear</Link>
        </div>
      </div>
    </nav>
  );
}

export default function App() {
  return (
    <Router>
      <NavBar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/post-item" element={<PostItem />} />
        <Route path="/item/:id" element={<ItemDetails />} />
        <Route path="/bookings" element={<Bookings />} />
      </Routes>
    </Router>
  );
}
