-- ============================================================
-- Kyoto Music - Ecommerce Schema Migration
-- Run these statements in your Cloudflare D1 database console
-- (Dashboard → Storage & Databases → D1 → your database → Console)
-- ============================================================

-- NOTE: Cloudflare D1 uses SQLite syntax.
-- VARCHAR2, BOOLEAN, NUMBER → TEXT, INTEGER, REAL in SQLite.
-- Use "IF NOT EXISTS" so re-running this is safe.

-- ─────────────────────────────────────────────────────────────
-- FIX: Existing base tables (re-create safely if you haven't yet)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS ADMIN_USER (
    ADMINID  TEXT NOT NULL,
    NAME     TEXT,
    PASSWORD TEXT,
    PRIMARY KEY (ADMINID)
);

CREATE TABLE IF NOT EXISTS ARTIST (
    ARTISTID           TEXT NOT NULL,
    NAME               TEXT,
    PASSWORD           TEXT,
    SONGSRELEASED      TEXT,
    DATEACCOUNTCREATED TEXT,
    GENDER             TEXT,
    ADMINID            TEXT,
    USERNAME           TEXT,
    PRIMARY KEY (ARTISTID),
    FOREIGN KEY (ADMINID) REFERENCES ADMIN_USER(ADMINID)
);

CREATE TABLE IF NOT EXISTS GENRE (
    GENREID   TEXT NOT NULL,
    GENRENAME TEXT,
    PRIMARY KEY (GENREID)
);

CREATE TABLE IF NOT EXISTS ALBUM (
    ALBUMID      TEXT NOT NULL,
    TITLE        TEXT,
    RELEASEDATE  TEXT,
    COVERARTURL  TEXT,
    ARTISTID     TEXT,
    PRIMARY KEY (ALBUMID),
    FOREIGN KEY (ARTISTID) REFERENCES ARTIST(ARTISTID)
);

CREATE TABLE IF NOT EXISTS SONG (
    SONGID          TEXT NOT NULL,
    TITLE           TEXT,
    DURATION        TEXT,
    GENREID         TEXT,
    RELEASEDATE     TEXT,
    AUDIOFILEURL    TEXT,
    COVERARTURL     TEXT,
    ARTISTID        TEXT,
    PlayCount       INTEGER DEFAULT 0,
    DownloadCount   INTEGER DEFAULT 0,
    PRIMARY KEY (SONGID),
    FOREIGN KEY (GENREID)  REFERENCES GENRE(GENREID),
    FOREIGN KEY (ARTISTID) REFERENCES ARTIST(ARTISTID)
);

CREATE TABLE IF NOT EXISTS ALBUM_SONG (
    ALBUMID     TEXT,
    SONGID      TEXT,
    TRACKNUMBER INTEGER NOT NULL,
    PRIMARY KEY (TRACKNUMBER),
    FOREIGN KEY (ALBUMID) REFERENCES ALBUM(ALBUMID),
    FOREIGN KEY (SONGID)  REFERENCES SONG(SONGID)
);

CREATE TABLE IF NOT EXISTS LISTENER (
    LISTENERID TEXT NOT NULL,
    NAME       TEXT,
    GENDER     TEXT,
    ISPREMIUM  TEXT,
    PASSWORD   TEXT,
    USERNAME   TEXT,
    PRIMARY KEY (LISTENERID)
);

CREATE TABLE IF NOT EXISTS PLAYLIST (
    PLAYLISTID  TEXT NOT NULL,
    NAME        TEXT,
    LISTENERID  TEXT,
    DATECREATED TEXT,
    ISPUBLIC    TEXT,
    PRIMARY KEY (PLAYLISTID),
    FOREIGN KEY (LISTENERID) REFERENCES LISTENER(LISTENERID)
);

CREATE TABLE IF NOT EXISTS PLAYLISTSONG (
    PLAYLISTID TEXT,
    SONGID     TEXT,
    POSITION   INTEGER,
    FOREIGN KEY (PLAYLISTID) REFERENCES PLAYLIST(PLAYLISTID),
    FOREIGN KEY (SONGID)     REFERENCES SONG(SONGID)
);

CREATE TABLE IF NOT EXISTS LISTENHISTORY (
    HISTORYID  TEXT NOT NULL,
    LISTENERID TEXT,
    SONGID     TEXT,
    LISTENDATE TEXT,
    PRIMARY KEY (HISTORYID),
    FOREIGN KEY (LISTENERID) REFERENCES LISTENER(LISTENERID),
    FOREIGN KEY (SONGID)     REFERENCES SONG(SONGID)
);

-- ─────────────────────────────────────────────────────────────
-- NEW: PRODUCT table (replaces MERCH — used by Store pages)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS PRODUCT (
    ProductID   TEXT NOT NULL,
    ArtistID    TEXT NOT NULL,
    Name        TEXT NOT NULL,
    Description TEXT,
    Price       REAL NOT NULL DEFAULT 0.0,
    Category    TEXT NOT NULL DEFAULT 'Merch',
    Stock       INTEGER NOT NULL DEFAULT 0,
    ImageURL    TEXT,
    CreatedAt   TEXT,
    PRIMARY KEY (ProductID),
    FOREIGN KEY (ArtistID) REFERENCES ARTIST(ARTISTID)
);

-- ─────────────────────────────────────────────────────────────
-- NEW: PRODUCT_ORDER table (tracks listener purchases of merch)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS PRODUCT_ORDER (
    OrderID     TEXT NOT NULL,
    ProductID   TEXT NOT NULL,
    ListenerID  TEXT NOT NULL,
    Quantity    INTEGER NOT NULL DEFAULT 1,
    TotalAmount REAL NOT NULL DEFAULT 0.0,
    PurchasedAt TEXT,
    PRIMARY KEY (OrderID),
    FOREIGN KEY (ProductID)  REFERENCES PRODUCT(ProductID),
    FOREIGN KEY (ListenerID) REFERENCES LISTENER(LISTENERID)
);

-- ─────────────────────────────────────────────────────────────
-- NEW: EVENT table (artist concerts / shows)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EVENT (
    EventID       TEXT NOT NULL,
    ArtistID      TEXT NOT NULL,
    Title         TEXT NOT NULL,
    Venue         TEXT,
    EventDate     TEXT,
    EventTime     TEXT,
    TicketPrice   REAL NOT NULL DEFAULT 0.0,
    TotalTickets  INTEGER NOT NULL DEFAULT 0,
    TicketsSold   INTEGER NOT NULL DEFAULT 0,
    Description   TEXT,
    ImageURL      TEXT,
    CreatedAt     TEXT,
    PRIMARY KEY (EventID),
    FOREIGN KEY (ArtistID) REFERENCES ARTIST(ARTISTID)
);

-- ─────────────────────────────────────────────────────────────
-- NEW: TICKET_ORDER table (listener buys tickets for events)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS TICKET_ORDER (
    OrderID     TEXT NOT NULL,
    EventID     TEXT NOT NULL,
    ListenerID  TEXT NOT NULL,
    Quantity    INTEGER NOT NULL DEFAULT 1,
    TotalAmount REAL NOT NULL DEFAULT 0.0,
    TicketCode  TEXT,
    PurchasedAt TEXT,
    PRIMARY KEY (OrderID),
    FOREIGN KEY (EventID)    REFERENCES EVENT(EventID),
    FOREIGN KEY (ListenerID) REFERENCES LISTENER(LISTENERID)
);

-- ─────────────────────────────────────────────────────────────
-- DONE — All 4 missing tables created.
-- ─────────────────────────────────────────────────────────────
