import React from "react";

export default function ReviewCard({ name, rating, comment, date }) {
  const initial = name ? name.charAt(0).toUpperCase() : "?";
  const formattedDate = date ? new Date(date).toLocaleDateString() : "";

  return (
    <div style={{ display: "flex", gap: "12px", padding: "12px 0", borderBottom: "1px solid #eee" }}>
      <div
        style={{
          width: "48px",
          height: "48px",
          borderRadius: "50%",
          background: "var(--primary)",
          color: "white",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: 700,
          fontSize: "1.1rem",
          flexShrink: 0,
        }}
      >
        {initial}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div style={{ fontWeight: 700 }}>{name || "Member"}</div>
          <small style={{ color: "#888" }}>{formattedDate}</small>
        </div>
        <div style={{ color: "#f5a623", fontSize: "0.95rem", margin: "2px 0" }}>
          {"★".repeat(rating || 0)}{"☆".repeat(Math.max(0, 5 - (rating || 0)))}
          <span style={{ color: "#444", marginLeft: "6px" }}>{rating}/5</span>
        </div>
        <div style={{ color: "#333", lineHeight: 1.4 }}>{comment || "No comment provided."}</div>
      </div>
    </div>
  );
}

