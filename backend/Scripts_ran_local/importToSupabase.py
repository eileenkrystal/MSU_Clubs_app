#!/usr/bin/env python3
"""
import_to_supabase.py

Usage:
    python import_to_supabase.py path/to/clubs.csv
    python import_to_supabase.py clubs.csv --dry-run --batch-size 20

Notes:
- Expects CSV with columns like: id,name,slug,description,website,address,email,phone,emails,...
- Uses SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY from env if present; otherwise falls back to literals (not recommended).
"""
import csv
import os
import re
import sys
import json
import time
import uuid
import argparse
from typing import Dict, Optional

import requests
from email_validator import validate_email, EmailNotValidError

# -----------------------
# Config / env
# -----------------------
SUPABASE_URL = os.environ.get("SUPABASE_URL", "https://xsqggtnsjykswbizmfbu.supabase.co")
SERVICE_ROLE_KEY = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhzcWdndG5zanlrc3diaXptZmJ1Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MjgyMzIwNiwiZXhwIjoyMDc4Mzk5MjA2fQ.27yLUKQi2z6EdIqXMIWF6BuRzGYCzm5pq8ggC-u4F_I")

# small safety: prefer env var; if not present, ask user to confirm
if not SERVICE_ROLE_KEY:
    print("WARNING: SUPABASE_SERVICE_ROLE_KEY not set in environment.", file=sys.stderr)
    print("If you want to proceed without env vars, re-run with --force (not recommended).", file=sys.stderr)

REST_ENDPOINT = SUPABASE_URL.rstrip("/") + "/rest/v1/clubs"

HEADERS_TEMPLATE = {
    # filled later once we confirm the key
    "Content-Type": "application/json",
    "Prefer": "resolution=merge-duplicates"
}

