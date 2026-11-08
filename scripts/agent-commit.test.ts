import { afterEach, describe, expect, test } from "bun:test";
import { spawn, spawnSync, type ChildProcess } from "node:child_process";
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, isAbsolute, join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const REPO_ROOT = resolve(import.meta.dir, "..");
const SCRIPT = join(REPO_ROOT, "scripts", "agent-commit.sh");

const tmpDirs: string[] = [];
const children: ChildProcess[] = [];

function cleanEnv(extra: Record<string, string> = {}): Record<string, string> {
	const env: Record<string, string> = {};
	for (const [key, value] of Object.entries(process.env)) {
		if (value !== undefined) env[key] = value;
	}
	delete env.GIT_DIR;
	delete env.GIT_WORK_TREE;
	delete env.GIT_INDEX_FILE;
	delete env.GIT_OBJECT_DIRECTORY;
	delete env.GIT_ALTERNATE_OBJECT_DIRECTORIES;
	env.GIT_CONFIG_NOSYSTEM = "1";
	env.GIT_CONFIG_GLOBAL = "/dev/null";
	env.GIT_MERGE_AUTOEDIT = "no";
	env.LC_ALL = "C";
	env.LANG = "C";
	return Object.assign(env, extra);
}

interface GitResult {
	status: number | null;
	stdout: string;
	stderr: string;
}

function git(repo: string, args: string[], expectSuccess = true): GitResult {
	const result = spawnSync("git", args, { cwd: repo, encoding: "utf8", env: cleanEnv() });
	if (expectSuccess && result.status !== 0) {
		throw new Error(`git ${args.join(" ")} failed (${result.status}): ${result.stderr}`);
	}
	return { status: result.status, stdout: result.stdout ?? "", stderr: result.stderr ?? "" };
}

function gitText(repo: string, args: string[]): string {
	return git(repo, args).stdout;
}

function write(repo: string, relativePath: string, content: string): void {
	const fullPath = join(repo, relativePath);
	mkdirSync(dirname(fullPath), { recursive: true });
	writeFileSync(fullPath, content);
}

function initRepo(options: { initialCommit?: boolean } = {}): string {
	const repo = mkdtempSync(join(tmpdir(), "agent-commit-test-"));
	tmpDirs.push(repo);
	git(repo, ["init", "-q", "-b", "main"]);
	git(repo, ["config", "user.email", "agent-commit-test@example.invalid"]);
	git(repo, ["config", "user.name", "Agent Commit Test"]);
	git(repo, ["config", "commit.gpgsign", "false"]);
	git(repo, ["config", "tag.gpgsign", "false"]);
	if (options.initialCommit !== false) {
		write(repo, "seed.txt", "seed\n");
		git(repo, ["add", "--", "seed.txt"]);
		git(repo, ["commit", "-q", "-m", "initial"]);
	}
	return repo;
}

function commitCount(repo: string): number {
	const result = git(repo, ["rev-list", "--count", "HEAD"], false);
	return result.status === 0 ? Number.parseInt(result.stdout.trim(), 10) : 0;
}

function nulTerms(output: string): string[] {
	return output.split("\0").filter((entry) => entry.length > 0);
}

function statusEntries(repo: string): string[] {
	return nulTerms(gitText(repo, ["status", "--porcelain=v1", "-z"])).sort();
}

function headTree(repo: string): string {
	return gitText(repo, ["rev-parse", "HEAD"]).trim();
}

function indexTree(repo: string): string {
	return gitText(repo, ["write-tree"]).trim();
}

function indexEntries(repo: string): string {
	return gitText(repo, ["ls-files", "-s", "-z"]);
}

function runCheckpoint(repo: string, args: string[]): GitResult {
	const result = spawnSync("bash", [SCRIPT, ...args], {
		cwd: repo,
		encoding: "utf8",
		env: cleanEnv(),
		timeout: 20_000,
	});
	return { status: result.status, stdout: result.stdout ?? "", stderr: result.stderr ?? "" };
}

function assertRejected(result: GitResult, pattern: RegExp, label: string): void {
	expect(result.status, `${label}: expected non-zero exit\nstdout=${result.stdout}\nstderr=${result.stderr}`).not.toBe(0);
	expect(`${result.stdout}\n${result.stderr}`).toMatch(pattern);
}

