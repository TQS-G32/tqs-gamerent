import { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Link, useNavigate } from "react-router-dom";
import Home from "./pages/Home.jsx";
import ItemDetails from "./pages/ItemDetails.jsx";
import Bookings from "./pages/Bookings.jsx";
import PostItem from "./pages/PostItem.jsx";
import AuthPage from "./pages/AuthPage.jsx";

function NavBar({ user }) {
  const [query, setQuery] = useState("");
  const currentUser = user;
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
            {currentUser ? (
              <>
                <span style={{marginLeft: '12px'}}>Hi, {currentUser.name}</span>
                <button className="btn btn-ghost" style={{marginLeft: '12px'}} onClick={async () => {
                  try {
                    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
                  } catch (e) {
                    console.error(e);
                  }
                  window.localStorage.removeItem('user');
                  window.location.href = '/';
                }}>Logout</button>
              </>
            ) : (
              <Link to="/auth" className="nav-link">Login</Link>
            )}
        </div>
      </div>
    </nav>
  );
}

export default function App() {
  const [user, setUser] = useState(() => {
    const u = window.localStorage.getItem('user');
    return u ? JSON.parse(u) : null;
  });

  useEffect(() => {
    if (user) window.localStorage.setItem('user', JSON.stringify(user));
    else window.localStorage.removeItem('user');
  }, [user]);

  useEffect(() => {
    // Check if there's an active session with the backend on app load
    const checkSession = async () => {
      try {
        const res = await fetch('/api/auth/me', {
          credentials: 'include'
        });
        if (res.ok) {
          const userData = await res.json();
          setUser(userData);
          window.localStorage.setItem('user', JSON.stringify(userData));
        } else {
          window.localStorage.removeItem('user');
        }
      } catch (err) {
        console.error('Session check failed:', err);
        window.localStorage.removeItem('user');
      }
    };
    checkSession();
  }, []);

  const handleAuth = (u) => {
    setUser(u);
  };

  return (
    <Router>
      <NavBar user={user} />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/post-item" element={<PostItem />} />
        <Route path="/item/:id" element={<ItemDetails />} />
        <Route path="/bookings" element={<Bookings />} />
        <Route path="/auth" element={<AuthPage onAuth={handleAuth} />} />
      </Routes>
    </Router>
  );
}