# -----------------------
# Utilities
# -----------------------
EMAIL_RE = re.compile(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", re.I)
PHONE_RE = re.compile(r"[\d\+\-\s().]{7,}")  # permissive

def normalize_phone(p: Optional[str]) -> Optional[str]:
    if not p:
        return None
    p = p.strip()
    # Remove common words like 'Phone:' etc
    p = re.sub(r"[Pp]hone[:\s]*", "", p)
    p = re.sub(r"[^\d\+]", "", p)  # keep digits and plus
    if not p:
        return None
    return p

def choose_email(row: Dict[str,str]) -> Optional[str]:
    # prefer explicit 'email' column, else try first in 'emails' (semi-colon separated), else scan description/contacts
    e = (row.get("email") or "").strip()
    if e:
        try:
            return validate_email(e, check_deliverability=False).normalized
        except EmailNotValidError:
            pass
    # try 'emails' column with separators
    emails_raw = row.get("emails") or row.get("Emails") or ""
    if emails_raw:
        for candidate in re.split(r"[;,/]| and ", emails_raw):
            candidate = candidate.strip()
            if not candidate:
                continue
            try:
                return validate_email(candidate, check_deliverability=False).normalized
            except EmailNotValidError:
                continue
    # fallback: try to find an email anywhere in the row values
    for col_val in row.values():
        if not col_val:
            continue
        for m in EMAIL_RE.findall(str(col_val)):
            try:
                return validate_email(m, check_deliverability=False).normalized
            except EmailNotValidError:
                continue
    return None

def build_payload_from_row(row: Dict[str,str]) -> Dict:
    # use id if present and looks like uuid, else make one
    id_candidate = (row.get("id") or "").strip()
    try:
        if id_candidate:
            # if it's a 36-char uuid-like keep it, else generate
            if len(id_candidate) == 36:
                uuid.UUID(id_candidate)  # validate
                club_id = id_candidate
            else:
                club_id = str(uuid.uuid4())
        else:
            club_id = str(uuid.uuid4())
    except Exception:
        club_id = str(uuid.uuid4())

    slug = (row.get("slug") or row.get("Slug") or "").strip()
    name = (row.get("name") or row.get("Name") or "").strip()
    description = (row.get("description") or row.get("Description") or row.get("summary") or "").strip()
    website = (row.get("website") or row.get("Website") or "").strip() or None
    address = (row.get("address") or row.get("Address") or "").strip() or None

    email = choose_email(row)
    phone = normalize_phone((row.get("phone") or row.get("Phone") or "").strip())

    payload = {
        "id": club_id,
        "slug": slug,
        "name": name or slug,
        "description": description or None,
        "website": website,
        "address": address,
        "email": email,
        "phone": phone
    }
    # remove null / empty
    payload = {k: v for k, v in payload.items() if v is not None and v != ""}
    return payload

# -----------------------
# HTTP / upsert with retries
# -----------------------
def post_with_retries(url: str, headers: Dict[str,str], params: Dict[str,str], json_payload: Dict, max_retries: int = 4, backoff_base: float = 0.6):
    for attempt in range(1, max_retries+1):
        try:
            r = requests.post(url, headers=headers, params=params, json=json_payload, timeout=30)
        except requests.RequestException as e:
            # network error, retry
            if attempt == max_retries:
                return None, f"request-exception: {e}"
            time.sleep(backoff_base * (2 ** (attempt-1)))
            continue
        if r.status_code in (200, 201, 204):
            return r, None
        # 409/422 may indicate bad payload; treat as terminal
        if r.status_code in (400, 401, 403, 404, 409, 422):
            return r, f"status {r.status_code}: {r.text}"
        # other 5xx -> retry
        if 500 <= r.status_code < 600:
            if attempt == max_retries:
                return r, f"server error {r.status_code}: {r.text}"
            time.sleep(backoff_base * (2 ** (attempt-1)))
            continue
        # unknown status -> return
        return r, f"unexpected status {r.status_code}: {r.text}"
    return None, "retries exhausted"

# -----------------------
# Main
# -----------------------
def main():
    parser = argparse.ArgumentParser(description="Import clubs CSV to Supabase (PostgREST)")
    parser.add_argument("csv", nargs="?", default="clubs.csv")
    parser.add_argument("--dry-run", action="store_true", help="Don't POST to Supabase; print payloads")
    parser.add_argument("--batch-size", type=int, default=1, help="How many rows to POST before sleeping")
    parser.add_argument("--delay", type=float, default=0.12, help="Delay seconds between batches")
    parser.add_argument("--force", action="store_true", help="Allow running without SUPABASE_SERVICE_ROLE_KEY env var (not recommended)")
    args = parser.parse_args()

    if not args.force and not SERVICE_ROLE_KEY and not args.dry_run:
        print("Error: SUPABASE_SERVICE_ROLE_KEY is not set. Export it to env or run with --force to bypass. Exiting.", file=sys.stderr)
        sys.exit(2)

    headers = dict(HEADERS_TEMPLATE)
    if SERVICE_ROLE_KEY:
        headers["apikey"] = SERVICE_ROLE_KEY
        headers["Authorization"] = f"Bearer {SERVICE_ROLE_KEY}"
    else:
        # allow dry-run even without key
        headers["apikey"] = ""
        headers["Authorization"] = ""

    params = {"on_conflict": "slug"}  # upsert by slug

    csv_path = args.csv
    if not os.path.exists(csv_path):
        print("CSV file not found:", csv_path, file=sys.stderr)
        sys.exit(2)

    total = 0
    succeeded = 0
    failed = 0
    failures = []

    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    print(f"[INFO] Processing {len(rows)} rows from {csv_path}")

    for i, row in enumerate(rows, 1):
        if not (row.get("slug") or row.get("Slug")) or not (row.get("name") or row.get("Name")):
            print(f"[SKIP] Row {i} missing slug/name")
            continue

        payload = build_payload_from_row(row)
        total += 1

        if args.dry_run:
            print(json.dumps(payload, ensure_ascii=False))
            succeeded += 1
        else:
            r, err = post_with_retries(REST_ENDPOINT, headers, params, payload)
            if err is None:
                # success
                succeeded += 1
            else:
                failed += 1
                failures.append((i, payload.get("slug"), err, getattr(r, "status_code", None), getattr(r, "text", None)))
                print(f"[FAILED] row {i} slug={payload.get('slug')} -> {err}", file=sys.stderr)

        # batch delay
        if (i % args.batch_size) == 0:
            time.sleep(args.delay)

    print("----------")
    print(f"Total processed: {total}")
    print(f"Succeeded (sent or dry-run): {succeeded}")
    print(f"Failed: {failed}")
    if failures:
        print("\nFailures (first 10):")
        for fitem in failures[:10]:
            idx, slug, err, status, text = fitem
            print(f"  row {idx} slug={slug} -> {err} status={status}")
    print("Done.")

if __name__ == "__main__":
    main()