function holdCheckpointLock(repo: string): Promise<{ process: ChildProcess; release: () => Promise<void> }> {
	const lockPath = gitText(repo, ["rev-parse", "--git-path", "agent-commit.lock"]).trim();
	const absoluteLockPath = isAbsolute(lockPath) ? lockPath : join(repo, lockPath);
	const holder = spawn(
		"bash",
		[
			"-c",
			'exec 9>"$1" || exit 90; flock -n 9 || exit 91; printf "READY\\n"; cat >/dev/null',
			"bash",
			absoluteLockPath,
		],
		{ stdio: ["pipe", "pipe", "pipe"], env: cleanEnv() },
	);
	children.push(holder);
	const ready = Promise.withResolvers<{ process: ChildProcess; release: () => Promise<void> }>();
	let buffered = "";
	const cleanup = () => {
		holder.stdout?.off("data", onData);
		holder.off("exit", onExit);
	};
	const onData = (chunk: Buffer) => {
		buffered += chunk.toString();
		if (!buffered.includes("READY")) return;
		cleanup();
		ready.resolve({
			process: holder,
			async release() {
				await closeProcess(holder);
			},
		});
	};
	const onExit = (code: number | null) => {
		cleanup();
		ready.reject(new Error(`lock holder exited early (${code}): ${buffered}`));
	};
	holder.stdout?.on("data", onData);
	holder.on("exit", onExit);
	return ready.promise;
}

async function closeProcess(child: ChildProcess): Promise<void> {
	if (child.exitCode !== null) return;
	const exited = Promise.withResolvers<void>();
	child.once("exit", () => exited.resolve());
	child.stdin?.end();
	await exited.promise;
}

afterEach(async () => {
	for (const child of children.splice(0)) {
		if (child.exitCode === null) child.kill("SIGKILL");
	}
	for (const directory of tmpDirs.splice(0)) {
		rmSync(directory, { recursive: true, force: true });
	}
});

