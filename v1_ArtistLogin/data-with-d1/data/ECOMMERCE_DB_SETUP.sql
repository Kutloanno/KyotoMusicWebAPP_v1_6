-- ================================================================
-- KYOTO MUSIC - ECOMMERCE & EVENTS TABLES
-- Run these SQL statements in your Cloudflare D1 database console
-- Dashboard → Storage & Databases → D1 → your DB → Console
-- ================================================================

-- 1. PRODUCTS TABLE (Artist Merch / Music Store)
CREATE TABLE IF NOT EXISTS PRODUCT (
    ProductID   TEXT PRIMARY KEY,
    ArtistID    TEXT NOT NULL,
    Name        TEXT NOT NULL,
    Description TEXT DEFAULT '',
    Price       REAL NOT NULL,
    Category    TEXT NOT NULL DEFAULT 'Merch',  -- Merch / Music / Digital / Other
    Stock       INTEGER NOT NULL DEFAULT 0,
    ImageURL    TEXT DEFAULT '',
    CreatedAt   TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ArtistID) REFERENCES ARTIST(ArtistID)
);

-- 2. PRODUCT ORDERS TABLE (Listener purchases a product)
CREATE TABLE IF NOT EXISTS PRODUCT_ORDER (
    OrderID     TEXT PRIMARY KEY,
    ProductID   TEXT NOT NULL,
    ListenerID  TEXT NOT NULL,
    Quantity    INTEGER NOT NULL DEFAULT 1,
    TotalAmount REAL NOT NULL,
    PurchasedAt TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ProductID)  REFERENCES PRODUCT(ProductID),
    FOREIGN KEY (ListenerID) REFERENCES LISTENER(ListenerID)
);

-- 3. EVENTS TABLE (Artist posts concerts / events)
CREATE TABLE IF NOT EXISTS EVENT (
    EventID      TEXT PRIMARY KEY,
    ArtistID     TEXT NOT NULL,
    Title        TEXT NOT NULL,
    Venue        TEXT NOT NULL,
    EventDate    TEXT NOT NULL,   -- stored as YYYY-MM-DD
    EventTime    TEXT NOT NULL,   -- stored as HH:MM
    TicketPrice  REAL NOT NULL DEFAULT 0,
    TotalTickets INTEGER NOT NULL DEFAULT 0,
    TicketsSold  INTEGER NOT NULL DEFAULT 0,
    Description  TEXT DEFAULT '',
    ImageURL     TEXT DEFAULT '',
    CreatedAt    TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ArtistID) REFERENCES ARTIST(ArtistID)
);

-- 4. TICKET ORDERS TABLE (Listener buys a ticket)
CREATE TABLE IF NOT EXISTS TICKET_ORDER (
    OrderID     TEXT PRIMARY KEY,
    EventID     TEXT NOT NULL,
    ListenerID  TEXT NOT NULL,
    Quantity    INTEGER NOT NULL DEFAULT 1,
    TotalAmount REAL NOT NULL,
    TicketCode  TEXT NOT NULL,    -- unique code shown to user
    PurchasedAt TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (EventID)    REFERENCES EVENT(EventID),
    FOREIGN KEY (ListenerID) REFERENCES LISTENER(ListenerID)
);

-- ================================================================
-- VERIFICATION - run these to confirm tables were created:
-- SELECT name FROM sqlite_master WHERE type='table';
-- ================================================================
