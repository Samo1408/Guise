#!/usr/bin/env python3
"""Convert the official MobileModels CSV export into Guise's bundled SQLite DB."""

import argparse
import csv
import sqlite3
from contextlib import closing
from pathlib import Path
from typing import Optional


SCHEMA = """
CREATE TABLE models (
    model TEXT NOT NULL,
    dtype TEXT,
    brand TEXT NOT NULL,
    brand_title TEXT NOT NULL,
    code TEXT,
    code_alias TEXT,
    model_name TEXT,
    ver_name TEXT
);
CREATE INDEX models_brand_model_index ON models (brand, model);
CREATE TABLE metadata (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
"""


def optional(value: str) -> Optional[str]:
    value = value.strip()
    return value or None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("csv", type=Path, help="MobileModels-csv models.csv")
    parser.add_argument("output", type=Path, help="Output devices.db")
    parser.add_argument("--source-revision", required=True)
    args = parser.parse_args()

    with args.csv.open(encoding="utf-8-sig", newline="") as source:
        rows = list(csv.DictReader(source))

    required = {
        "model", "dtype", "brand", "brand_title", "code", "code_alias",
        "model_name", "ver_name",
    }
    if not rows or set(rows[0]) != required:
        raise ValueError("Unexpected MobileModels CSV schema")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(".db.tmp")
    temporary.unlink(missing_ok=True)

    with closing(sqlite3.connect(temporary)) as database:
        database.executescript(SCHEMA)
        database.executemany(
            """
            INSERT INTO models (
                model, dtype, brand, brand_title, code, code_alias, model_name, ver_name
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                (
                    row["model"].strip(),
                    optional(row["dtype"]),
                    row["brand"].strip(),
                    row["brand_title"].strip(),
                    optional(row["code"]),
                    optional(row["code_alias"]),
                    optional(row["model_name"]),
                    optional(row["ver_name"]),
                )
                for row in rows
                if row["model"].strip() and row["brand"].strip()
            ),
        )
        database.executemany(
            "INSERT INTO metadata (key, value) VALUES (?, ?)",
            (
                ("source", "KHwang9883/MobileModels-csv"),
                ("source_revision", args.source_revision),
                ("license", "CC BY-NC-SA 4.0"),
            ),
        )
        database.commit()
        database.execute("VACUUM")

    try:
        temporary.replace(args.output)
    except PermissionError:
        # Gradle can briefly hold an asset file open on Windows. SQLite backup
        # safely updates the existing file even when an atomic rename is denied.
        with closing(sqlite3.connect(temporary)) as source, closing(
            sqlite3.connect(args.output)
        ) as target:
            source.backup(target)
        temporary.unlink()

    with closing(sqlite3.connect(args.output)) as database:
        row_count = database.execute("SELECT COUNT(*) FROM models").fetchone()[0]
        brand_count = database.execute(
            "SELECT COUNT(DISTINCT brand) FROM models"
        ).fetchone()[0]
    print(f"Wrote {row_count} models across {brand_count} brands to {args.output}")


if __name__ == "__main__":
    main()
