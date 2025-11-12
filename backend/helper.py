# create_user.py
import os, bcrypt, uuid, requests
from dotenv import load_dotenv
load_dotenv()
SUPABASE_URL = os.getenv("SUPABASE_URL")
SRK = os.getenv("SUPABASE_SERVICE_ROLE_KEY")
headers = {"apikey": SRK, "Authorization": f"Bearer {SRK}", "Content-Type":"application/json"}
email = "test@msu.edu"
pw = "password123"
pw_hash = bcrypt.hashpw(pw.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
payload = {"id": str(uuid.uuid4()), "email": email, "password_hash": pw_hash}
r = requests.post(f"{SUPABASE_URL.rstrip('/')}/rest/v1/users", headers=headers, json=payload)
print(r.status_code, r.text)
