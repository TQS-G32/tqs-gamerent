import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import ReviewCard from "../components/ReviewCard.jsx";

export default function UserProfile() {
  const { id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get("tab") === "reviews" ? "reviews" : "listings";

  const [profile, setProfile] = useState(null);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [reviews, setReviews] = useState([]);
  const observerRef = useRef(null);

  useEffect(() => {
    fetch(`/api/users/${id}/profile`)
      .then((res) => res.json())
      .then(setProfile)
      .catch(() => {});
  }, [id]);

  useEffect(() => {
    // reset items when user changes
    setItems([]);
    setPage(0);
    setTotalPages(1);
  }, [id]);

  useEffect(() => {
    if (activeTab !== "listings") return;
    if (loading) return;
    if (page >= totalPages) return;
    setLoading(true);
    fetch(`/api/items?ownerId=${id}&page=${page}&pageSize=20`)
      .then((res) => res.json())
      .then((data) => {
        const newItems = Array.isArray(data.items) ? data.items : [];
        setItems((prev) => [...prev, ...newItems]);
        setTotalPages(Number.isFinite(data.totalPages) ? data.totalPages : 1);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id, page, activeTab]);

  useEffect(() => {
    fetch(`/api/reviews/user/${id}`)
      .then((res) => res.json())
      .then((data) => setReviews(Array.isArray(data) ? data : []))
      .catch(() => {});
  }, [id]);

  const initial = useMemo(() => {
    if (profile?.name) return profile.name.charAt(0).toUpperCase();
    return "?";
  }, [profile]);

  const switchTab = (tab) => {
    setSearchParams({ tab });
  };

  useEffect(() => {
    if (activeTab !== "listings") return;
    const sentinel = observerRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver(
      (entries) => {
        const first = entries[0];
        if (first.isIntersecting && !loading && page + 1 < totalPages) {
          setPage((p) => p + 1);
        }
      },
      { rootMargin: "200px" }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [activeTab, loading, page, totalPages]);

  return (
    <div className="container" style={{ paddingTop: "24px" }}>
      <div className="sidebar-card" style={{ display: "flex", gap: "16px", alignItems: "center" }}>
        <div
          style={{
            width: "72px",
            height: "72px",
            borderRadius: "50%",
            background: "var(--primary)",
            color: "white",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "1.6rem",
            fontWeight: 800,
          }}
        >
          {initial}
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 700, fontSize: "1.2rem" }}>{profile?.name || "Member"}</div>
          <div style={{ display: "flex", gap: "10px", color: "#666", fontSize: "0.95rem", alignItems: "center", flexWrap: "wrap" }}>
            <span>Listings: {profile?.itemsCount ?? 0}</span>
            <span>Reviews: {profile?.reviewCount ?? 0}</span>
            <span style={{ display: "flex", alignItems: "center", gap: "4px", color: "#f5a623", fontWeight: 700 }}>
              ★ {profile?.averageRating?.toFixed ? profile.averageRating.toFixed(1) : "0.0"}
            </span>
          </div>
        </div>
      </div>

      <div className="tabs" style={{ display: "flex", gap: "12px", marginTop: "16px", marginBottom: "12px" }}>
        <div
          className={`tab ${activeTab === "listings" ? "active" : ""}`}
          onClick={() => switchTab("listings")}
          style={{
            padding: "10px 14px",
            borderRadius: "6px",
            border: activeTab === "listings" ? "2px solid var(--primary)" : "1px solid #ddd",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          Listings
        </div>
        <div
          className={`tab ${activeTab === "reviews" ? "active" : ""}`}
          onClick={() => switchTab("reviews")}
          style={{
            padding: "10px 14px",
            borderRadius: "6px",
            border: activeTab === "reviews" ? "2px solid var(--primary)" : "1px solid #ddd",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          Reviews
        </div>
      </div>

      {activeTab === "listings" && (
        <div className="sidebar-card">
          {items.length === 0 ? (
            <div style={{ color: "#777" }}>No listings.</div>
          ) : (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: "12px" }}>
              {items.map((item) => (
                <Link
                  to={`/item/${item.id}`}
                  key={item.id}
                  className="sidebar-card"
                  style={{ textDecoration: "none", color: "inherit", padding: "12px" }}
                >
                  <img
                    src={item.imageUrl || "https://via.placeholder.com/300x300?text=No+Image"}
                    alt={item.name}
                    style={{ width: "100%", height: "160px", objectFit: "cover", borderRadius: "6px", marginBottom: "8px" }}
                  />
                  <div style={{ fontWeight: 700 }}>{item.name}</div>
                  <div style={{ color: "#666", fontSize: "0.9rem" }}>{item.category || "Gaming"}</div>
                  {item.pricePerDay != null && (
                    <div style={{ color: "var(--primary)", fontWeight: 700, marginTop: "4px" }}>€{item.pricePerDay.toFixed(2)}/day</div>
                  )}
                </Link>
              ))}
              <div ref={observerRef} style={{ height: "1px" }} />
            </div>
          )}
          {loading && <div style={{ marginTop: "8px", color: "#666" }}>Loading...</div>}
          {!loading && page + 1 >= totalPages && <div style={{ marginTop: "8px", color: "#999" }}>End of listings.</div>}
        </div>
      )}

      {activeTab === "reviews" && (
        <div className="sidebar-card">
          {reviews.length === 0 ? (
            <div style={{ color: "#777" }}>Without any reviews.</div>
          ) : (
            reviews.map((r, idx) => (
              <ReviewCard
                key={r.id || idx}
                name={r.reviewerName}
                rating={r.rating}
                comment={r.comment}
                date={r.createdAt}
              />
            ))
          )}
        </div>
      )}
    </div>
  );
}