describe("scripts/agent-commit.sh", () => {
	test("creates the first commit on an unborn HEAD with unicode and spaced filenames", () => {
		const repo = initRepo({ initialCommit: false });
		write(repo, "tài liệu/ghi chú buổi họp.txt", "nội dung\n");
		write(repo, "src/app main.ts", "export const a = 1;\n");

		const message = "feat(khởi tạo): commit đầu tiên có tên tệp unicode và khoảng trắng";
		const result = runCheckpoint(repo, [message, "--", "tài liệu/ghi chú buổi họp.txt", "src/app main.ts"]);

		expect(result.status, `stderr=${result.stderr}`).toBe(0);
		expect(commitCount(repo)).toBe(1);
		expect(gitText(repo, ["log", "-1", "--format=%s"]).trim()).toBe(message);
		expect(nulTerms(gitText(repo, ["ls-tree", "-r", "-z", "--name-only", "HEAD"])).sort()).toEqual([
			"src/app main.ts",
			"tài liệu/ghi chú buổi họp.txt",
		]);
		expect(statusEntries(repo)).toEqual([]);
	});

	test("is a no-op without creating an empty commit when the named file has no change", () => {
		const repo = initRepo();
		const before = headTree(repo);

		const result = runCheckpoint(repo, ["chore: no-op", "--", "seed.txt"]);

		expect(result.status, `stderr=${result.stderr}`).toBe(0);
		expect(headTree(repo)).toBe(before);
		expect(commitCount(repo)).toBe(1);
		expect(gitText(repo, ["log", "-1", "--format=%s"]).trim()).toBe("initial");
	});

	test("commits only explicit files and leaves unrelated tracked and untracked changes alone", () => {
		const repo = initRepo();
		write(repo, "chosen.txt", "old chosen\n");
		write(repo, "tracked-other.txt", "old other\n");
		write(repo, "user-notes.txt", "old notes\n");
		git(repo, ["add", "--", "chosen.txt", "tracked-other.txt", "user-notes.txt"]);
		git(repo, ["commit", "-q", "-m", "add scope fixtures"]);

		write(repo, "chosen.txt", "new chosen\n");
		write(repo, "tracked-other.txt", "dirty other\n");
		write(repo, "user-notes.txt", "dirty notes\n");
		write(repo, "new-chosen.txt", "new file\n");
		write(repo, "foreign-untracked.txt", "someone else's file\n");
		const before = commitCount(repo);

		const result = runCheckpoint(repo, ["feat: scoped checkpoint", "--", "chosen.txt", "new-chosen.txt"]);

		expect(result.status, `stderr=${result.stderr}`).toBe(0);
		expect(commitCount(repo)).toBe(before + 1);
		expect(nulTerms(gitText(repo, ["diff", "HEAD~1", "HEAD", "--name-only", "-z"])).sort()).toEqual([
			"chosen.txt",
			"new-chosen.txt",
		]);
		const status = statusEntries(repo);
		expect(status).toContain(" M tracked-other.txt");
		expect(status).toContain(" M user-notes.txt");
		expect(status).toContain("?? foreign-untracked.txt");
		expect(status.some((entry) => entry.endsWith("chosen.txt"))).toBe(false);
	});

	test("refuses a pre-existing staged index and preserves it byte-for-byte", () => {
		const repo = initRepo();
		write(repo, "staged.txt", "staged base\n");
		write(repo, "target.txt", "target base\n");
		git(repo, ["add", "--", "staged.txt", "target.txt"]);
		git(repo, ["commit", "-q", "-m", "index fixture"]);

		write(repo, "staged.txt", "someone else's staged change\n");
		git(repo, ["add", "--", "staged.txt"]);
		write(repo, "target.txt", "my unstaged change\n");
		const beforeHead = headTree(repo);
		const beforeIndex = indexTree(repo);
		const beforeStaged = gitText(repo, ["diff", "--cached", "--", "staged.txt"]);

		const result = runCheckpoint(repo, ["feat: should not steal the index", "--", "target.txt"]);

		assertRejected(result, /staged|index/i, "pre-existing staged index");
		expect(headTree(repo)).toBe(beforeHead);
		expect(indexTree(repo)).toBe(beforeIndex);
		expect(gitText(repo, ["diff", "--cached", "--", "staged.txt"])).toBe(beforeStaged);
		expect(gitText(repo, ["diff", "--", "target.txt"])).toContain("my unstaged change");
	});

	test("rejects root, directory, escaping, absolute, wildcard, and pathspec-magic paths without changing state", () => {
		const repo = initRepo();
		write(repo, "dir/nested.txt", "nested\n");
		write(repo, "target.txt", "target base\n");
		write(repo, "sub/target.txt", "sub target\n");
		git(repo, ["add", "--", "dir/nested.txt", "target.txt", "sub/target.txt"]);
		git(repo, ["commit", "-q", "-m", "path fixtures"]);
		write(repo, "target.txt", "dirty target\n");
		const outsideDir = mkdtempSync(join(tmpdir(), "agent-commit-outside-"));
		tmpDirs.push(outsideDir);
		write(outsideDir, "outside.txt", "outside\n");

		const cases: Array<{ args: string[]; pattern: RegExp; label: string }> = [
			{ args: ["feat: root", "--", "."], pattern: /root|director|current|dot/i, label: "root directory" },
			{ args: ["feat: directory", "--", "dir"], pattern: /director|folder|regular file/i, label: "directory" },
			{ args: ["feat: parent escape", "--", "../outside.txt"], pattern: /outside|escape|repository|parent|\.\./i, label: "parent escape" },
			{ args: ["feat: interior dotdot", "--", "sub/../target.txt"], pattern: /\.\.|parent|escape|normali[sz]e/i, label: "interior .." },
			{ args: ["feat: absolute", "--", join(repo, "target.txt")], pattern: /absolute|relative|outside|repository/i, label: "absolute path" },
			{ args: ["feat: wildcard", "--", "*.txt"], pattern: /wildcard|glob|pathspec|magic|literal/i, label: "wildcard" },
			{ args: ["feat: nested wildcard", "--", "sub/*.txt"], pattern: /wildcard|glob|pathspec|magic|literal/i, label: "nested wildcard" },
			{ args: ["feat: pathspec magic", "--", ":(glob)**/*.txt"], pattern: /pathspec|magic|wildcard|literal/i, label: "pathspec magic" },
			{ args: ["feat: git metadata", "--", ".git/config"], pattern: /\.git|metadata|internal/i, label: ".git path" },
		];

		for (const item of cases) {
			const beforeHead = headTree(repo);
			const beforeIndex = indexTree(repo);
			const beforeStatus = statusEntries(repo);
			const result = runCheckpoint(repo, item.args);
			assertRejected(result, item.pattern, item.label);
			expect(headTree(repo), item.label).toBe(beforeHead);
			expect(indexTree(repo), item.label).toBe(beforeIndex);
			expect(statusEntries(repo), item.label).toEqual(beforeStatus);
		}
	}, 20_000);

	test("commits a tracked deletion named explicitly", () => {
		const repo = initRepo();
		write(repo, "gone.txt", "remove me\n");
		write(repo, "stay.txt", "keep me\n");
		git(repo, ["add", "--", "gone.txt", "stay.txt"]);
		git(repo, ["commit", "-q", "-m", "deletion fixture"]);
		rmSync(join(repo, "gone.txt"));
		const before = commitCount(repo);

		const result = runCheckpoint(repo, ["chore: remove obsolete file", "--", "gone.txt"]);

		expect(result.status, `stderr=${result.stderr}`).toBe(0);
		expect(commitCount(repo)).toBe(before + 1);
		expect(nulTerms(gitText(repo, ["diff", "--name-only", "--diff-filter=D", "-z", "HEAD~1", "HEAD"]))).toEqual(["gone.txt"]);
		expect(existsSync(join(repo, "gone.txt"))).toBe(false);
		expect(statusEntries(repo)).toEqual([]);
	});

	test("fails fast without touching the index while another checkpoint holds the lock", async () => {
		const repo = initRepo();
		write(repo, "target.txt", "target base\n");
		git(repo, ["add", "--", "target.txt"]);
		git(repo, ["commit", "-q", "-m", "lock fixture"]);
		write(repo, "target.txt", "dirty target\n");
		const beforeHead = headTree(repo);
		const beforeIndex = indexTree(repo);

		const lock = await holdCheckpointLock(repo);
		try {
			const result = runCheckpoint(repo, ["feat: concurrent checkpoint", "--", "target.txt"]);
			assertRejected(result, /lock|busy|concurrent|another/i, "lock contention");
			expect(headTree(repo)).toBe(beforeHead);
			expect(indexTree(repo)).toBe(beforeIndex);
			expect(gitText(repo, ["diff", "--", "target.txt"])).toContain("dirty target");
		} finally {
			await lock.release();
		}
	}, 20_000);

	test("refuses to commit while a merge conflict is unresolved", () => {
		const repo = initRepo();
		write(repo, "conflict.txt", "base\n");
		git(repo, ["add", "--", "conflict.txt"]);
		git(repo, ["commit", "-q", "-m", "conflict base"]);
		git(repo, ["checkout", "-q", "-b", "other"]);
		write(repo, "conflict.txt", "other side\n");
		git(repo, ["commit", "-q", "-am", "other change"]);
		git(repo, ["checkout", "-q", "main"]);
		write(repo, "conflict.txt", "main side\n");
		git(repo, ["commit", "-q", "-am", "main change"]);
		const merge = git(repo, ["merge", "other"], false);
		expect(merge.status).not.toBe(0);
		const beforeHead = headTree(repo);
		const beforeIndex = indexEntries(repo);

		const result = runCheckpoint(repo, ["feat: must not commit a conflict", "--", "conflict.txt"]);

		assertRejected(result, /staged|index|conflict|merge|rebase|unmerged/i, "merge conflict");
		expect(headTree(repo)).toBe(beforeHead);
		expect(indexEntries(repo)).toBe(beforeIndex);
	}, 20_000);
});

