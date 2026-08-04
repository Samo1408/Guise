#!/usr/bin/env python3
"""Update README download totals from GitHub Releases API data."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


START_MARKER = "<!-- download-stats:start -->"
END_MARKER = "<!-- download-stats:end -->"


def load_releases(source: str) -> list[dict[str, Any]]:
    raw = json.load(sys.stdin if source == "-" else Path(source).open(encoding="utf-8"))
    if raw and isinstance(raw[0], list):
        return [release for page in raw for release in page]
    return raw


def count_downloads(releases: list[dict[str, Any]]) -> tuple[int, int]:
    guise = 0
    guise_test = 0
    for release in releases:
        if release.get("draft"):
            continue
        for asset in release.get("assets", []):
            name = str(asset.get("name", "")).lower()
            if not name.endswith(".apk"):
                continue
            downloads = int(asset.get("download_count", 0))
            if name.startswith("guise-test-"):
                guise_test += downloads
            elif name.startswith("guise-"):
                guise += downloads
    return guise, guise_test


def render_block(guise: int, guise_test: int) -> str:
    return f"""| 下载项目 | 累计下载量 |
| --- | ---: |
| Guise Reborn APK | **{guise:,}** |
| Guise Test APK | **{guise_test:,}** |
| 仓库 APK 合计 | **{guise + guise_test:,}** |

> 数据由 GitHub Actions 根据当前全部 Release 附件自动累计，正式版与 Pre-release 不作区分；已删除 Release 的历史下载量无法由 GitHub API 恢复。"""


def update_readme(readme_path: Path, block: str) -> None:
    content = readme_path.read_text(encoding="utf-8")
    if content.count(START_MARKER) != 1 or content.count(END_MARKER) != 1:
        raise ValueError("README download-stat markers are missing or duplicated")
    before, remainder = content.split(START_MARKER, 1)
    _, after = remainder.split(END_MARKER, 1)
    updated = f"{before}{START_MARKER}\n{block}\n{END_MARKER}{after}"
    with readme_path.open("w", encoding="utf-8", newline="\n") as output:
        output.write(updated)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--releases-json", required=True)
    parser.add_argument("--readme", default="README.md")
    args = parser.parse_args()

    releases = load_releases(args.releases_json)
    guise, guise_test = count_downloads(releases)
    update_readme(Path(args.readme), render_block(guise, guise_test))
    print(f"Guise={guise}, Guise Test={guise_test}, total={guise + guise_test}")


if __name__ == "__main__":
    main()
