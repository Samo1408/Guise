#!/usr/bin/env python3
"""Generate the compact carrier preset asset from pbakondy/mcc-mnc-list."""

import argparse
import json
from pathlib import Path


INCLUDED_STATUSES = {
    "Operational",
    "Temporary operational",
    "Planned",
    "Upcoming",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="upstream mcc-mnc-list.json")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets/carriers.json"),
    )
    args = parser.parse_args()

    records = json.loads(args.source.read_text(encoding="utf-8"))
    carriers = {}
    for record in records:
        if record.get("status") not in INCLUDED_STATUSES:
            continue
        mcc = str(record.get("mcc", "")).strip()
        mnc = str(record.get("mnc", "")).strip()
        if len(mcc) != 3 or not mcc.isdigit() or not mnc.isdigit():
            continue
        plmn = mcc + mnc
        name = (record.get("brand") or record.get("operator") or "").strip()
        if not name:
            continue
        country_code = str(record.get("countryCode") or "").split("-", 1)[0].upper()
        carriers.setdefault(
            plmn,
            {
                "name": name,
                "plmn": plmn,
                "countryCode": country_code,
                "countryName": (record.get("countryName") or "International").strip(),
            },
        )

    output = sorted(
        carriers.values(),
        key=lambda item: (item["countryName"].casefold(), item["name"].casefold(), item["plmn"]),
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(output, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    print("Wrote {} carriers to {}".format(len(output), args.output))


if __name__ == "__main__":
    main()