const HOOK_FILE = join(REPO_ROOT, ".omp", "hooks", "post", "commit-checkpoint.ts");
let hookModule: { default: (pi: unknown) => unknown } | undefined;

async function loadHookModule(): Promise<{ default: (pi: unknown) => unknown }> {
	hookModule ??= (await import(pathToFileURL(HOOK_FILE).href)) as { default: (pi: unknown) => unknown };
	return hookModule;
}

type JsonObject = Record<string, unknown>;
type HookHandler = (event: JsonObject, ctx: JsonObject) => unknown;
type CommandHandler = (args: string, ctx: JsonObject) => Promise<void> | void;

interface HookHarness {
	sessionId: string;
	handlers: Map<string, HookHandler[]>;
	commands: Map<string, { description?: string; handler: CommandHandler }>;
	notifications: Array<{ text: string; level?: string }>;
	messages: unknown[];
	execCalls: Array<{ command: string; args: string[]; cwd: string }>;
}

let hookSessionCounter = 0;

function hookContext(repo: string, harness: HookHarness): JsonObject {
	return {
		cwd: repo,
		sessionManager: { getSessionId: () => harness.sessionId },
		mode: "tui",
		hasUI: false,
		ui: {
			notify(text: string, level?: string) {
				harness.notifications.push({ text, level });
			},
			setStatus() {},
			confirm: async () => false,
			select: async () => undefined,
			input: async () => undefined,
			editor: async () => undefined,
		},
		isIdle: () => true,
		hasPendingMessages: () => false,
		abort() {},
		async shutdown() {},
	};
}

