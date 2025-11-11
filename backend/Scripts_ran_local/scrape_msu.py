# scrape_msu.py
"""
Scrapes MSU Engage (msu.campuslabs.com/engage) for registered student organizations.
Outputs clubs.jsonl and clubs.csv containing fields suitable for importing into the SQL schema.
"""
import argparse
import asyncio
import json
import os
import re
import sys
import uuid
from dataclasses import dataclass
from typing import List, Dict, Optional, Set, Tuple

import pandas as pd
from bs4 import BeautifulSoup
from email_validator import validate_email, EmailNotValidError

from playwright.async_api import async_playwright, TimeoutError as PlaywrightTimeoutError

# URL to the org directory listing page
ORG_LIST_URL = "https://msu.campuslabs.com/engage/organizations"
ORG_PAGE_PREFIX = "https://msu.campuslabs.com/engage/organization/"

# Regex to match any email-like string
EMAIL_RE = re.compile(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", re.I)

# Very permissive phone regex (US-style + international-ish)
PHONE_RE = re.compile(r"(\+?\d[\d\-\s().]{6,}\d)")

# What counts as social media (classifies based on URL pattern)
SOCIAL_KEYS = {
    "instagram": re.compile(r"(instagram\.com|^ig:)", re.I),
    "facebook": re.compile(r"(facebook\.com|fb\.me|^fb:)", re.I),
    "twitter": re.compile(r"(x\.com|twitter\.com|^tw:)", re.I),
    "linkedin": re.compile(r"(linkedin\.com|^li:)", re.I),
    "website": re.compile(r".", re.I),  # fallback catch-all for any website
}

@dataclass
class Club:
    id: str
    name: str
    slug: str
    url: str
    description: str
    emails: List[str]
    email: Optional[str]           # primary email (first)
    phone: Optional[str]
    address: Optional[str]
    contacts_raw: Optional[str]
    categories: List[str]
    socials: Dict[str, Optional[str]]

# --------------------------------------------------
# Utility functions
# --------------------------------------------------

def clean_text(s: Optional[str]) -> str:
    """Normalize whitespace and strip newlines/spaces."""
    if not s:
        return ""
    return re.sub(r"\s+", " ", s).strip()

def unique_emails(text: str) -> List[str]:
    """Extract unique, validated emails from arbitrary text."""
    found = set()
    for m in EMAIL_RE.findall(text or ""):
        try:
            v = validate_email(m, check_deliverability=False)
            found.add(v.normalized)
        except EmailNotValidError:
            continue
    return sorted(found)

def extract_first_phone(text: str) -> Optional[str]:
    """Return the first plausible phone number found, or None."""
    if not text:
        return None
    m = PHONE_RE.search(text)
    if m:
        ph = re.sub(r"\s+", " ", m.group(1)).strip()
        return ph
    return None

def classify_social(url_or_text: str) -> str:
    """Classify a URL into a social media platform (Instagram, Facebook, etc.)"""
    u = (url_or_text or "").strip()
    for key, pat in SOCIAL_KEYS.items():
        if pat.search(u):
            return key
    return "website"

def absolutize(href: str) -> str:
    """Convert relative '/engage/...' links into full URLs."""
    if not href:
        return ""
    href = href.strip()
    if href.startswith("http"):
        return href
    return f"https://msu.campuslabs.com{href}"

# --------------------------------------------------
# Playwright logic (scroll & extract)
# --------------------------------------------------

async def scroll_to_load_all(page, pause_ms: int = 600, max_steps: int = 40):
    last_height = 0
    for _ in range(max_steps):
        await page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        await page.wait_for_timeout(pause_ms)
        try:
            height = await page.evaluate("document.body.scrollHeight")
        except Exception:
            break
        if height == last_height:
            break
        last_height = height

async def extract_org_links_from_directory(html: str) -> List[str]:
    soup = BeautifulSoup(html, "html.parser")
    links = set()
    for a in soup.select("a[href*='/engage/organization/']"):
        href = a.get("href", "")
        if "/engage/organization/" in href:
            links.add(absolutize(href.split("?")[0]))
    return sorted(links)

# --------------------------------------------------
# Org page parsing (BeautifulSoup)
# --------------------------------------------------

def parse_org_page(html: str, url: str) -> Club:
    soup = BeautifulSoup(html, "html.parser")

    # Organization name (header <h1> or fallback from page <title>)
    name = ""
    h1 = soup.find(["h1", "h2"], string=True)
    if h1:
        name = clean_text(h1.get_text())

    if not name and soup.title:
        name = clean_text(soup.title.get_text()).replace(" - ", " ").split("|")[0]

    # Slug (URL last part)
    slug = url.rstrip("/").split("/")[-1]

    # Helper to extract a section following a header ("About", "Contact", etc.)
    def section_text(header_keywords: List[str]) -> Optional[str]:
        for hdr in soup.find_all(["h2", "h3", "h4"], string=True):
            if any(k.lower() in hdr.get_text(strip=True).lower() for k in header_keywords):
                texts = []
                node = hdr.find_next_sibling()
                while node and node.name not in ["h2", "h3", "h4"]:
                    texts.append(node.get_text(separator=" ", strip=True))
                    node = node.find_next_sibling()
                return clean_text(" ".join(t for t in texts if t))
        return None

    description = section_text(["about", "mission", "purpose"]) or ""
    contacts_raw = section_text(["contact", "contact information", "officers", "leadership"]) or ""

    # Address heuristic (from contacts block)
    address = None
    ADDRESS_HINTS = [
        "address", "msu", "east lansing", "road", "rd", "drive", "dr", "ave", "avenue", "street", "st"
    ]
    if contacts_raw:
        lines = [l.strip() for l in re.split(r"[;\n]", contacts_raw) if l.strip()]
        for l in lines:
            if any(h in l.lower() for h in ADDRESS_HINTS) and not EMAIL_RE.search(l):
                address = l
                break

    # Emails (all)
    emails = unique_emails((contacts_raw or "") + " " + description + " " + name)
    primary_email = emails[0] if emails else None

    # Phone (try contacts block, fallback to scanning whole page)
    phone = extract_first_phone(contacts_raw)
    if not phone:
        phone = extract_first_phone(soup.get_text(" "))

    # Categories / tags
    categories = []
    for el in soup.select("a, span"):
        txt = clean_text(el.get_text())
        if txt and len(txt) <= 40 and any(
            k in txt.lower()
            for k in [
                "academic", "professional", "culture", "service", "sports", "greek", "arts",
                "media", "tech", "engineering", "business", "health", "medical", "education",
                "volunteer", "religious", "political", "recreation", "gaming", "esports",
            ]
        ):
            categories.append(txt)
    categories = sorted(set(categories))

    # Social links; also capture a fallback website from anchors that appear likely to be official site
    socials: Dict[str, Optional[str]] = {k: None for k in SOCIAL_KEYS.keys()}
    website_guess = None
    for a in soup.select("a[href]"):
        href = a.get("href", "").strip()
        if not href:
            continue
        href_abs = absolutize(href)
        key = classify_social(href_abs)
        if key in socials and socials[key] is None:
            socials[key] = href_abs
        if (not website_guess) and ("website" in a.get_text("").lower() or ("http" in href_abs and "campuslabs.com" not in href_abs)):
            website_guess = href_abs

    # prefer socials['website'] if found, else website_guess
    website = socials.get("website") or website_guess

    # generate id as UUID4 string
    club_id = str(uuid.uuid4())

    return Club(
        id=club_id,
        name=name or slug,
        slug=slug,
        url=url,
        description=description or "",
        emails=emails,
        email=primary_email,
        phone=phone,
        address=address,
        contacts_raw=contacts_raw,
        categories=categories,
        socials=socials,
    )

# --------------------------------------------------
# Fetch logic (directory + org pages)
# --------------------------------------------------

async def fetch_directory_org_links(browser, headless: bool, user_agent: str, directory_timeout: int = 30_000) -> List[str]:
    page = await browser.new_page(user_agent=user_agent)
    try:
        await page.goto(ORG_LIST_URL, wait_until="domcontentloaded", timeout=directory_timeout)
    except PlaywrightTimeoutError:
        try:
            await page.goto(ORG_LIST_URL, wait_until="load", timeout=60_000)
        except Exception as e:
            print(f"[ERROR] Directory load failed: {e}", file=sys.stderr)
            await page.close()
            return []

    # Try repeated "Load more" / "Show more" clicks (common pattern)
    for _ in range(40):
        try:
            # selectors to try
            candidates = [
                "button:has-text('Load more')",
                "button:has-text('Load More')",
                "button:has-text('Show more')",
                "button:has-text('Show More')",
                "button.load-more",
                ".load-more button",
                "button[class*='load']",
                "a:has-text('Load more')",
            ]
            clicked = False
            for sel in candidates:
                loc = page.locator(sel)
                if await loc.count() > 0:
                    await loc.first.scroll_into_view_if_needed()
                    try:
                        await loc.first.click(force=True)
                        clicked = True
                        await page.wait_for_timeout(600)
                        break
                    except Exception:
                        # ignore one click failure and try other selectors
                        continue
            if not clicked:
                break
        except Exception:
            break

    # robust scroll (in case of lazy-loading)
    await scroll_to_load_all(page, pause_ms=800, max_steps=120)

    # short wait for XHRs
    try:
        await page.wait_for_load_state("networkidle", timeout=5000)
    except Exception:
        pass

    html = await page.content()
    links = await extract_org_links_from_directory(html)

    # fallback: try to pull any fully-qualified organization links from page text
    if len(links) <= 10:
        text = await page.content()
        for match in re.finditer(r"https?://[^\"'>\s]*?/engage/organization/[-\w\d%._]+", text):
            links.append(match.group(0))
        links = sorted(set(links))

    await page.close()
    return links

async def fetch_org_page_and_parse(browser, org_url: str, user_agent: str, delay_ms: int = 800, goto_timeout: int = 30_000) -> Optional[Club]:
    page = await browser.new_page(user_agent=user_agent)
    try:
        await page.goto(org_url, wait_until="domcontentloaded", timeout=goto_timeout)
        await page.wait_for_timeout(delay_ms)
        html = await page.content()
        club = parse_org_page(html, org_url)
        return club
    except PlaywrightTimeoutError:
        print(f"[WARN] Timeout loading {org_url}", file=sys.stderr)
        return None
    except Exception as e:
        print(f"[WARN] Failed {org_url}: {e}", file=sys.stderr)
        return None
    finally:
        try:
            await page.close()
        except Exception:
            pass

# --------------------------------------------------
# Saving results
# --------------------------------------------------

def save_jsonl_csv(records: List[Club], out_dir: str):
    os.makedirs(out_dir, exist_ok=True)
    jsonl_path = os.path.join(out_dir, "clubs.jsonl")
    csv_path = os.path.join(out_dir, "clubs.csv")

    # JSON Lines format
    with open(jsonl_path, "w", encoding="utf-8") as f:
        for r in records:
            f.write(json.dumps({
                "id": r.id,
                "name": r.name,
                "slug": r.slug,
                "url": r.url,
                "description": r.description,
                "emails": r.emails,
                "email": r.email,
                "phone": r.phone,
                "address": r.address,
                "contacts_raw": r.contacts_raw,
                "categories": r.categories,
                "socials": r.socials,
            }, ensure_ascii=False) + "\n")

    # CSV output (flatten)
    df = pd.DataFrame([{
        "id": r.id,
        "name": r.name,
        "slug": r.slug,
        "url": r.url,
        "description": r.description,
        "emails": "; ".join(r.emails),
        "email": r.email or "",
        "phone": r.phone or "",
        "address": r.address or "",
        "contacts_raw": r.contacts_raw or "",
        "categories": "; ".join(r.categories),
        "socials": json.dumps(r.socials, ensure_ascii=False),
        "website": r.socials.get("website") or "",
        "instagram": r.socials.get("instagram") or "",
        "facebook": r.socials.get("facebook") or "",
        "twitter": r.socials.get("twitter") or "",
        "linkedin": r.socials.get("linkedin") or "",
    } for r in records])
    df.to_csv(csv_path, index=False)
    print(f"[OK] Wrote {jsonl_path} and {csv_path}")

# Visited tracking
def load_visited(path: str) -> Set[str]:
    if not os.path.exists(path):
        return set()
    with open(path, "r", encoding="utf-8") as f:
        return set(l.strip() for l in f if l.strip())

def save_visited(path: str, urls: Set[str]):
    with open(path, "w", encoding="utf-8") as f:
        for u in sorted(urls):
            f.write(u + "\n")

# --------------------------------------------------
# Main async entry point (concurrent + resumable)
# --------------------------------------------------

async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="data/msu_engage", help="Output folder")
    parser.add_argument("--max-orgs", type=int, default=0, help="Limit scrape count (0 = no limit)")
    parser.add_argument("--headless", action="store_true", help="Run headless")
    parser.add_argument("--resume", action="store_true", help="Skip previously scraped org URLs")
    parser.add_argument("--delay-ms", type=int, default=800, help="Delay after page load (ms)")
    parser.add_argument("--per-batch-sleep-ms", type=int, default=600, help="Pause every N requests (ms)")
    parser.add_argument("--save-every", type=int, default=25, help="Save progress every N org pages")
    parser.add_argument("--concurrency", type=int, default=6, help="Concurrent page fetches")
    parser.add_argument("--user-agent", type=str, default="Mozilla/5.0 (compatible; msu-engage-scraper/1.0)", help="User-Agent header for requests")
    args = parser.parse_args()

    out_dir = args.out
    os.makedirs(out_dir, exist_ok=True)
    visited_path = os.path.join(out_dir, "visited_urls.txt")

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=args.headless)
        print("[*] Loading directory…")
        org_links = await fetch_directory_org_links(browser, headless=args.headless, user_agent=args.user_agent)
        print(f"[*] Found {len(org_links)} org links")

        if args.resume:
            already = load_visited(visited_path)
            org_links = [u for u in org_links if u not in already]
            print(f"[*] Resuming, {len(org_links)} remaining")

        if args.max_orgs and args.max_orgs > 0:
            org_links = org_links[: args.max_orgs]

        records: List[Club] = []
        visited_now: Set[str] = set(load_visited(visited_path))

        sem = asyncio.Semaphore(args.concurrency)

        async def worker(url: str) -> Tuple[str, Optional[Club], Optional[Exception]]:
            async with sem:
                try:
                    club = await fetch_org_page_and_parse(browser, url, user_agent=args.user_agent, delay_ms=args.delay_ms)
                    return (url, club, None)
                except Exception as e:
                    return (url, None, e)

        tasks = []
        processed = 0
        for i, url in enumerate(org_links, 1):
            tasks.append(worker(url))

            # dispatch batch when enough gathered or last item
            if len(tasks) >= args.concurrency or i == len(org_links):
                results = await asyncio.gather(*tasks, return_exceptions=False)
                for (url_ret, club, err) in results:
                    processed += 1
                    if err:
                        print(f"[ERROR] {url_ret} -> {err}", file=sys.stderr)
                        continue
                    if club:
                        records.append(club)
                        visited_now.add(url_ret)

                tasks = []

                # save periodically
                if (processed % args.save_every) == 0 or processed == len(org_links):
                    print(f"[*] Progress: scraped {processed} pages — saving partial results")
                    save_visited(visited_path, visited_now)
                    save_jsonl_csv(records, out_dir)
                    await asyncio.sleep(args.per_batch_sleep_ms / 1000.0)

        await browser.close()

    # final save
    save_visited(visited_path, visited_now)
    save_jsonl_csv(records, out_dir)
    print("[*] Done.")

if __name__ == "__main__":
    asyncio.run(main())
