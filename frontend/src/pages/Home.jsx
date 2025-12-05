import React, { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

const CATEGORIES = ["All", "Game", "Console", "Accessory"];
const PAGE_SIZE = 10;

export default function Home() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchParams] = useSearchParams();
  const q = searchParams.get('q');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [selectedCategory, setSelectedCategory] = useState("All");

  useEffect(() => {
    setCurrentPage(0); // Reset to first page when search/category changes
  }, [q, selectedCategory]);

  useEffect(() => {
    setLoading(true);
    // Always use /api/items/catalog which shows ALL items (with or without rental listings)
    let url = `/api/items/catalog?page=${currentPage}`;
    
    if (selectedCategory !== "All") {
      url += `&category=${encodeURIComponent(selectedCategory)}`;
    }
    
    if (q) {
      url += `&q=${encodeURIComponent(q)}`;
    }

    fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error(`API error: ${res.status}`);
        return res.json();
      })
      .then((data) => {
        setItems(Array.isArray(data.items) ? data.items : []);
        setTotalPages(data.totalPages || 0);
        setTotalCount(data.totalCount || 0);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [q, currentPage, selectedCategory]);

  const handleNextPage = () => {
    if (currentPage < totalPages - 1) {
      setCurrentPage(currentPage + 1);
    }
  };

  const handlePrevPage = () => {
    if (currentPage > 0) {
      setCurrentPage(currentPage - 1);
    }
  };

  return (
    <div className="container">
      {/* Category Filter */}
      <div style={{marginTop: '24px', marginBottom: '24px', display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center'}}>
        <span style={{fontWeight: 600, color: '#666', marginRight: '8px'}}>Filter:</span>
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            style={{
              padding: '8px 16px',
              borderRadius: '20px',
              border: selectedCategory === cat ? 'none' : '1px solid #ddd',
              background: selectedCategory === cat ? 'var(--primary)' : 'white',
              color: selectedCategory === cat ? 'white' : '#666',
              cursor: 'pointer',
              fontWeight: 500,
              fontSize: '0.9rem',
              transition: 'all 0.2s'
            }}
          >
            {cat}
          </button>
        ))}
      </div>

      {loading && <div style={{padding: '40px', textAlign: 'center'}}>Loading items...</div>}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      
      {!loading && !error && items.length === 0 && (
        <div style={{textAlign: 'center', padding: '60px 20px', color: '#666'}}>
            <h2>No items found</h2>
            <p>Try adjusting your filters or search for something else.</p>
            {q && <p style={{marginTop: '8px', fontSize: '0.9rem'}}>No results for "{q}"</p>}
        </div>
      )}

      <div className="item-grid">
        {items.map((item) => (
          <Link to={`/item/${item.id}`} key={item.id} className="item-card">
            <div className="card-image">
                <img src={item.imageUrl || "https://via.placeholder.com/300x400?text=No+Image"} alt={item.name} />
            </div>
            <div className="card-price">
              {item.pricePerDay != null ? (
                <>€{item.pricePerDay.toFixed(2)}</>
              ) : (
                <span style={{color:'#888', fontSize:'0.9rem'}}>Not for rent</span>
              )}
            </div>
            <div className="card-info" style={{fontSize: '0.85rem', marginTop: '2px'}}>{item.name}</div>
            <div className="card-info" style={{fontSize: '0.75rem', color: '#999'}}>{item.category || 'Gaming'}</div>
          </Link>
        ))}
      </div>

      {/* Pagination Controls */}
      {!loading && !error && items.length > 0 && (
        <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '16px', margin: '32px 0', paddingBottom: '40px'}}>
          <button 
            className="btn btn-outline"
            onClick={handlePrevPage}
            disabled={currentPage === 0}
            style={{opacity: currentPage === 0 ? 0.5 : 1, cursor: currentPage === 0 ? 'not-allowed' : 'pointer'}}
          >
            ← Previous
          </button>
          
          <div style={{display: 'flex', gap: '8px', alignItems: 'center'}}>
            <span style={{fontSize: '0.9rem', color: '#666'}}>
              Page <strong>{currentPage + 1}</strong> of <strong>{totalPages}</strong>
            </span>
            <span style={{fontSize: '0.85rem', color: '#999'}}>
              ({totalCount} total items)
            </span>
          </div>
          
          <button 
            className="btn btn-outline"
            onClick={handleNextPage}
            disabled={currentPage >= totalPages - 1}
            style={{opacity: currentPage >= totalPages - 1 ? 0.5 : 1, cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer'}}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
