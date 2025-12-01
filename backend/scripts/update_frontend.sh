#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$BACKEND_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_STATIC_DIR="$BACKEND_DIR/src/main/resources/static"

if [ ! -d "$FRONTEND_DIR" ]; then
  echo "Frontend directory not found: $FRONTEND_DIR" >&2
  exit 1
fi

cd "$FRONTEND_DIR"

echo "Building frontend..."
npm install
npm run build

BUILD_DIR="$FRONTEND_DIR/dist"
if [ ! -d "$BUILD_DIR" ]; then
  echo "Build directory not found: $BUILD_DIR" >&2
  exit 1
fi

mkdir -p "$BACKEND_STATIC_DIR"

rm -rf "$BACKEND_STATIC_DIR"/*

cp -a "$BUILD_DIR/." "$BACKEND_STATIC_DIR/"

rm -rf "$BUILD_DIR"

echo "Frontend update complete."
