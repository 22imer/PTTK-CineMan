#!/usr/bin/env bash
# Checkpoint one coherent local commit from explicit files only.
#
# Usage:
#   bash scripts/agent-commit.sh "<message>" -- <file>...
#
# Safety contract:
#   - refuses anything except literal file paths inside this work tree
#   - refuses a dirty index, merge/rebase/cherry-pick/revert, or conflicts
#   - locks with flock on $(git rev-parse --git-path agent-commit.lock)
#   - stages only the listed files, runs `git diff --cached --check`, commits local
#   - never pushes, resets, cleans, or stages other paths
#
# Exit codes: 0 = committed or already clean (no-op); non-zero = refused/failed.
# On commit failure the staged files are preserved for inspection.

set -euo pipefail

die() {
	printf 'agent-commit: %s\n' "$*" >&2
	exit 1
}

usage() {
	cat >&2 <<'EOF'
Usage: bash scripts/agent-commit.sh "<message>" -- <file>...

  <message>   non-empty local commit message (the text before the separator)
  --          separator between the message and the explicit file list
  <file>...   one or more literal files, relative to the current directory,
              tracked or existing (a missing tracked file stages its deletion)

No directories, '.', wildcards, pathspec magic, absolute paths, paths outside
the repository, or .git paths. Quote names that contain spaces or shell symbols.
EOF
}

if [ "$#" -lt 3 ]; then
	usage
	die "no files"
fi

message=$1
shift

if [ -z "$message" ]; then
	usage
	die "empty message"
fi
case "$message" in
	*[![:space:]]*) ;;
	*)
		usage
		die "empty message"
		;;
esac

if [ "${1:-}" != "--" ]; then
	usage
	die "missing -- separator"
fi
shift

if [ "$#" -lt 1 ]; then
	usage
	die "no files"
fi

files=("$@")

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "not a git work tree"
toplevel=$(git rev-parse --show-toplevel 2>/dev/null) || die "cannot resolve work tree root"
root=$(cd "$toplevel" && pwd -P) || die "cannot resolve work tree root"

command -v flock >/dev/null 2>&1 || die "flock is required but not available"

lock_path=$(git rev-parse --git-path agent-commit.lock 2>/dev/null) || die "cannot resolve lock path"
exec 9>"$lock_path" || die "cannot open lock file: $lock_path"
flock -n 9 || die "lock busy (another agent-commit is running)"

staged=0
git diff --cached --quiet || staged=$?
if [ "$staged" -eq 1 ]; then
	die "staged index not clean (commit or unstage existing changes first)"
elif [ "$staged" -gt 1 ]; then
	die "cannot read the index"
fi

if [ -n "$(git ls-files -u)" ]; then
	die "conflict: unresolved index entries"
fi

if [ -e "$(git rev-parse --git-path MERGE_HEAD)" ] \
	|| [ -d "$(git rev-parse --git-path rebase-merge)" ] \
	|| [ -d "$(git rev-parse --git-path rebase-apply)" ] \
	|| [ -e "$(git rev-parse --git-path CHERRY_PICK_HEAD)" ] \
	|| [ -e "$(git rev-parse --git-path REVERT_HEAD)" ]; then
	die "merge or rebase in progress"
fi

for index in "${!files[@]}"; do
	file=${files[$index]}
	while [ "${file#./}" != "$file" ]; do
		file=${file#./}
	done
	files[$index]=$file
	if [ -z "$file" ]; then
		die "invalid path: empty file argument"
	fi
	case "$file" in
		-*)
			die "wildcard/pathspec magic or option-like path: $file"
			;;
		/*|[A-Za-z]:[\\/]*)
			die "outside repo: $file"
			;;
		:*|*':('*|*':!'*|*':^'*)
			die "wildcard/pathspec magic: $file"
			;;
		*'*'*|*'?'*|*'['*|*']'*)
			die "wildcard/pathspec magic: $file"
			;;
	esac
	case "$file" in
		*$'\n'*)
			die "invalid path (newline): $file"
			;;
	esac
	if [ "$file" = "." ] || [ "$file" = ".." ]; then
		die "directory or root: $file"
	fi
	if [ -d "$file" ]; then
		die "directory or root: $file"
	fi

	IFS='/' read -r -a segments <<<"$file"
	for segment in "${segments[@]}"; do
		case "$segment" in
			"")
				die "invalid path (empty segment): $file"
				;;
			".")
				die "directory or root: $file"
				;;
			"..")
				die "outside repo (parent escape): $file"
				;;
			".git")
				die ".git path not allowed: $file"
				;;
		esac
	done

	resolved=$(realpath -m -- "$file" 2>/dev/null) || die "cannot resolve path: $file"
	case "$resolved" in
		"$root"/*) ;;
		*) die "outside repo: $file" ;;
	esac

	if [ ! -e "$file" ] && [ ! -L "$file" ]; then
		git --literal-pathspecs ls-files --error-unmatch -- "$file" >/dev/null 2>&1 \
			|| die "not tracked (deletion): $file"
	fi
done

git --literal-pathspecs add -- "${files[@]}" || die "git add failed"

if git diff --cached --quiet; then
	printf 'agent-commit: nothing to commit\n' >&2
	exit 0
fi

if ! git diff --cached --check; then
	die "staged diff check failed; staged files preserved for inspection"
fi

if ! git commit -m "$message"; then
	die "commit failed; staged files preserved for inspection"
fi

printf 'agent-commit: committed %s\n' "$(git rev-parse --short HEAD)"
