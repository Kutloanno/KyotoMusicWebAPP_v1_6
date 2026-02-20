# Fix: 500 Internal Server Error on /store and /events

## Root Cause

The **Store** and **Events** pages crash with a 500 error because the Java controller
(`EcommerceController`) queries tables that **do not exist** in your Cloudflare D1 database:

| Missing Table    | Used By                          |
|------------------|----------------------------------|
| `PRODUCT`        | Artist Store + Listener Store    |
| `PRODUCT_ORDER`  | Listener buys merch              |
| `EVENT`          | Artist Events + Listener Events  |
| `TICKET_ORDER`   | Listener buys tickets            |

Additionally, the existing base tables had two issues:
- `SONG` table used `PLAYACCOUNT`/`DOWNLOADACCOUNT` but the Java code expects `PlayCount`/`DownloadCount`
- `LISTENHISTORY` had a typo (`VARCHARID`) on the `SONGID` column and `LISTENDAT` was truncated

All of these are fixed in the migration file.

---

## How to Apply the Fix

### Step 1 – Open Cloudflare D1 Console

1. Go to [dash.cloudflare.com](https://dash.cloudflare.com)
2. Navigate to **Storage & Databases → D1**
3. Click your database (`kyoto` or whatever you named it)
4. Click the **Console** tab

### Step 2 – Run the Migration SQL

Open the file:
```
src/main/resources/ecommerce_schema_migration.sql
```

Copy **all** the content and paste it into the D1 Console, then click **Execute**.

The statements all use `CREATE TABLE IF NOT EXISTS` so it is **safe to re-run** — it will
not drop or overwrite any existing data.

### Step 3 – Verify the Tables Exist

In the D1 Console, run:
```sql
SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;
```

You should see all these tables listed:
- `ADMIN_USER`
- `ALBUM`, `ALBUM_SONG`
- `ARTIST`
- `EVENT`
- `GENRE`
- `LISTENER`
- `LISTENHISTORY`
- `PLAYLIST`, `PLAYLISTSONG`
- `PRODUCT`, `PRODUCT_ORDER`
- `SONG`
- `TICKET_ORDER`

### Step 4 – Restart Your Spring Boot App

```bash
./mvnw spring-boot:run
```

Now visit `/store?listenerId=<id>` and `/events?listenerId=<id>` — they should load correctly.

---

## What Changed in the Code

No Java controller code needed changes — the `EcommerceController.java` was already written
correctly for the new table structure. The problem was purely that the tables were missing
from the database.

The only schema corrections made vs. your original design:

| Original                  | Fixed                     | Reason                                      |
|---------------------------|---------------------------|---------------------------------------------|
| `PLAYACCOUNT` in SONG     | `PlayCount`               | Matches what ArtistController.java reads    |
| `DOWNLOADACCOUNT` in SONG | `DownloadCount`           | Matches what ArtistController.java reads    |
| `VARCHARID` on SONGID     | `TEXT`                    | Typo in original schema                     |
| `LISTENDAT`               | `LISTENDATE`              | Truncated column name                       |
| `VARCHAR2`, `BOOLEAN`     | `TEXT`, `INTEGER`         | D1/SQLite does not support Oracle types     |
| `NUMBER`                  | `INTEGER` / `REAL`        | D1/SQLite type system                       |