function commandContext(repo: string, harness: HookHarness): JsonObject {
	return {
		...hookContext(repo, harness),
		async waitForIdle() {},
		async newSession() {
			return { cancelled: true };
		},
		async branch() {
			return { cancelled: true };
		},
		async navigateTree() {
			return { cancelled: true };
		},
		async switchSession() {
			return { cancelled: true };
		},
		async reload() {},
	};
}

async function installCommitHook(repo: string): Promise<{ harness: HookHarness; ctx: JsonObject }> {
	const harness: HookHarness = {
		sessionId: `hook-session-${++hookSessionCounter}`,
		handlers: new Map(),
		commands: new Map(),
		notifications: [],
		messages: [],
		execCalls: [],
	};
	const pi = {
		on(event: string, handler: HookHandler) {
			const registered = harness.handlers.get(event) ?? [];
			registered.push(handler);
			harness.handlers.set(event, registered);
		},
		registerCommand(name: string, options: { description?: string; handler: CommandHandler }) {
			harness.commands.set(name, options);
		},
		setLabel() {},
		logger: { debug() {}, info() {}, warn() {}, error() {} },
		sendMessage(message: unknown) {
			harness.messages.push(message);
		},
		appendEntry(entry: unknown) {
			harness.messages.push(entry);
		},
		async exec(command: string, args: string[], options: { cwd?: string; timeout?: number } = {}) {
			const cwd = options.cwd ?? repo;
			harness.execCalls.push({ command, args: [...args], cwd });
			const result = spawnSync(command, args, {
				cwd,
				encoding: "utf8",
				env: cleanEnv(),
				timeout: options.timeout,
			});
			return {
				stdout: result.stdout ?? "",
				stderr: result.stderr ?? "",
				code: result.status ?? (result.error ? -1 : 0),
				killed: result.signal !== null,
			};
		},
	};
	const module = await loadHookModule();
	expect(typeof module.default).toBe("function");
	await module.default(pi);
	return { harness, ctx: hookContext(repo, harness) };
}

async function emitHook(harness: HookHarness, event: JsonObject, ctx: JsonObject): Promise<unknown> {
	let latest: unknown;
	for (const handler of harness.handlers.get(String(event.type)) ?? []) {
		const result = await handler(event, ctx);
		if (typeof result === "object" && result !== null) latest = result;
	}
	return latest;
}

function stopBlocks(result: unknown): boolean {
	if (typeof result !== "object" || result === null) return false;
	return (
		("decision" in result && result.decision === "block") ||
		("continue" in result && result.continue === true)
	);
}

function reminderText(harness: HookHarness, result: unknown): string {
	const pieces: string[] = [];
	if (typeof result === "object" && result !== null && "additionalContext" in result) {
		if (typeof result.additionalContext === "string") pieces.push(result.additionalContext);
	}
	const content = typeof result === "object" && result !== null && "content" in result ? result.content : undefined;
	for (const chunk of Array.isArray(content) ? content : []) {
		if (typeof chunk === "object" && chunk !== null && "type" in chunk && chunk.type === "text") {
			if ("text" in chunk && typeof chunk.text === "string") pieces.push(chunk.text);
		}
	}
	for (const message of harness.messages) {
		pieces.push(typeof message === "string" ? message : JSON.stringify(message));
	}
	for (const notification of harness.notifications) pieces.push(notification.text);
	return pieces.join("\n");
}

