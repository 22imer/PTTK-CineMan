import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import type { ExtensionAPI } from "@oh-my-pi/pi-coding-agent";

const MODULE_DIR = (import.meta as ImportMeta & { dir?: string }).dir ?? dirname(fileURLToPath(import.meta.url));
const SCRIPT_PATH = join(MODULE_DIR, "..", "..", "..", "scripts", "agent-commit.sh");

/**
 * Files this process observed a successful edit/write on, keyed by session id.
 * Module state is shared by rebound child sessions, so every entry is scoped
 * to the session that performed the tool call.
 */
const OBSERVED = new Map<string, Set<string>>();

const INTERNAL_URL_RE = /^[A-Za-z][A-Za-z0-9+.-]*:\/\//u;
const WINDOWS_DRIVE_RE = /^[A-Za-z]:[\\/]/u;
const APPLY_PATCH_HEADER_RE = /^\*\*\* (?:Add|Update|Delete) File:[ \t]*(.+)$/gmu;
const APPLY_PATCH_MOVE_RE = /^\*\*\* Move to:[ \t]*(.+)$/gmu;
const HASHLINE_HEADER_RE = /^\[(.+?)#[0-9a-fA-F]{4}\]$/gmu;
const LEGACY_HASHLINE_HEADER_RE = /^¶+(.+?)#[0-9a-fA-F]{4}$/gmu;

type SessionContextLike = {
	sessionManager?: { getSessionId?: () => string };
};

function sessionKey(ctx: unknown): string | undefined {
	const manager = (ctx as SessionContextLike | undefined)?.sessionManager;
	const id = manager?.getSessionId?.();
	return typeof id === "string" && id.length > 0 ? id : undefined;
}

/** Map a tool-provided path to an absolute local file inside `cwd`, or ignore it. */
function toLocalPath(raw: string, cwd: string): string | undefined {
	const value = raw.trim();
	if (value.length === 0) return undefined;
	if (INTERNAL_URL_RE.test(value) || WINDOWS_DRIVE_RE.test(value)) return undefined;
	const root = resolve(cwd);
	const absolute = resolve(root, value);
	if (absolute === root || !absolute.startsWith(root + sep)) return undefined;
	return absolute;
}

/** Paths named by apply-patch headers or hashline file headers. */
function pathsFromText(text: string): string[] {
	const paths: string[] = [];
	for (const pattern of [
		APPLY_PATCH_HEADER_RE,
		APPLY_PATCH_MOVE_RE,
		HASHLINE_HEADER_RE,
		LEGACY_HASHLINE_HEADER_RE,
	]) {
		for (const match of text.matchAll(pattern)) {
			const value = match[1]?.trim();
			if (value) paths.push(value);
		}
	}
	return paths;
}

function collectObservedPaths(toolName: string, input: Record<string, unknown>): string[] {
	if (toolName !== "edit" && toolName !== "write") return [];
	const raw: string[] = [];
	const add = (value: unknown): void => {
		if (typeof value === "string") raw.push(value);
	};
	add(input._path);
	add(input.path);
	if (Array.isArray(input.paths)) {
		for (const value of input.paths) add(value);
	}
	if (toolName === "edit" && raw.length === 0) {
		// `path`/`paths` are derived by the host for hashline edits. Fall back to
		// parsing patch headers for hosts/tests that forward the raw patch only.
		for (const key of ["patch", "input", "_input", "diff"]) {
			const value = input[key];
			if (typeof value === "string") raw.push(...pathsFromText(value));
		}
	}
	return raw;
}

function displayPath(absolute: string, cwd: string): string {
	const rel = relative(cwd, absolute);
	return rel.length > 0 && rel !== ".." && !rel.startsWith(`..${sep}`) ? rel : absolute;
}

type Exec = (command: string, args: string[], options?: { cwd?: string; timeout?: number }) => Promise<{
	stdout: string;
	stderr: string;
	code: number;
}>;

/** Observed files that are still dirty. `undefined` means git could not tell. */
async function dirtyObservedPaths(exec: Exec, cwd: string, paths: string[]): Promise<string[] | undefined> {
	const rootResult = await exec("git", ["rev-parse", "--show-toplevel"], { cwd, timeout: 10_000 });
	if (rootResult.code !== 0) return undefined;
	const root = rootResult.stdout.trim();
	if (root.length === 0) return undefined;

	const relativePaths = paths
		.map(path => relative(root, path))
		.filter(path => path.length > 0 && path !== ".." && !path.startsWith(`..${sep}`));
	if (relativePaths.length === 0) return undefined;

	const status = await exec(
		"git",
		[
			"--literal-pathspecs",
			"status",
			"--porcelain=v1",
			"-z",
			"--untracked-files=all",
			"--",
			...relativePaths,
		],
		{ cwd: root, timeout: 10_000 },
	);
	if (status.code !== 0) return undefined;
	if (status.stdout.length === 0) return [];

	const dirty: string[] = [];
	const entries = status.stdout.split("\0");
	for (let index = 0; index < entries.length; index++) {
		const entry = entries[index];
		if (!entry || entry.length < 4) continue;
		const code = entry.slice(0, 2);
		dirty.push(resolve(root, entry.slice(3)));
		// Porcelain v1 -z emits the original path as the next field for renames/copies.
		if (code[0] === "R" || code[0] === "C") index++;
	}

	const wanted = new Set(paths);
	return dirty.filter(path => wanted.has(path));
}

function findSeparator(input: string): { index: number; length: number } | undefined {
	let quote: '"' | "'" | undefined;
	let escaped = false;
	let last: { index: number; length: number } | undefined;
	for (let index = 0; index < input.length; index++) {
		const char = input[index];
		if (escaped) {
			escaped = false;
			continue;
		}
		if (quote === "'") {
			if (char === "'") quote = undefined;
			continue;
		}
		if (quote === '"') {
			if (char === "\\") escaped = true;
			else if (char === '"') quote = undefined;
			continue;
		}
		if (char === "\\") {
			escaped = true;
			continue;
		}
		if (char === '"' || char === "'") {
			quote = char;
			continue;
		}
		if (char === "-" && input[index + 1] === "-") {
			const before = index === 0 || /\s/u.test(input[index - 1]);
			const after = index + 2 >= input.length || /\s/u.test(input[index + 2]);
			if (before && after) {
				last = { index, length: 2 };
				index++;
			}
		}
	}
	return last;
}

function tokenizeFileArgs(input: string): string[] | { error: string } {
	const tokens: string[] = [];
	let current = "";
	let started = false;
	let quote: '"' | "'" | undefined;
	let escaped = false;

	for (const char of input) {
		if (escaped) {
			current += char;
			started = true;
			escaped = false;
			continue;
		}
		if (quote === "'") {
			if (char === "'") quote = undefined;
			else {
				current += char;
				started = true;
			}
			continue;
		}
		if (quote === '"') {
			if (char === "\\") escaped = true;
			else if (char === '"') quote = undefined;
			else {
				current += char;
				started = true;
			}
			continue;
		}
		if (char === "\\") {
			escaped = true;
			continue;
		}
		if (char === '"' || char === "'") {
			quote = char;
			started = true;
			continue;
		}
		if (/\s/u.test(char)) {
			if (started) {
				tokens.push(current);
				current = "";
				started = false;
			}
			continue;
		}
		current += char;
		started = true;
	}

	if (escaped) {
		current += "\\";
		started = true;
	}
	if (quote) return { error: "unterminated quote in file list" };
	if (started) tokens.push(current);
	return tokens;
}

function parseCheckpointArgs(args: string): { message: string; files: string[] } | { error: string } {
	const usage =
		'Usage: /checkpoint <message> -- <file>... (message is raw text before --; quote only file paths)';
	const input = args.trim();
	if (input.length === 0) return { error: usage };

	const separator = findSeparator(input);
	if (!separator) return { error: `${usage} (missing standalone -- separator)` };

	const message = input.slice(0, separator.index).trim();
	if (message.length === 0) return { error: "Checkpoint message must be non-empty." };

	const filesPart = input.slice(separator.index + separator.length).trim();
	if (filesPart.length === 0) return { error: `${usage} (no files after --)` };

	const tokens = tokenizeFileArgs(filesPart);
	if (!Array.isArray(tokens)) return tokens;
	if (tokens.length === 0) return { error: `${usage} (no files after --)` };
	if (tokens.some(token => token.length === 0)) {
		return { error: "Checkpoint file arguments must be non-empty." };
	}
	return { message, files: tokens };
}

export default function (pi: ExtensionAPI): void {
	pi.on("tool_result", async (event, ctx) => {
		if (event.isError) return;
		if (event.toolName !== "edit" && event.toolName !== "write") return;

		const key = sessionKey(ctx) ?? "default";

		const paths = collectObservedPaths(event.toolName, event.input)
			.map(raw => toLocalPath(raw, ctx.cwd))
			.filter((path): path is string => path !== undefined);
		if (paths.length === 0) return;

		let observed = OBSERVED.get(key);
		if (!observed) {
			observed = new Set<string>();
			OBSERVED.set(key, observed);
		}
		for (const path of paths) observed.add(path);

		const listed = [...observed]
			.sort()
			.map(path => `- ${displayPath(path, ctx.cwd)}`)
			.join("\n");
		return {
			additionalContext: [
				`Checkpoint reminder: ${observed.size} local file(s) edited in this session:`,
				listed,
				`After your checks pass, commit one coherent scope: /checkpoint <type(scope): summary> -- <file> ...`,
				"Do not wrap the message in quotes (they become part of the commit message); quote only file paths that contain spaces.",
				"Only stage files you changed and verified. Never claim tests passed unless you ran them.",
			].join("\n"),
		};
	});

	pi.on("session_stop", async (event, ctx) => {
		const candidates = [event.session_id, sessionKey(ctx), "default"].filter(
			(value): value is string => typeof value === "string" && value.length > 0,
		);
		const key = candidates.find(candidate => OBSERVED.has(candidate));
		if (!key) return;

		const observed = OBSERVED.get(key);
		if (!observed || observed.size === 0) return;

		const dirty = await dirtyObservedPaths(
			(command, args, options) => pi.exec(command, args, options),
			ctx.cwd,
			[...observed],
		);
		// A broken git environment must not trap the session in a stop loop.
		if (dirty === undefined) return;
		if (dirty.length === 0) {
			OBSERVED.delete(key);
			return;
		}

		const dirtySet = new Set(dirty);
		for (const path of observed) {
			if (!dirtySet.has(path)) observed.delete(path);
		}

		const listed = dirty
			.slice()
			.sort()
			.map(path => `- ${displayPath(path, ctx.cwd)}`)
			.join("\n");
		return {
			decision: "block" as const,
			reason: [
				"Uncommitted changes remain in files this session edited:",
				listed,
				`Run /checkpoint <type(scope): summary> -- <file> ... after your checks pass (message unquoted; quote only file paths with spaces), or hand the file list to the coordinator for the shared-tree integration commit. Never claim tests passed unless you ran them.`,
			].join("\n"),
		};
	});

	pi.registerCommand("checkpoint", {
		description: "Commit explicit files after verification: /checkpoint <message> -- <file>... (message unquoted)",
		handler: async (args, ctx) => {
			const parsed = parseCheckpointArgs(args);
			if ("error" in parsed) {
				ctx.ui.notify(parsed.error, "warning");
				return;
			}

			const result = await pi.exec("bash", [SCRIPT_PATH, parsed.message, "--", ...parsed.files], {
				cwd: ctx.cwd,
				timeout: 120_000,
			});
			const output = [result.stdout.trim(), result.stderr.trim()].filter(Boolean).join("\n");

			if (result.code === 0) {
				const key = sessionKey(ctx);
				if (key) {
					const observed = OBSERVED.get(key);
					if (observed) {
						for (const file of parsed.files) observed.delete(resolve(ctx.cwd, file));
						if (observed.size === 0) OBSERVED.delete(key);
					}
				}
				ctx.ui.notify(output || "Checkpoint: nothing to commit.", "info");
				return;
			}

			ctx.ui.notify(output.slice(-4_000) || `agent-commit failed (exit ${result.code}).`, "error");
		},
	});
}
