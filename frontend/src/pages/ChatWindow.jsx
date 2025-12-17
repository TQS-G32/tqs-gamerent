import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './ChatWindow.css';

export default function ChatWindow() {
  const { chatId } = useParams();
  const [chat, setChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef(null);
  const navigate = useNavigate();

  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    fetchChatDetails();
    fetchMessages();
    // Mark messages as read when opening chat
    markAsRead();
  }, [chatId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchChatDetails = async () => {
    try {
      const response = await fetch(`/api/chats/${chatId}`, { credentials: 'include' });
      if (response.status === 401) {
        navigate('/auth');
        return;
      }
      if (response.status === 403) {
        alert('You do not have access to this chat');
        navigate('/chats');
        return;
      }
      if (response.ok) {
        const data = await response.json();
        setChat(data);
      }
    } catch (error) {
      console.error('Error fetching chat:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchMessages = async () => {
    try {
      const response = await fetch(`/api/chats/${chatId}/messages`, { credentials: 'include' });
      if (response.ok) {
        const data = await response.json();
        setMessages(data);
      }
    } catch (error) {
      console.error('Error fetching messages:', error);
    }
  };

  const markAsRead = async () => {
    try {
      await fetch(`/api/chats/${chatId}/read`, {
        method: 'PUT',
        credentials: 'include'
      });
    } catch (error) {
      console.error('Error marking messages as read:', error);
    }
  };

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || sending) return;

    setSending(true);
    try {
      const response = await fetch(`/api/chats/${chatId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ content: newMessage.trim() })
      });

      if (response.ok) {
        const sentMessage = await response.json();
        setMessages([...messages, sentMessage]);
        setNewMessage('');
      } else {
        const error = await response.text();
        alert(error || 'Failed to send message');
      }
    } catch (error) {
      console.error('Error sending message:', error);
      alert('Failed to send message');
    } finally {
      setSending(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return <div className="chat-window-container"><p>Loading chat...</p></div>;
  }

  if (!chat) {
    return <div className="chat-window-container"><p>Chat not found</p></div>;
  }

  const otherUser = currentUser.id === chat.ownerId
    ? { name: chat.renterName, email: chat.renterEmail }
    : { name: chat.ownerName, email: chat.ownerEmail };

  return (
    <div className="chat-window-container">
      <div className="chat-header">
        <button onClick={() => navigate('/chats')} className="back-button">
          ← Back
        </button>
        <div className="chat-header-info">
          {chat.itemImageUrl && (
            <img src={chat.itemImageUrl} alt={chat.itemName} className="chat-item-image" />
          )}
          <div>
            <h2>{chat.itemName}</h2>
            <p>Chatting with {otherUser.name}</p>
          </div>
        </div>
        <button onClick={() => navigate(`/item/${chat.itemId}`)} className="view-item-button">
          View Item
        </button>
      </div>

      <div className="messages-container">
        {messages.length === 0 ? (
          <p className="no-messages">No messages yet. Start the conversation!</p>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`message ${msg.senderId === currentUser.id ? 'message-own' : 'message-other'}`}
            >
              <div className="message-header">
                <span className="message-sender">{msg.senderName}</span>
                <span className="message-time">{formatTimestamp(msg.sentAt)}</span>
              </div>
              <div className="message-content">{msg.content}</div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={sendMessage} className="message-input-form">
        <div className="input-container input-style">
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            placeholder="Type a message"
            maxLength={2000}
            disabled={sending}
            className="message-input"
            autoComplete="off"
          />
          <button type="submit" disabled={!newMessage.trim() || sending} className="send-button">
            {sending ? (
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" fill="none"/>
                <path d="M12 6v6l4 2" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            ) : (
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" fill="currentColor"/>
              </svg>
            )}
          </button>
        </div>
        {newMessage.trim() && (
          <div className="char-counter">{newMessage.length}/2000</div>
        )}
      </form>
    </div>
  );
}
