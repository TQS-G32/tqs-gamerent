import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

export default function Disputes() {
  const [disputes, setDisputes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bookingsMap, setBookingsMap] = useState({});
  const [itemsMap, setItemsMap] = useState({});
  const [selectedDispute, setSelectedDispute] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [bookings, setBookings] = useState([]);

  const userJson = window.localStorage.getItem('user');
  const currentUser = userJson ? JSON.parse(userJson) : null;

  // Fetch all disputes for current user
  useEffect(() => {
    fetchDisputes();
    fetchBookings();
  }, []);

  const fetchDisputes = async () => {
    try {
      const res = await fetch('/api/disputes/my-disputes', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        setDisputes(data);
        
        // Fetch related bookings and items
        const bookingIds = [...new Set(data.map(d => d.bookingId))];
        await fetchBookingsAndItems(bookingIds);
      } else {
        setError('Failed to load disputes');
      }
    } catch (err) {
      console.error('Error fetching disputes:', err);
      setError('Error loading disputes');
    } finally {
      setLoading(false);
    }
  };

  const fetchBookings = async () => {
    try {
      const res = await fetch('/api/bookings/my-bookings', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        setBookings(data.filter(b => {
          const endDate = new Date(b.endDate);
          const today = new Date();
          const thirtyDaysAgo = new Date(today.setDate(today.getDate() - 30));
          return b.status === 'APPROVED' && endDate >= thirtyDaysAgo;
        }));
      }
    } catch (err) {
      console.error('Error fetching bookings:', err);
    }
  };

  const fetchBookingsAndItems = async (bookingIds) => {
    const bookingsTemp = {};
    const itemsTemp = {};

    for (const bookingId of bookingIds) {
      try {
        const bookingRes = await fetch(`/api/bookings/${bookingId}`, {
          credentials: 'include'
        });
        if (bookingRes.ok) {
          const booking = await bookingRes.json();
          bookingsTemp[bookingId] = booking;

          // Fetch item details
          if (booking.itemId && !itemsTemp[booking.itemId]) {
            const itemRes = await fetch(`/api/items/${booking.itemId}`);
            if (itemRes.ok) {
              const item = await itemRes.json();
              itemsTemp[booking.itemId] = item;
            }
          }
        }
      } catch (err) {
        console.error('Error fetching booking/item:', err);
      }
    }

    setBookingsMap(bookingsTemp);
    setItemsMap(itemsTemp);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString();
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString();
  };

  const getStatusBadge = (status) => {
    const styles = {
      SUBMITTED: { bg: '#3b82f6', color: 'white' },
      UNDER_REVIEW: { bg: '#f59e0b', color: 'white' },
      RESOLVED: { bg: '#10b981', color: 'white' },
      REJECTED: { bg: '#ef4444', color: 'white' }
    };
    const style = styles[status] || { bg: '#6b7280', color: 'white' };
    return (
      <span style={{
        padding: '4px 12px',
        borderRadius: '12px',
        fontSize: '0.85rem',
        fontWeight: '600',
        backgroundColor: style.bg,
        color: style.color
      }}>
        {status.replace('_', ' ')}
      </span>
    );
  };

  const getReasonLabel = (reason) => {
    const labels = {
      DAMAGED_ITEM: 'Damaged Item',
      NO_SHOW: 'No Show',
      LATE_RETURN: 'Late Return',
      INCORRECT_LISTING: 'Incorrect Listing',
      PAYMENT_ISSUE: 'Payment Issue',
      OTHER: 'Other'
    };
    return labels[reason] || reason;
  };

  if (loading) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Loading disputes...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#ef4444' }}>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Disputes & Issues</h1>
        <button 
          className="btn btn-primary"
          onClick={() => setShowCreateModal(true)}
        >
          Report New Issue
        </button>
      </div>

      {disputes.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '3rem', background: '#f9fafb', borderRadius: '8px' }}>
          <p style={{ fontSize: '1.1rem', color: '#6b7280' }}>No disputes found</p>
          <p style={{ color: '#9ca3af', marginTop: '0.5rem' }}>
            You can report issues for active or recently completed bookings
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: '1rem' }}>
          {disputes.map(dispute => {
            const booking = bookingsMap[dispute.bookingId];
            const item = booking ? itemsMap[booking.itemId] : null;

            return (
              <div 
                key={dispute.id}
                style={{
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  padding: '1.5rem',
                  background: 'white',
                  cursor: 'pointer',
                  transition: 'box-shadow 0.2s',
                }}
                onMouseEnter={(e) => e.currentTarget.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)'}
                onMouseLeave={(e) => e.currentTarget.style.boxShadow = 'none'}
                onClick={() => setSelectedDispute(dispute)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1rem' }}>
                  <div>
                    <h3 style={{ margin: '0 0 0.5rem 0' }}>
                      Dispute #{dispute.id} - {getReasonLabel(dispute.reason)}
                    </h3>
                    {item && (
                      <p style={{ color: '#6b7280', margin: 0 }}>
                        Related to: <strong>{item.name}</strong>
                      </p>
                    )}
                  </div>
                  {getStatusBadge(dispute.status)}
                </div>

                {dispute.description && (
                  <p style={{ color: '#374151', marginBottom: '1rem' }}>
                    {dispute.description.substring(0, 150)}
                    {dispute.description.length > 150 ? '...' : ''}
                  </p>
                )}

                <div style={{ display: 'flex', gap: '2rem', fontSize: '0.9rem', color: '#6b7280' }}>
                  <span>Created: {formatDate(dispute.createdAt)}</span>
                  {dispute.updatedAt && (
                    <span>Updated: {formatDate(dispute.updatedAt)}</span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Dispute Details Modal */}
      {selectedDispute && (
        <DisputeDetailsModal
          dispute={selectedDispute}
          booking={bookingsMap[selectedDispute.bookingId]}
          item={bookingsMap[selectedDispute.bookingId] ? itemsMap[bookingsMap[selectedDispute.bookingId].itemId] : null}
          currentUser={currentUser}
          onClose={() => setSelectedDispute(null)}
          onUpdate={fetchDisputes}
          formatDateTime={formatDateTime}
          getStatusBadge={getStatusBadge}
          getReasonLabel={getReasonLabel}
        />
      )}

      {/* Create Dispute Modal */}
      {showCreateModal && (
        <CreateDisputeModal
          bookings={bookings}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            fetchDisputes();
          }}
        />
      )}
    </div>
  );
}

function DisputeDetailsModal({ dispute, booking, item, currentUser, onClose, onUpdate, formatDateTime, getStatusBadge, getReasonLabel }) {
  const [adminNotes, setAdminNotes] = useState('');
  const [newStatus, setNewStatus] = useState(dispute.status);
  const [updating, setUpdating] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const isAdmin = currentUser?.role === 'ADMIN';

  const handleStatusUpdate = async () => {
    setUpdating(true);
    setSuccessMessage('');
    setErrorMessage('');
    try {
      const res = await fetch(`/api/disputes/${dispute.id}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          status: newStatus,
          adminNotes: adminNotes || undefined
        })
      });

      if (res.ok) {
        setSuccessMessage('Dispute status updated successfully');
        setTimeout(() => {
          onUpdate();
          onClose();
        }, 1500);
      } else {
        const error = await res.text();
        setErrorMessage('Failed to update dispute: ' + error);
      }
    } catch (err) {
      console.error('Error updating dispute:', err);
      setErrorMessage('Error updating dispute');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '1rem'
    }}>
      <div style={{
        background: 'white',
        borderRadius: '8px',
        maxWidth: '800px',
        width: '100%',
        maxHeight: '90vh',
        overflow: 'auto',
        padding: '2rem'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2 style={{ margin: 0 }}>Dispute #{dispute.id}</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
        </div>

        <div style={{ display: 'grid', gap: '1.5rem' }}>
          <div>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Status</label>
            {getStatusBadge(dispute.status)}
          </div>

          <div>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Reason</label>
            <p style={{ margin: 0 }}>{getReasonLabel(dispute.reason)}</p>
          </div>

          {dispute.description && (
            <div>
              <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Description</label>
              <p style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{dispute.description}</p>
            </div>
          )}

          {item && (
            <div>
              <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Related Item</label>
              <Link to={`/item/${item.id}`} style={{ color: '#3b82f6', textDecoration: 'underline' }}>
                {item.name}
              </Link>
            </div>
          )}

          {booking && (
            <div>
              <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Booking Details</label>
              <p style={{ margin: 0 }}>
                Period: {new Date(booking.startDate).toLocaleDateString()} - {new Date(booking.endDate).toLocaleDateString()}
              </p>
            </div>
          )}

          <div>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Timestamps</label>
            <p style={{ margin: 0 }}>Created: {formatDateTime(dispute.createdAt)}</p>
            {dispute.updatedAt && <p style={{ margin: 0 }}>Updated: {formatDateTime(dispute.updatedAt)}</p>}
          </div>

          {dispute.adminNotes && (
            <div>
              <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Admin Notes</label>
              <p style={{ margin: 0, background: '#f9fafb', padding: '1rem', borderRadius: '4px', whiteSpace: 'pre-wrap' }}>
                {dispute.adminNotes}
              </p>
            </div>
          )}

          {successMessage && (
            <div style={{ padding: '0.75rem', background: '#d1fae5', color: '#065f46', borderRadius: '4px', marginBottom: '1rem' }}>
              ✓ {successMessage}
            </div>
          )}

          {errorMessage && (
            <div style={{ padding: '0.75rem', background: '#fee2e2', color: '#dc2626', borderRadius: '4px', marginBottom: '1rem' }}>
              ✗ {errorMessage}
            </div>
          )}

          {isAdmin && (
            <div style={{ borderTop: '2px solid #e5e7eb', paddingTop: '1.5rem', marginTop: '1rem' }}>
              <h3>Admin Actions</h3>
              
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Update Status</label>
                <select 
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #d1d5db' }}
                >
                  <option value="SUBMITTED">Submitted</option>
                  <option value="UNDER_REVIEW">Under Review</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Admin Notes</label>
                <textarea
                  value={adminNotes}
                  onChange={(e) => setAdminNotes(e.target.value)}
                  placeholder="Add notes about the resolution..."
                  rows={4}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #d1d5db' }}
                />
              </div>

              <button
                onClick={handleStatusUpdate}
                disabled={updating}
                className="btn btn-primary"
                style={{ width: '100%' }}
              >
                {updating ? 'Updating...' : 'Update Dispute'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function CreateDisputeModal({ bookings, onClose, onSuccess }) {
  const [selectedBooking, setSelectedBooking] = useState('');
  const [reason, setReason] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const reasons = [
    { value: 'DAMAGED_ITEM', label: 'Damaged Item' },
    { value: 'NO_SHOW', label: 'No Show' },
    { value: 'LATE_RETURN', label: 'Late Return' },
    { value: 'INCORRECT_LISTING', label: 'Incorrect Listing' },
    { value: 'PAYMENT_ISSUE', label: 'Payment Issue' },
    { value: 'OTHER', label: 'Other' }
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!selectedBooking) {
      setError('Please select a booking');
      return;
    }

    if (!reason) {
      setError('Please select a reason');
      return;
    }

    if (reason === 'OTHER' && !description.trim()) {
      setError('Description is required for "Other" reason');
      return;
    }

    if (description.length > 500) {
      setError('Description cannot exceed 500 characters');
      return;
    }

    setSubmitting(true);

    try {
      const res = await fetch('/api/disputes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          bookingId: parseInt(selectedBooking),
          reason: reason,
          description: description || undefined
        })
      });

      if (res.ok) {
        setSuccessMessage('Dispute submitted successfully. An admin will review it shortly.');
        setTimeout(() => {
          onSuccess();
        }, 1500);
      } else {
        const errorText = await res.text();
        setError(errorText || 'Failed to submit dispute');
      }
    } catch (err) {
      console.error('Error submitting dispute:', err);
      setError('Error submitting dispute');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '1rem'
    }}>
      <div style={{
        background: 'white',
        borderRadius: '8px',
        maxWidth: '600px',
        width: '100%',
        padding: '2rem'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2 style={{ margin: 0 }}>Report an Issue</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Select Booking *</label>
            <select
              value={selectedBooking}
              onChange={(e) => setSelectedBooking(e.target.value)}
              required
              style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #d1d5db' }}
            >
              <option value="">-- Select a booking --</option>
              {bookings.map(booking => (
                <option key={booking.id} value={booking.id}>
                  Booking #{booking.id} - {new Date(booking.startDate).toLocaleDateString()} to {new Date(booking.endDate).toLocaleDateString()}
                </option>
              ))}
            </select>
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>Reason *</label>
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
              style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #d1d5db' }}
            >
              <option value="">-- Select a reason --</option>
              {reasons.map(r => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>
              Description {reason === 'OTHER' ? '*' : '(optional)'}
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide details about the issue..."
              rows={4}
              maxLength={500}
              required={reason === 'OTHER'}
              style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid #d1d5db' }}
            />
            <div style={{ textAlign: 'right', fontSize: '0.85rem', color: '#6b7280', marginTop: '0.25rem' }}>
              {description.length}/500
            </div>
          </div>

          {error && (
            <div style={{ padding: '0.75rem', background: '#fee2e2', color: '#dc2626', borderRadius: '4px', marginBottom: '1rem' }}>
              ✗ {error}
            </div>
          )}

          {successMessage && (
            <div style={{ padding: '0.75rem', background: '#d1fae5', color: '#065f46', borderRadius: '4px', marginBottom: '1rem' }}>
              ✓ {successMessage}
            </div>
          )}

          <div style={{ display: 'flex', gap: '1rem' }}>
            <button
              type="button"
              onClick={onClose}
              className="btn"
              style={{ flex: 1 }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary"
              style={{ flex: 1 }}
            >
              {submitting ? 'Submitting...' : 'Submit Dispute'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
