#!/usr/bin/env python3
"""Fail when a tracked *.md names a file that does not exist in the repo.

Checks markdown link targets and backtick spans that look like a file path
(have a known file extension). A reference resolves if it exists relative to the
repo root or to the .md file, or (bare file name) if any tracked file has that name.
References deliberately pointing outside the repo go in EXTERNAL.
"""
import re
import subprocess
import sys
from pathlib import Path

EXTERNAL = {
    # Tài liệu gốc nằm ngoài repo (AGENTS.md "Nguồn nghiệp vụ"); không có sẵn trong checkout.
    "../bc.docx",
    "bc.docx",
    "../phase/quy-uoc-phan-tich.md",
    "../phase/quy-uoc-thiet-ke.md",
    "../phase/nhan-xet-thiet-ke.md",
    "phase/quy-uoc-phan-tich.md",
    "requirement.md",
}

EXT = r"md|puml|svg|png|java|tsx?|mjs|py|sql|ya?ml|sh|json|docx|properties|xml|html|css"
BACKTICK = re.compile(r"`([^`\s]+)`")
LINK = re.compile(r"\]\(([^)\s]+)\)")
PATH_LIKE = re.compile(rf"^[\w./@-]+\.(?:{EXT})$")


def tracked() -> list[str]:
    out = subprocess.run(
        ["git", "-c", "core.quotePath=false", "ls-files"],
        check=True, capture_output=True, text=True,
    ).stdout.splitlines()
    return [f for f in out if Path(f).is_file()]


def main() -> int:
    files = tracked()
    names = {Path(f).name for f in files}
    docs = [f for f in files if f.endswith(".md") and not f.startswith("graphify-out/")]
    problems = []
    for doc in docs:
        text = Path(doc).read_text(encoding="utf-8")
        refs = set(BACKTICK.findall(text)) | set(LINK.findall(text))
        for ref in sorted(refs):
            target = ref.split("#")[0]
            if not target or re.match(r"^[a-z]+://", target) or not PATH_LIKE.match(target):
                continue
            if target in EXTERNAL:
                continue
            if (Path(target).is_file() or (Path(doc).parent / target).is_file()
                    or ("/" not in target and target in names)):
                continue
            problems.append(f"{doc}: {ref}")
    if problems:
        print(f"{len(problems)} dead reference(s) in markdown:")
        for p in problems:
            print(f"  {p}")
        print("Fix the path, or add a deliberate external reference to EXTERNAL in scripts/check-md-links.py.")
        return 1
    print(f"markdown references OK ({len(docs)} files).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
