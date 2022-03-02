package main

import (
	"encoding/base64"
	"errors"
	"fmt"
	"os"
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

func TestGitQuotedFileWithSpacesStatus(t *testing.T) {
	fileName := "\"quoted file\""
	fileStatus, e := checkStatus(func(g *Git) error {
		editFile(fileName, "content")
		return stageFile(g, fileName, "stage")
	})

	if e != nil {
		t.Error(e)
		return
	}

	if fileStatus.Path != "\\\"quoted file\\\"" { // not sure if this is correct, but okay for now
		t.Errorf("Expecting '"+fileName+"' file. Got: '%s' (%+v)", fileStatus.Path, fileStatus)
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
