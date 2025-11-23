import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

const DEFAULT_PLATFORMS = ["PlayStation 5", "PlayStation 4", "Xbox Series X/S", "Xbox One", "Nintendo Switch", "PC", "Other"];

// Debounce hook
function useDebounce(value, delay) {
    const [debouncedValue, setDebouncedValue] = useState(value);
    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedValue(value);
        }, delay);
        return () => {
            clearTimeout(handler);
        };
    }, [value, delay]);
    return debouncedValue;
}

export default function PostItem() {
  const [step, setStep] = useState(0); // 0: Type, 1: Search (optional), 2: Details
  const [itemType, setItemType] = useState(""); // "Game", "Console", "Accessory"
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  
  // Debounce the query
  const debouncedQuery = useDebounce(query, 500);
  
  // Item Data
  const [selectedGame, setSelectedGame] = useState(null); // Raw IGDB data
  const [manualName, setManualName] = useState(""); // For Manual/Accessory
  const [selectedPlatform, setSelectedPlatform] = useState(""); 
  
  const [formData, setFormData] = useState({
    pricePerDay: "",
    description: "",
    category: "Game",
    imageUrl: ""
  });
  const navigate = useNavigate();

  // Effect for auto-search
  useEffect(() => {
    if (debouncedQuery && step === 1) {
        searchGames(debouncedQuery);
    } else if (!debouncedQuery) {
        setResults([]);
    }
  }, [debouncedQuery, step]);

  const handleTypeSelection = (type) => {
    setItemType(type);
    setFormData({...formData, category: type});
    setResults([]);
    setQuery("");
    setSelectedGame(null);
    setManualName("");
    setSelectedPlatform("");

    if (type === "Accessory") {
        // Skip search for accessories
        setStep(2);
        setFormData(prev => ({
            ...prev, 
            category: "Accessory",
            imageUrl: "" // Start empty for accessory
        }));
    } else {
        setStep(1);
    }
  };

  const searchGames = async (searchTerm) => {
    setLoading(true);
    try {
        const res = await fetch(`/api/igdb/search?q=${searchTerm}&type=${itemType}`);
        const data = await res.json();
        setResults(data);
    } catch (err) {
        console.error("Search failed", err);
    } finally {
        setLoading(false);
    }
  };

  const getImage = (item) => {
      if (item.cover && item.cover.url) return item.cover.url;
      if (item.platform_logo && item.platform_logo.url) return item.platform_logo.url;
      return null;
  }

  const selectGame = (game) => {
    let imageUrl = "";
    const rawUrl = getImage(game);
    
    if (rawUrl) {
        imageUrl = rawUrl.startsWith("//") ? "https:" + rawUrl : rawUrl;
        imageUrl = imageUrl.replace("t_thumb", "t_cover_big").replace("t_logo_med", "t_logo_huge"); 
    }
    
    setSelectedGame(game);
    
    // Pre-select platform if only one exists
    if (itemType === "Game" && game.platforms && game.platforms.length > 0) {
        setSelectedPlatform(game.platforms[0].name);
    } else {
        setSelectedPlatform("");
    }

    setFormData({
      ...formData,
      imageUrl: imageUrl
    });
    setStep(2);
  };

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onloadend = () => {
            setFormData({ ...formData, imageUrl: reader.result });
        };
        reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    let finalName = "";
    
    if (itemType === "Accessory") {
        finalName = manualName;
    } else {
        finalName = selectedGame.name;
        // Append platform to name for Games
        if (itemType === "Game" && selectedPlatform) {
            finalName = `${selectedGame.name} (${selectedPlatform})`;
        }
    }

    if (!finalName) return;

    // Mandatory Image Check
    if (!formData.imageUrl) {
        alert("Please add at least one photo of your item.");
        return;
    }

    const item = {
      name: finalName,
      description: formData.description,
      pricePerDay: parseFloat(formData.pricePerDay),
      imageUrl: formData.imageUrl,
      category: formData.category
    };

    try {
        const res = await fetch("/api/items", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(item)
        });
    
        if (res.ok) {
          navigate("/");
        } else {
          alert("Failed to post item");
        }
    } catch (err) {
        console.error("Post failed", err);
        alert("Failed to post item");
    }
  };

  const getTypeIcon = (type) => {
      switch(type) {
          case 'Game': return '🎮';
          case 'Console': return '🕹️';
          case 'Accessory': return '🎧';
          default: return '📦';
      }
  }

  // Determine available platforms for dropdown
  const getPlatformOptions = () => {
      if (selectedGame && selectedGame.platforms) {
          return selectedGame.platforms.map(p => p.name);
      }
      return DEFAULT_PLATFORMS;
  }

  return (
    <div className="container" style={{maxWidth: '800px', marginTop: '40px'}}>
      
      {/* Step 0: Type Selection */}
      {step === 0 && (
        <div style={{textAlign: 'center'}}>
            <h2 style={{marginBottom: '30px'}}>What are you listing today?</h2>
            <div style={{display: 'flex', gap: '20px', justifyContent: 'center', flexWrap: 'wrap'}}>
                {['Game', 'Console', 'Accessory'].map(type => (
                    <div 
                        key={type}
                        onClick={() => handleTypeSelection(type)}
                        style={{
                            padding: '40px', 
                            border: '2px solid #eee', 
                            borderRadius: '8px', 
                            cursor: 'pointer', 
                            width: '200px',
                            transition: 'all 0.2s',
                            background: 'white'
                        }}
                        onMouseOver={(e) => e.currentTarget.style.borderColor = 'var(--primary)'}
                        onMouseOut={(e) => e.currentTarget.style.borderColor = '#eee'}
                    >
                        <div style={{fontSize: '3rem', marginBottom: '10px'}}>
                            {type === 'Game' && '🎮'}
                            {type === 'Console' && '🕹️'}
                            {type === 'Accessory' && '🎧'}
                        </div>
                        <h3 style={{margin: 0}}>
                            {type === 'Game' && 'Video Game'}
                            {type === 'Console' && 'Console'}
                            {type === 'Accessory' && 'Accessory'}
                        </h3>
                    </div>
                ))}
            </div>
        </div>
      )}

      {/* Step 1: Search (Skipped for Accessories) */}
      {step === 1 && (
        <div>
            <div style={{display: 'flex', alignItems: 'center', marginBottom: '30px'}}>
                 <button className="btn btn-outline" onClick={() => setStep(0)} style={{marginRight: '20px'}}>← Back</button>
                 <h2 style={{margin: 0}}>Search for your {itemType}</h2>
            </div>
            
            <div style={{display: 'flex', gap: '10px', marginBottom: '30px'}}>
                <input 
                  type="text" 
                  className="form-input"
                  value={query} 
                  onChange={(e) => setQuery(e.target.value)} 
                  placeholder={`Start typing ${itemType} name...`}
                  autoFocus
                />
            </div>
          
            {loading && <p style={{color: '#888'}}>Searching...</p>}
            
            {!loading && results.length > 0 && <h4 style={{marginBottom: '16px', color: '#666'}}>Select a match:</h4>}
            <div className="item-grid">
                {results.map((game) => {
                    const rawUrl = getImage(game);
                    const displayUrl = rawUrl ? (rawUrl.startsWith("//") ? "https:" + rawUrl : rawUrl) : null;

                    return (
                        <div key={game.id} className="item-card" onClick={() => selectGame(game)}>
                            <div className="card-image">
                                {displayUrl ? (
                                <img 
                                    src={displayUrl} 
                                    alt={game.name} 
                                />
                                ) : (
                                    <div style={{width: '100%', height: '100%', background: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>No Image</div>
                                )}
                            </div>
                            <div className="card-info" style={{fontWeight: 600, padding: '8px 0'}}>{game.name}</div>
                            {itemType === 'Game' && game.platforms && (
                                <div className="card-info" style={{fontSize: '0.75rem'}}>{game.platforms.map(p => p.name).join(', ').substring(0, 30)}...</div>
                            )}
                        </div>
                    );
                })}
            </div>
            
            {!loading && results.length === 0 && query && (
                <div style={{textAlign: 'center', color: '#888', marginTop: '40px'}}>
                    <p>No results found.</p>
                </div>
            )}
        </div>
      )}

      {/* Step 2: Details */}
      {step === 2 && (
        <div>
            <button className="btn btn-outline" onClick={() => {
                if (itemType === 'Accessory') setStep(0);
                else setStep(1);
            }} style={{marginBottom: '20px'}}>← Back</button>
            
            <div className="sidebar-card">
                
                {/* Image Upload Section */}
                <div style={{marginBottom: '20px', paddingBottom: '20px', borderBottom: '1px solid #eee'}}>
                    <h4 style={{marginTop: 0, marginBottom: '12px'}}>Photos</h4>
                    <div style={{display: 'flex', gap: '16px', alignItems: 'flex-start'}}>
                        {formData.imageUrl ? (
                            <div style={{position: 'relative', width: '120px', height: '120px'}}>
                                <img 
                                    src={formData.imageUrl} 
                                    alt="Preview" 
                                    style={{width: '100%', height: '100%', objectFit: 'cover', borderRadius: '4px', border: '1px solid #ddd'}} 
                                />
                                <button 
                                    onClick={() => setFormData({...formData, imageUrl: ""})}
                                    style={{
                                        position: 'absolute', top: '-8px', right: '-8px', 
                                        background: 'white', border: '1px solid #ccc', borderRadius: '50%', 
                                        width: '24px', height: '24px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center'
                                    }}
                                >
                                    ×
                                </button>
                            </div>
                        ) : (
                            <label style={{
                                width: '120px', height: '120px', border: '2px dashed #ccc', borderRadius: '4px', 
                                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
                                cursor: 'pointer', color: 'var(--primary)', background: '#fafafa'
                            }}>
                                <span style={{fontSize: '24px'}}>+</span>
                                <span style={{fontSize: '0.8rem', marginTop: '4px'}}>Add Photo</span>
                                <input type="file" accept="image/*" onChange={handleImageUpload} style={{display: 'none'}} />
                            </label>
                        )}
                        
                        <div style={{flex: 1}}>
                             <h3 style={{margin: '0 0 4px 0'}}>
                                {itemType === "Accessory" ? "New Accessory" : selectedGame?.name}
                             </h3>
                             <p style={{margin: 0, color: '#666', fontSize: '0.9rem'}}>
                                {getTypeIcon(itemType)} {itemType}
                             </p>
                             <p style={{fontSize: '0.8rem', color: '#888', marginTop: '8px'}}>
                                {formData.imageUrl ? "Photo added" : "Add at least one photo (Required)"}
                             </p>
                        </div>
                    </div>
                </div>

                <form onSubmit={handleSubmit}>
                    
                    {/* Manual Name Input for Accessories */}
                    {itemType === "Accessory" && (
                        <div className="form-group">
                            <label className="form-label">Item Name</label>
                            <input 
                                type="text"
                                className="form-input"
                                value={manualName}
                                onChange={(e) => setManualName(e.target.value)}
                                placeholder="e.g. PS5 DualSense Controller"
                                required
                            />
                        </div>
                    )}
                    
                    {/* Platform Selection for Games */}
                    {itemType === "Game" && (
                        <div className="form-group">
                            <label className="form-label">Platform</label>
                            <select 
                                className="form-select"
                                value={selectedPlatform}
                                onChange={(e) => setSelectedPlatform(e.target.value)}
                                required
                            >
                                <option value="">Select Platform</option>
                                {getPlatformOptions().map(p => (
                                    <option key={p} value={p}>{p}</option>
                                ))}
                            </select>
                        </div>
                    )}

                    <div className="form-group">
                        <label className="form-label">Description</label>
                        <textarea 
                            className="form-textarea"
                            value={formData.description}
                            onChange={(e) => setFormData({...formData, description: e.target.value})}
                            placeholder="Describe the condition, included items (cables, controllers), etc."
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Price per Day (€)</label>
                        <input 
                            type="number" 
                            step="0.50"
                            min="0"
                            className="form-input"
                            value={formData.pricePerDay}
                            onChange={(e) => setFormData({...formData, pricePerDay: e.target.value})}
                            placeholder="0.00"
                            required
                        />
                    </div>

                    <button type="submit" className="btn btn-primary" style={{width: '100%', marginTop: '10px'}}>Publish Listing</button>
                </form>
            </div>
        </div>
      )}
    </div>
  );
}
