package main

import (
	"encoding/base64"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

const testRepoDir = "/tmp/test_repo"

func TestGitUntrackedFileStatusWithSpaces(t *testing.T) {
	fileName := "0 - readme (1).md"
	fileStatus, e := checkStatus(func(_ *Git) error {
		editFile(fileName, "# sample")
		return nil
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != fileName {
		t.Errorf("Expecting '"+fileName+"' file. Got: %+v", fileStatus)
		return
	}

	if fileStatus.Status != StatusUntracked {
		t.Errorf("Expecting untracked file. Got: %+v", fileStatus)
		return
	}
}

func TestGitNewFileStatusWithCyrillicName(t *testing.T) {
	fileName := "Без названия 1.canvas"
	fileStatus, e := checkStatus(func(g *Git) error {
		return stageFile(g, fileName, "canvas content")
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != fileName {
		t.Errorf("Expecting '%s' file. Got: %+v", fileName, fileStatus)
		return
	}

	if fileStatus.Status != StatusNew {
		t.Errorf("Expecting new file. Got: %+v", fileStatus)
		return
	}
}

func TestGitRollbackCyrillicNewFile(t *testing.T) {
	fileName := "Без названия.canvas"
	e := initRepo()
	if e != nil {
		t.Fatal(e)
	}

	g := NewGit(testRepoDir, "")
	if e := stageFile(g, fileName, "canvas content"); e != nil {
		t.Fatal(e)
	}

	if e := g.Rollback(&FileRollback{Path: fileName}); e != nil {
		t.Fatal(e)
	}

	s, e := g.Status()
	if e != nil {
		t.Fatal(e)
	}
	if len(s.Files) != 0 {
		t.Errorf("Expected clean status after rollback. Got: %+v", s.Files)
	}

	if _, statErr := os.Stat(filepath.Join(testRepoDir, fileName)); !os.IsNotExist(statErr) {
		t.Errorf("Expected file to be removed after rollback. stat err: %v", statErr)
	}
}

func TestGitRollbackOctalEscapedPath(t *testing.T) {
	fileName := "Без названия 1.canvas"
	escapedPath := `\320\221\320\265\320\267 \320\275\320\260\320\267\320\262\320\260\320\275\320\270\321\217 1.canvas`
	e := initRepo()
	if e != nil {
		t.Fatal(e)
	}

	g := NewGit(testRepoDir, "")
	if e := stageFile(g, fileName, "canvas content"); e != nil {
		t.Fatal(e)
	}

	if e := g.Rollback(&FileRollback{Path: escapedPath}); e != nil {
		t.Fatal(e)
	}

	s, e := g.Status()
	if e != nil {
		t.Fatal(e)
	}
	if len(s.Files) != 0 {
		t.Errorf("Expected clean status after rollback of escaped path. Got: %+v", s.Files)
	}
}

func TestUnquoteGitPath(t *testing.T) {
	input := `\320\221\320\265\320\267 \320\275\320\260\320\267\320\262\320\260\320\275\320\270\321\217 1.canvas`
	expected := "Без названия 1.canvas"
	if got := unquoteGitPath(input); got != expected {
		t.Errorf("unquoteGitPath() = %q, want %q", got, expected)
	}
}

func TestGitStageGitignoredFile(t *testing.T) {
	e := initRepo()
	if e != nil {
		t.Fatal(e)
	}

	s := &Shell{testRepoDir}
	s.StrictExecute("echo '.obsidian/' >> .gitignore")
	s.StrictExecute("git add .gitignore")
	s.StrictExecute("git commit -m 'ignore obsidian'")

	g := NewGit(testRepoDir, "")
	if e := stageFile(g, ".obsidian/app.json", "{\"version\":1}"); e == nil {
		t.Fatal("expected staging gitignored file to fail")
	}

	status, e := g.Status()
	if e != nil {
		t.Fatal(e)
	}
	for _, f := range status.Files {
		if f.Path == ".obsidian/app.json" {
			t.Fatalf("gitignored file should not be staged, got: %+v", f)
		}
	}
}

func TestGitQuotedFileWithSpacesStatus(t *testing.T) {
	fileName := "\"quoted file\""
	fileStatus, e := checkStatus(func(g *Git) error {
		return stageFile(g, fileName, "content")
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != "quoted file" {
		t.Errorf("Expecting quoted file name. Got: '%s' (%+v)", fileStatus.Path, fileStatus)
		return
	}
}

func TestGitNewFileStatusWithSpaces(t *testing.T) {
	fileName := "0 - readme (1).md"
	fileStatus, e := checkStatus(func(g *Git) error {
		stageFile(g, fileName, "init")
		return nil
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != fileName {
		t.Errorf("Expecting '"+fileName+"' file. Got: %+v", fileStatus)
		return
	}

	if fileStatus.Status != StatusNew {
		t.Errorf("Expecting new file. Got: %+v", fileStatus)
		return
	}
}

func TestGitModifiedFileStatus(t *testing.T) {
	fileName := "modify.md"
	fileStatus, e := checkStatus(func(g *Git) error {
		if e := stageFile(g, fileName, "# sample"); e != nil {
			return e
		}
		_, e := g.Commit(&Commitment{
			Message: "okay",
		})

		if e != nil {
			return e
		}

		editFile(fileName, "modification")
		return nil
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != fileName {
		t.Errorf("Expecting '"+fileName+"' file. Got: %+v", fileStatus)
		return
	}

	if fileStatus.Status != StatusModified {
		t.Errorf("Expecting modified file. Got: %+v", fileStatus)
		return
	}
}

func TestGitModifiedFileStatusWhenStaged(t *testing.T) {
	fileName := "modify.md"
	fileStatus, e := checkStatus(func(g *Git) error {
		if e := stageFile(g, fileName, "# sample"); e != nil {
			return e
		}
		_, e := g.Commit(&Commitment{
			Message: "okay",
		})

		if e != nil {
			return e
		}

		return stageFile(g, fileName, "staging")
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != fileName {
		t.Errorf("Expecting '"+fileName+"' file. Got: %+v", fileStatus)
		return
	}

	if fileStatus.Status != StatusModified {
		t.Errorf("Expecting modified file. Got: %+v", fileStatus)
		return
	}
}

func checkStatus(action func(g *Git) error) (*FileStatus, error) {
	e := initRepo()
	if e != nil {
		return nil, e
	}

	g := NewGit(testRepoDir, "")
	e = action(g)
	if e != nil {
		return nil, e
	}

	s, e := g.Status()

	if e != nil {
		return nil, e
	}

	if len(s.Files) != 1 {
		return nil, errors.New(fmt.Sprintf("Expecting only 1 file. Got: %+v", s.Files))
	}

	return s.Files[0], nil
}

func TestGitStatusDiff(t *testing.T) {
	err := initRepo()
	if err != nil {
		t.Error(err)
		return
	}

	g := NewGit(testRepoDir, "")
	editFile("README.md", "")
	e := g.Stage(&FileContent{
		Path:    "README.md",
		Content: base64.StdEncoding.EncodeToString([]byte("# sample")),
	})

	if e != nil {
		t.Error(e)
		return
	}

	_, e = g.Commit(&Commitment{
		Message: "add readme.md",
	})

	if e != nil {
		t.Error(e)
		return
	}

	e = g.Stage(&FileContent{
		Path:    "README.md",
		Content: base64.StdEncoding.EncodeToString([]byte("# diff")),
	})

	if e != nil {
		t.Error(e)
		return
	}
	s, e := g.Status()

	if e != nil {
		t.Error(e)
	}

	if len(s.Files) != 1 {
		t.Errorf("Expecting only 1 file. Got: %+v", s.Files)
		return
	}

	file := s.Files[0]
	if file.Path != "README.md" {
		t.Errorf("Expecting 'README.md' file. Got: %+v", file)
		return
	}

	if file.Status != StatusModified {
		t.Errorf("Expecting modified file. Got: %+v", file)
		return
	}

	if !strings.Contains(file.Diff, "# diff") {
		t.Errorf("Expecting file contain diff. Got: %+v", file)
		return
	}
}

func editFile(path string, content string) {
	s := &Shell{testRepoDir}
	s.StrictExecute("echo '" + content + "' > '" + path + "'")
}

func stageFile(g *Git, path string, content string) error {
	return g.Stage(&FileContent{
		Path:    path,
		Content: base64.StdEncoding.EncodeToString([]byte(content)),
	})
}

func initRepo() error {
	s := &Shell{"/tmp"}

	if e := os.RemoveAll(testRepoDir); e != nil {
		return e
	}

	if stderr, e := s.Execute("mkdir " + testRepoDir); e != nil {
		return errors.New(e.Error() + "std err: " + stderr)
	}

	s = &Shell{testRepoDir}
	if stderr, e := s.Execute("git init"); e != nil {
		return errors.New(e.Error() + "std err: " + stderr)
	}

	if stderr, e := s.Execute("git config --local user.email \"you@example.com\""); e != nil {
		return errors.New(e.Error() + "std err: " + stderr)
	}

	if stderr, e := s.Execute("git config --local user.name \"Tester\""); e != nil {
		return errors.New(e.Error() + "std err: " + stderr)
	}

	return nil
}

func TestTryCloneRecoversFromInvalidGitDir(t *testing.T) {
	bareRepo := "/tmp/test_bare_try_clone.git"
	targetDir := "/tmp/test_try_clone_target"
	initDir := "/tmp/test_try_clone_init"

	defer os.RemoveAll(bareRepo)
	defer os.RemoveAll(targetDir)
	defer os.RemoveAll(initDir)

	initShell := &Shell{"/tmp"}
	initShell.StrictExecute("rm -rf " + initDir + " " + bareRepo + " " + targetDir)
	initShell.StrictExecute("mkdir -p " + initDir)
	initShell = &Shell{initDir}
	initShell.StrictExecute("git init")
	initShell.StrictExecute("git config user.email \"test@mail.com\"")
	initShell.StrictExecute("git config user.name \"Tester\"")
	initShell.StrictExecute("echo '# test' > README.md")
	initShell.StrictExecute("git add README.md")
	initShell.StrictExecute("git commit -m \"Initial Commit\"")
	initShell.StrictExecute("git clone --bare .git " + bareRepo)

	if e := os.MkdirAll(targetDir+"/.git", 0755); e != nil {
		t.Fatal(e)
	}

	g := NewGit(targetDir, bareRepo)
	g.TryClone("")

	if _, err := g.shell.Execute("git rev-parse --git-dir"); err != nil {
		t.Fatalf("expected valid repo after TryClone, got: %v", err)
	}
}

func TestTryCloneRunsCmdAfterCloneInRepoRoot(t *testing.T) {
	withAfterCloneStateFile(t, func(stateFile string) {
		bareRepo := "/tmp/test_bare_after_clone.git"
		targetDir := "/tmp/test_after_clone_target"
		initDir := "/tmp/test_after_clone_init"
		markerFile := "after_clone_ran.txt"

		defer os.RemoveAll(bareRepo)
		defer os.RemoveAll(targetDir)
		defer os.RemoveAll(initDir)

		initShell := &Shell{"/tmp"}
		initShell.StrictExecute("rm -rf " + initDir + " " + bareRepo + " " + targetDir)
		initShell.StrictExecute("mkdir -p " + initDir)
		initShell = &Shell{initDir}
		initShell.StrictExecute("git init")
		initShell.StrictExecute("git config user.email \"test@mail.com\"")
		initShell.StrictExecute("git config user.name \"Tester\"")
		initShell.StrictExecute("echo '# test' > README.md")
		initShell.StrictExecute("git add README.md")
		initShell.StrictExecute("git commit -m \"Initial Commit\"")
		initShell.StrictExecute("git clone --bare .git " + bareRepo)

		g := NewGit(targetDir, bareRepo)
		g.TryClone("touch " + markerFile)

		markerPath := filepath.Join(targetDir, markerFile)
		if _, err := os.Stat(markerPath); err != nil {
			t.Fatalf("expected after-clone command to create %s in repo root, got: %v", markerPath, err)
		}

		// Second call should not re-run the command for an already-cloned repo.
		if err := os.Remove(markerPath); err != nil {
			t.Fatal(err)
		}
		g.TryClone("touch " + markerFile)
		if _, err := os.Stat(markerPath); !os.IsNotExist(err) {
			t.Fatalf("expected after-clone command to be skipped when repo already exists")
		}
	})
}

func TestTryCloneRunsCmdAfterCloneForExistingRepoInNewContainer(t *testing.T) {
	withAfterCloneStateFile(t, func(stateFile string) {
		bareRepo := "/tmp/test_bare_after_clone_existing.git"
		targetDir := "/tmp/test_after_clone_existing_target"
		initDir := "/tmp/test_after_clone_existing_init"
		markerFile := "after_clone_ran.txt"

		defer os.RemoveAll(bareRepo)
		defer os.RemoveAll(targetDir)
		defer os.RemoveAll(initDir)

		initShell := &Shell{"/tmp"}
		initShell.StrictExecute("rm -rf " + initDir + " " + bareRepo + " " + targetDir)
		initShell.StrictExecute("mkdir -p " + initDir)
		initShell = &Shell{initDir}
		initShell.StrictExecute("git init")
		initShell.StrictExecute("git config user.email \"test@mail.com\"")
		initShell.StrictExecute("git config user.name \"Tester\"")
		initShell.StrictExecute("echo '# test' > README.md")
		initShell.StrictExecute("git add README.md")
		initShell.StrictExecute("git commit -m \"Initial Commit\"")
		initShell.StrictExecute("git clone --bare .git " + bareRepo)
		initShell.StrictExecute("git clone " + bareRepo + " " + targetDir)

		g := NewGit(targetDir, bareRepo)
		g.TryClone("touch " + markerFile)

		markerPath := filepath.Join(targetDir, markerFile)
		if _, err := os.Stat(markerPath); err != nil {
			t.Fatalf("expected after-clone command to run for existing repo in new container, got: %v", err)
		}
	})
}

// withConflictingRemote builds a clone whose local commit and the remote's
// commit both touch the same file, so any rebase between them conflicts.
// It returns the Git of the clone and the path to the bare remote.
func withConflictingRemote(t *testing.T, name string) (*Git, string) {
	t.Helper()

	bareRepo := "/tmp/test_bare_" + name + ".git"
	originDir := "/tmp/test_origin_" + name
	cloneDir := "/tmp/test_clone_" + name

	t.Cleanup(func() {
		os.RemoveAll(bareRepo)
		os.RemoveAll(originDir)
		os.RemoveAll(cloneDir)
	})

	tmp := &Shell{"/tmp"}
	tmp.StrictExecute("rm -rf " + originDir + " " + bareRepo + " " + cloneDir)
	tmp.StrictExecute("mkdir -p " + originDir)

	origin := &Shell{originDir}
	origin.StrictExecute("git init")
	origin.StrictExecute("git config user.email \"test@mail.com\"")
	origin.StrictExecute("git config user.name \"Tester\"")
	origin.StrictExecute("git checkout -B master")
	origin.StrictExecute("echo 'base' > README.md")
	origin.StrictExecute("git add README.md")
	origin.StrictExecute("git commit -m \"Initial Commit\"")
	origin.StrictExecute("git clone --bare .git " + bareRepo)

	tmp.StrictExecute("git clone " + bareRepo + " " + cloneDir)
	clone := &Shell{cloneDir}
	clone.StrictExecute("git config user.email \"wiki@mail.com\"")
	clone.StrictExecute("git config user.name \"Wiki Committer\"")

	// Diverge: the remote and the clone edit the same line of the same file.
	origin.StrictExecute("git remote add bare " + bareRepo + " || true")
	origin.StrictExecute("echo 'by remote' > README.md")
	origin.StrictExecute("git add README.md")
	origin.StrictExecute("git commit -m \"remote edit\"")
	origin.StrictExecute("git push bare master")

	clone.StrictExecute("echo 'by wiki' > README.md")
	clone.StrictExecute("git add README.md")
	clone.StrictExecute("git commit -m \"local edit\"")

	return NewGit(cloneDir, bareRepo), bareRepo
}

func TestRebaseConflictIsPreservedAtRemoteBranch(t *testing.T) {
	g, bareRepo := withConflictingRemote(t, "rebase_conflict")

	localHead, headErr := g.execute("git rev-parse HEAD")
	if headErr != nil {
		t.Fatal(headErr)
	}

	err := g.Rebase()

	var conflict *ConflictError
	if !errors.As(err, &conflict) {
		t.Fatalf("expected a ConflictError, got: %v", err)
	}

	if !strings.HasPrefix(conflict.Branch, CONFLICT_BRANCH_PREFIX) {
		t.Fatalf("expected branch prefixed with '%s', got: '%s'", CONFLICT_BRANCH_PREFIX, conflict.Branch)
	}

	// The conflicting work must be reachable at the remote...
	refs, refsErr := (&Shell{"/tmp"}).Execute("git ls-remote " + bareRepo)
	if refsErr != nil {
		t.Fatal(refsErr)
	}
	if !strings.Contains(refs, conflict.Branch) {
		t.Fatalf("expected '%s' at the remote, got refs:\n%s", conflict.Branch, refs)
	}
	if !strings.Contains(refs, strings.TrimSpace(localHead)) {
		t.Fatalf("expected the local commit '%s' at the remote, got refs:\n%s",
			strings.TrimSpace(localHead), refs)
	}

	// ...and the local repo must be unblocked, sitting exactly on the remote.
	head, headErr := g.execute("git rev-parse HEAD")
	if headErr != nil {
		t.Fatal(headErr)
	}
	remoteHead, remoteErr := g.execute("git rev-parse origin/master")
	if remoteErr != nil {
		t.Fatal(remoteErr)
	}
	if strings.TrimSpace(head) != strings.TrimSpace(remoteHead) {
		t.Fatalf("expected HEAD to match origin/master after a conflict, got '%s' vs '%s'",
			strings.TrimSpace(head), strings.TrimSpace(remoteHead))
	}

	if g.isRebaseInProgress() {
		t.Fatal("expected no rebase left in progress")
	}
}

func TestRebaseConflictPreservesUncommittedChanges(t *testing.T) {
	g, bareRepo := withConflictingRemote(t, "rebase_conflict_dirty")

	// An unstaged edit on top of the conflicting commit: Rebase() wraps it into
	// a temporary commit, which must reach the conflict branch with a real message.
	(&Shell{g.repoDir}).StrictExecute("echo 'not yet committed' > notes.md")

	err := g.Rebase()

	var conflict *ConflictError
	if !errors.As(err, &conflict) {
		t.Fatalf("expected a ConflictError, got: %v", err)
	}

	log, logErr := (&Shell{"/tmp"}).Execute(
		"git --git-dir=" + bareRepo + " log -1 --pretty=%s " + conflict.Branch)
	if logErr != nil {
		t.Fatal(logErr)
	}
	if strings.TrimSpace(log) != conflictAmendMessage {
		t.Fatalf("expected the preserved commit to be re-worded to '%s', got: '%s'",
			conflictAmendMessage, strings.TrimSpace(log))
	}

	files, filesErr := (&Shell{"/tmp"}).Execute(
		"git --git-dir=" + bareRepo + " ls-tree --name-only " + conflict.Branch)
	if filesErr != nil {
		t.Fatal(filesErr)
	}
	if !strings.Contains(files, "notes.md") {
		t.Fatalf("expected uncommitted work at the conflict branch, got:\n%s", files)
	}
}

func TestRebaseWithoutConflictStillSucceeds(t *testing.T) {
	bareRepo := "/tmp/test_bare_no_conflict.git"
	originDir := "/tmp/test_origin_no_conflict"
	cloneDir := "/tmp/test_clone_no_conflict"

	defer os.RemoveAll(bareRepo)
	defer os.RemoveAll(originDir)
	defer os.RemoveAll(cloneDir)

	tmp := &Shell{"/tmp"}
	tmp.StrictExecute("rm -rf " + originDir + " " + bareRepo + " " + cloneDir)
	tmp.StrictExecute("mkdir -p " + originDir)

	origin := &Shell{originDir}
	origin.StrictExecute("git init")
	origin.StrictExecute("git config user.email \"test@mail.com\"")
	origin.StrictExecute("git config user.name \"Tester\"")
	origin.StrictExecute("git checkout -B master")
	origin.StrictExecute("echo 'base' > README.md")
	origin.StrictExecute("git add README.md")
	origin.StrictExecute("git commit -m \"Initial Commit\"")
	origin.StrictExecute("git clone --bare .git " + bareRepo)

	tmp.StrictExecute("git clone " + bareRepo + " " + cloneDir)
	clone := &Shell{cloneDir}
	clone.StrictExecute("git config user.email \"wiki@mail.com\"")
	clone.StrictExecute("git config user.name \"Wiki Committer\"")

	// Different files on each side: this rebases cleanly.
	origin.StrictExecute("git remote add bare " + bareRepo + " || true")
	origin.StrictExecute("echo 'remote only' > remote.md")
	origin.StrictExecute("git add remote.md")
	origin.StrictExecute("git commit -m \"remote edit\"")
	origin.StrictExecute("git push bare master")

	clone.StrictExecute("echo 'local only' > local.md")
	clone.StrictExecute("git add local.md")
	clone.StrictExecute("git commit -m \"local edit\"")

	g := NewGit(cloneDir, bareRepo)
	if err := g.Rebase(); err != nil {
		t.Fatalf("expected a clean rebase, got: %v", err)
	}

	refs, refsErr := (&Shell{"/tmp"}).Execute("git ls-remote " + bareRepo)
	if refsErr != nil {
		t.Fatal(refsErr)
	}
	if strings.Contains(refs, CONFLICT_BRANCH_PREFIX) {
		t.Fatalf("expected no conflict branch after a clean rebase, got refs:\n%s", refs)
	}
}

func TestConflictBranchesAreListedFromRemote(t *testing.T) {
	g, _ := withConflictingRemote(t, "conflict_listing")

	branches, e := g.ConflictBranches()
	if e != nil {
		t.Fatal(e)
	}
	if len(branches) != 0 {
		t.Fatalf("expected no conflict branches before a conflict, got: %v", branches)
	}

	err := g.Rebase()
	var conflict *ConflictError
	if !errors.As(err, &conflict) {
		t.Fatalf("expected a ConflictError, got: %v", err)
	}

	branches, e = g.ConflictBranches()
	if e != nil {
		t.Fatal(e)
	}
	if len(branches) != 1 || branches[0] != conflict.Branch {
		t.Fatalf("expected exactly ['%s'], got: %v", conflict.Branch, branches)
	}

	status, statusErr := g.Status()
	if statusErr != nil {
		t.Fatal(statusErr)
	}
	if len(status.ConflictBranches) != 1 || status.ConflictBranches[0] != conflict.Branch {
		t.Fatalf("expected status to report ['%s'], got: %v", conflict.Branch, status.ConflictBranches)
	}
}

func TestConflictBranchesToleratesUnreachableRemote(t *testing.T) {
	if e := initRepo(); e != nil {
		t.Fatal(e)
	}
	g := NewGit(testRepoDir, "")

	// No remote configured at all: status must still work.
	status, e := g.Status()
	if e != nil {
		t.Fatalf("expected status to survive an unreachable remote, got: %v", e)
	}
	if len(status.ConflictBranches) != 0 {
		t.Fatalf("expected no conflict branches, got: %v", status.ConflictBranches)
	}
}
