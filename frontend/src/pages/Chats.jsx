import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Chats.css';

export default function Chats() {
  const [chats, setChats] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchChats();
  }, []);

  const fetchChats = async () => {
    try {
      const response = await fetch('/api/chats', { credentials: 'include' });
      if (response.status === 401) {
        navigate('/auth');
        return;
      }
      if (response.ok) {
        const data = await response.json();
        setChats(data);
      }
    } catch (error) {
      console.error('Error fetching chats:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now - date) / (1000 * 60 * 60);
    
    if (diffInHours < 24) {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } else if (diffInHours < 48) {
      return 'Yesterday';
    } else {
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }
  };

  if (loading) {
    return <div className="chats-container"><p>Loading chats...</p></div>;
  }

  return (
    <div className="chats-container">
      <h1>Messages</h1>
      {chats.length === 0 ? (
        <div className="no-chats">
          <p>No conversations yet.</p>
          <p>Start chatting with item owners by clicking "Chat with Owner" on item pages.</p>
        </div>
      ) : (
        <div className="chats-list">
          {chats.map((chat) => (
            <div
              key={chat.id}
              className="chat-item"
              onClick={() => navigate(`/chats/${chat.id}`)}
            >
              <div className="chat-item-image">
                {chat.itemImageUrl ? (
                  <img src={chat.itemImageUrl} alt={chat.itemName} />
                ) : (
                  <div className="chat-item-placeholder">📦</div>
                )}
              </div>
              <div className="chat-item-details">
                <div className="chat-item-header">
                  <h3>{chat.itemName}</h3>
                  <span className="chat-timestamp">{formatTimestamp(chat.updatedAt)}</span>
                </div>
                <p className="chat-participant">
                  {localStorage.getItem('user') && JSON.parse(localStorage.getItem('user')).id === chat.ownerId
                    ? `Chatting with ${chat.renterName}`
                    : `Owner: ${chat.ownerName}`}
                </p>
                <p className="chat-last-message">
                  {chat.lastMessage || 'No messages yet'}
                </p>
                {chat.unreadCount > 0 && (
                  <span className="chat-unread-badge">{chat.unreadCount}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
