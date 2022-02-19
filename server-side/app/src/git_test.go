package main

import (
	"encoding/base64"
	"errors"
	"os"
	"strings"
	"testing"
)

const testRepoDir = "/tmp/test_repo"

func TestGitStatusSmoke(t *testing.T) {
	err := initRepo()
	if err != nil {
		t.Error(err)
		return
	}

	g := NewGit(testRepoDir)
	editFile("README.md", "# sample")
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

	if file.Status != StatusUntracked {
		t.Errorf("Expecting untracked file. Got: %+v", file)
		return
	}
}

func TestGitStatusDiff(t *testing.T) {
	err := initRepo()
	if err != nil {
		t.Error(err)
		return
	}

	g := NewGit(testRepoDir)
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

func editFile(path string, content string) error {
	s := &Shell{testRepoDir}
	_, e := s.Execute("echo '" + content + "' > " + path)
	return e
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
