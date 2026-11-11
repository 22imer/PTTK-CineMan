#!/usr/bin/env python3
"""Fail when graphify-out/manifest.json no longer matches the tracked files.

The manifest stores an md5 (`ast_hash`) per file graphify indexed. A file that
changed, vanished, or is a new file of an indexed type means graph.json is stale.
Fix: run `graphify update .` and commit graphify-out/.
"""
import hashlib
import json
import subprocess
import sys
from pathlib import Path

MANIFEST = Path("graphify-out/manifest.json")


def md5(path: str) -> str:
    return hashlib.md5(Path(path).read_bytes()).hexdigest()


def main() -> int:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    tracked = subprocess.run(
        ["git", "-c", "core.quotePath=false", "ls-files"],
        check=True, capture_output=True, text=True,
    ).stdout.splitlines()
    tracked = [f for f in tracked if not f.startswith("graphify-out/")]

    problems = []
    for path, entry in manifest.items():
        if not Path(path).is_file():
            problems.append(f"deleted: {path}")
        elif md5(path) != entry["ast_hash"]:
            problems.append(f"changed: {path}")

    indexed_suffixes = {Path(p).suffix for p in manifest}
    for path in tracked:
        suffix = Path(path).suffix
        if path not in manifest and suffix and suffix in indexed_suffixes:
            problems.append(f"new: {path}")

    if problems:
        print(f"graphify-out is stale ({len(problems)} file(s)):")
        for line in sorted(problems):
            print(f"  {line}")
        print("Run `graphify update .` and commit graphify-out/.")
        return 1
    print(f"graphify-out is fresh ({len(manifest)} files).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
