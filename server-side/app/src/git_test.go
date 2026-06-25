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
	if e := stageFile(g, ".obsidian/app.json", "{\"version\":1}"); e != nil {
		t.Fatalf("staging gitignored file failed: %v", e)
	}

	status, e := g.Status()
	if e != nil {
		t.Fatal(e)
	}
	if len(status.Files) != 1 {
		t.Fatalf("expected 1 staged file, got: %+v", status.Files)
	}
	if status.Files[0].Path != ".obsidian/app.json" {
		t.Fatalf("expected .obsidian/app.json, got: %+v", status.Files[0])
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
		e := g.Commit(&Commitment{
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
		e := g.Commit(&Commitment{
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

	e = g.Commit(&Commitment{
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
	g.TryClone()

	if _, err := g.shell.Execute("git rev-parse --git-dir"); err != nil {
		t.Fatalf("expected valid repo after TryClone, got: %v", err)
	}
}