function toolResultEvent(toolName: string, input: Record<string, unknown>, isError = false): JsonObject {
	return {
		type: "tool_result",
		toolName,
		toolCallId: `call-${toolName}-${isError ? "error" : "ok"}`,
		input,
		content: [{ type: "text", text: isError ? "tool failed" : "tool succeeded" }],
		details: undefined,
		isError,
	};
}

function stopEvent(harness: HookHarness): JsonObject {
	return {
		type: "session_stop",
		messages: [],
		turn_id: 1,
		session_id: harness.sessionId,
		stop_hook_active: false,
		signal: new AbortController().signal,
	};
}

async function stopNow(harness: HookHarness, ctx: JsonObject): Promise<boolean> {
	return stopBlocks(await emitHook(harness, stopEvent(harness), ctx));
}

describe(".omp/hooks/post/commit-checkpoint.ts", () => {
	test("reminds on a successful edit and blocks session_stop until the observed file is committed", async () => {
		const repo = initRepo();
		write(repo, "src/a.ts", "base\n");
		git(repo, ["add", "--", "src/a.ts"]);
		git(repo, ["commit", "-q", "-m", "edit fixture"]);
		write(repo, "src/a.ts", "changed by the agent\n");
		const { harness, ctx } = await installCommitHook(repo);

		const editResult = await emitHook(
			harness,
			toolResultEvent("edit", { path: "src/a.ts", input: "replace a line" }),
			ctx,
		);
		expect(reminderText(harness, editResult)).toMatch(/checkpoint|commit/i);
		expect(await stopNow(harness, ctx)).toBe(true);

		const commit = runCheckpoint(repo, ["feat: commit observed edit", "--", "src/a.ts"]);
		expect(commit.status, `stderr=${commit.stderr}`).toBe(0);
		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("allows session_stop once an observed dirty file is restored", async () => {
		const repo = initRepo();
		write(repo, "src/restored.ts", "base\n");
		git(repo, ["add", "--", "src/restored.ts"]);
		git(repo, ["commit", "-q", "-m", "restore fixture"]);
		write(repo, "src/restored.ts", "temporary agent edit\n");
		const { harness, ctx } = await installCommitHook(repo);

		await emitHook(harness, toolResultEvent("edit", { path: "src/restored.ts" }), ctx);
		expect(await stopNow(harness, ctx)).toBe(true);

		git(repo, ["restore", "--", "src/restored.ts"]);
		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("ignores dirty tracked and untracked files it never observed", async () => {
		const repo = initRepo();
		write(repo, "src/observed.ts", "base\n");
		write(repo, "user-notes.txt", "user base\n");
		git(repo, ["add", "--", "src/observed.ts", "user-notes.txt"]);
		git(repo, ["commit", "-q", "-m", "scope fixture"]);
		write(repo, "src/observed.ts", "agent change\n");
		const { harness, ctx } = await installCommitHook(repo);

		await emitHook(harness, toolResultEvent("edit", { path: "src/observed.ts" }), ctx);
		const commit = runCheckpoint(repo, ["feat: commit observed", "--", "src/observed.ts"]);
		expect(commit.status, `stderr=${commit.stderr}`).toBe(0);

		write(repo, "user-notes.txt", "user's own later edit\n");
		write(repo, "user-untracked.txt", "not the agent's file\n");
		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("never observes xd:// URLs, read tools, or failed tool results", async () => {
		const repo = initRepo();
		write(repo, "src/a.ts", "base\n");
		git(repo, ["add", "--", "src/a.ts"]);
		git(repo, ["commit", "-q", "-m", "ignore fixture"]);
		write(repo, "src/a.ts", "uncommitted\n");
		const { harness, ctx } = await installCommitHook(repo);

		await emitHook(harness, toolResultEvent("write", { path: "xd://report_issue" }), ctx);
		await emitHook(harness, toolResultEvent("read", { path: "src/a.ts" }), ctx);
		await emitHook(harness, toolResultEvent("edit", { path: "src/a.ts" }, true), ctx);
		await emitHook(harness, toolResultEvent("write", { path: "src/a.ts" }, true), ctx);

		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("tracks a write-created file until it is committed", async () => {
		const repo = initRepo();
		const { harness, ctx } = await installCommitHook(repo);

		write(repo, "src/new.ts", "created by write\n");
		await emitHook(harness, toolResultEvent("write", { path: "src/new.ts" }), ctx);
		expect(await stopNow(harness, ctx)).toBe(true);

		const creation = runCheckpoint(repo, ["feat: add observed file", "--", "src/new.ts"]);
		expect(creation.status, `stderr=${creation.stderr}`).toBe(0);
		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("blocks session_stop when an observed tracked file is deleted", async () => {
		const repo = initRepo();
		write(repo, "src/doomed.ts", "tracked\n");
		git(repo, ["add", "--", "src/doomed.ts"]);
		git(repo, ["commit", "-q", "-m", "deletion observation fixture"]);
		const { harness, ctx } = await installCommitHook(repo);

		await emitHook(harness, toolResultEvent("write", { path: "src/doomed.ts" }), ctx);
		rmSync(join(repo, "src/doomed.ts"));
		expect(await stopNow(harness, ctx)).toBe(true);

		const deletion = runCheckpoint(repo, ["chore: remove observed file", "--", "src/doomed.ts"]);
		expect(deletion.status, `stderr=${deletion.stderr}`).toBe(0);
		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("tracks every path of a multi-file edit and keeps blocking while any observed path is dirty", async () => {
		const repo = initRepo();
		write(repo, "src/a.ts", "a base\n");
		write(repo, "src/b.ts", "b base\n");
		git(repo, ["add", "--", "src/a.ts", "src/b.ts"]);
		git(repo, ["commit", "-q", "-m", "multi fixture"]);
		write(repo, "src/a.ts", "a changed\n");
		write(repo, "src/b.ts", "b changed\n");
		const { harness, ctx } = await installCommitHook(repo);

		await emitHook(
			harness,
			toolResultEvent("edit", { input: "[src/a.ts#abcd]\n[edit]\n[src/b.ts#1234]", paths: ["src/a.ts", "src/b.ts"] }),
			ctx,
		);
		expect(await stopNow(harness, ctx)).toBe(true);

		const first = runCheckpoint(repo, ["feat: commit first path", "--", "src/a.ts"]);
		expect(first.status, `stderr=${first.stderr}`).toBe(0);
		expect(await stopNow(harness, ctx)).toBe(true);

		const second = runCheckpoint(repo, ["feat: commit second path", "--", "src/b.ts"]);
		expect(second.status, `stderr=${second.stderr}`).toBe(0);
		expect(await stopNow(harness, ctx)).toBe(false);
	});

	test("checkpoint command forwards an exact argv to the script without shell interpolation", async () => {
		const repo = initRepo();
		write(repo, "src/a b.ts", "space file\n");
		write(repo, "tài liệu/ü.txt", "unicode file\n");
		const { harness } = await installCommitHook(repo);
		const command = harness.commands.get("checkpoint");
		expect(command).toBeDefined();

		const message = "feat(auth): sửa đăng nhập $(literal) (#7)";
		await command!.handler(`${message} -- "src/a b.ts" "tài liệu/ü.txt"`, commandContext(repo, harness));

		expect(harness.execCalls).toHaveLength(1);
		const [call] = harness.execCalls;
		expect(call.command).toBe("bash");
		expect(call.args[0].endsWith(join("scripts", "agent-commit.sh"))).toBe(true);
		expect(call.args.slice(1)).toEqual([message, "--", "src/a b.ts", "tài liệu/ü.txt"]);
		expect(call.cwd).toBe(repo);
		expect(existsSync(join(repo, "pwned"))).toBe(false);
		expect(commitCount(repo)).toBe(2);
		expect(gitText(repo, ["log", "-1", "--format=%s"]).trim()).toBe(message);
		expect(nulTerms(gitText(repo, ["ls-files", "-z", "--", "src/a b.ts", "tài liệu/ü.txt"])).sort()).toEqual([
			"src/a b.ts",
			"tài liệu/ü.txt",
		]);
	}, 20_000);

	test("checkpoint command refuses malformed arguments without invoking the script", async () => {
		const repo = initRepo();
		const { harness } = await installCommitHook(repo);
		const command = harness.commands.get("checkpoint");
		expect(command).toBeDefined();
		const ctx = commandContext(repo, harness);

		await command!.handler("feat: missing separator", ctx);
		await command!.handler("feat: no files --", ctx);

		expect(harness.execCalls).toHaveLength(0);
		expect(reminderText(harness, undefined)).toMatch(/usage|--|file/i);
	});
});
