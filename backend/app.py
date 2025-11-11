# app.py
from flask import Flask, jsonify
import sqlite3

DB = "clubs.db"
app = Flask(__name__)

def rows_to_dicts(cur, rows):
    cols = [c[0] for c in cur.description]
    return [dict(zip(cols, r)) for r in rows]

@app.get("/api/clubs")
def get_clubs():
    con = sqlite3.connect(DB)
    cur = con.cursor()
    cur.execute("SELECT slug,name,email,phone,categories,summary,logo_url FROM clubs ORDER BY name;")
    data = rows_to_dicts(cur, cur.fetchall())
    con.close()
    return jsonify({"clubs": data})

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=True)
