import React, { useState } from "react";

export default function StarRating({ value = 0, onChange, disabled = false, size = 24, requireSelection = false }) {
  const [hovered, setHovered] = useState(null);
  const displayValue = hovered != null ? hovered : value;

  const handleClick = (n) => {
    if (disabled) return;
    onChange?.(n);
  };

  const starStyle = (n) => ({
    cursor: disabled ? "default" : "pointer",
    color: n <= displayValue ? "#f5a623" : "#ddd",
    fontSize: `${size}px`,
    transition: "color 0.15s ease",
    marginRight: "4px",
  });

  return (
    <div style={{ display: "inline-flex", alignItems: "center" }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span
          key={n}
          role="button"
          aria-label={`Rate ${n} star${n > 1 ? "s" : ""}`}
          style={starStyle(n)}
          onMouseEnter={() => !disabled && setHovered(n)}
          onMouseLeave={() => !disabled && setHovered(null)}
          onClick={() => handleClick(n)}
        >
          ★
        </span>
      ))}
    </div>
  );
}

