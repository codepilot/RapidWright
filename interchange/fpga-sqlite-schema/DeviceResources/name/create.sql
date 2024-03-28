create table if not exists `name` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL UNIQUE-- name            @0 : Text;
) strict;