# create_db.py
import csv, sqlite3, os

DB = "clubs.db"
CSV = "engineering_clubs_basic.csv"

schema = """
CREATE TABLE IF NOT EXISTS clubs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  slug TEXT UNIQUE,
  name TEXT NOT NULL,
  email TEXT,
  phone TEXT,
  categories TEXT,
  summary TEXT,
  logo_url TEXT,
  updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_clubs_name ON clubs(name);
"""

def upsert_row(cur, r):
    cur.execute("""
      INSERT INTO clubs(slug,name,email,phone,categories,summary,logo_url)
      VALUES(?,?,?,?,?,?,?)
      ON CONFLICT(slug) DO UPDATE SET
        email=excluded.email,
        phone=excluded.phone,
        categories=excluded.categories,
        summary=excluded.summary,
        logo_url=excluded.logo_url
    """, (
        r.get("slug",""), r.get("name",""), r.get("email",""),
        r.get("phone",""), r.get("categories",""), r.get("summary",""),
        r.get("logo_url","")
    ))

def main():
    if not os.path.exists(CSV):
        raise SystemExit(f"CSV not found: {CSV}")
    con = sqlite3.connect(DB)
    cur = con.cursor()
    cur.executescript(schema)
    with open(CSV, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            upsert_row(cur, row)
    con.commit()
    con.close()
    print(f"Loaded data into {DB}.")
if __name__ == "__main__":
    main()
