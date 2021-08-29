package main

import (
	"encoding/base64"
	"errors"
	"io/ioutil"
	"os"
	"strings"
)

const (
	DEBUG_MESSAGES = true
)

type Staging struct {
	Files []*FileContent `json:"files"`
}

type FileContent struct {
	Path    string `json:"path"`
	Content string `json:"content"`
}

type Commitment struct {
	Message string `json:"message"`
}

type Git struct {
	branch  string
	remote  string
	repoDir string
	shell   *Shell
}

func NewGit(dir string) *Git {
	return &Git{
		branch:  "master",
		remote:  "origin",
		repoDir: dir,
		shell:   &Shell{dir}}
}

func (g *Git) LastRevision() (string, error) {
	if _, err := os.Stat(g.repoDir + "/.git"); os.IsNotExist(err) {
		return "", errors.New("repo's .git not found!")
	}

	result, err := g.execute("git rev-parse HEAD~0")

	if err != nil {
		g.shell.PrintOutput("pwd")
		g.shell.PrintOutput("ls -l")
		g.shell.PrintOutput("git log -1")
		return "", err
	}

	return strings.Replace(result, "\n", "", -1), nil
}

func (g *Git) Stage(f *FileContent) {
	decoded, _ := base64.StdEncoding.DecodeString(f.Content)
	filePath := g.repoDir + "/" + f.Path
	e := ioutil.WriteFile(filePath, []byte(decoded), os.ModePerm)

	if e != nil {
		panic(e)
	}

	_, err := g.shell.Execute("git add " + filePath)

	if err != nil {
		panic(e)
	}
}

func (g *Git) Commit(commitment *Commitment) error {
	if commitment.Message == "" {
		return errors.New("No commit message specified")
	}

	if _, commitErr := g.execute("git commit --message=\"" + commitment.Message + "\""); commitErr != nil {
		return g.maybeIncludeDebugInfo(commitErr)
	}

	return nil
}

func (g *Git) maybeIncludeDebugInfo(e error) error {
	if !DEBUG_MESSAGES {
		return e
	}

	status, _ := g.execute("git status")
	return errors.New(e.Error() + " GIT STATUS: " + status)
}

func (g *Git) softReset() error {
	_, e := g.execute("git reset --soft HEAD~1")
	return e

}

func (g *Git) Rebase() error {
	hadChanges, changesErr := g.hasUncommitedChanges()

	if changesErr != nil {
		return changesErr
	}

	if hadChanges {
		if err := g.Commit(&Commitment{Message: "temporary commit for rebasement"}); err != nil {
			return err
		}
	}

	_, fetchErr := g.execute("git fetch " + g.remote + " " + g.branch)

	if fetchErr != nil {
		if hadChanges {
			if resetErr := g.softReset(); resetErr != nil {
				return errors.New("Both fetch and reset failed!" +
					"\nFetch error: " + fetchErr.Error() +
					"\nReset error: " + resetErr.Error())

			}
		}
		return fetchErr
	}

	if _, rebaseErr := g.execute("git rebase " + g.remote + "/" + g.branch); rebaseErr != nil {
		if _, abortErr := g.execute("git rebase --abort"); abortErr != nil {
			return errors.New("Both rebase and abort failed!" +
				"\nRebase error: " + rebaseErr.Error() +
				"\nAbort error: " + abortErr.Error())
		}

		if resetErr := g.softReset(); resetErr != nil {
			return errors.New("Both rebase and reset failed!" +
				"\nRebase error: " + rebaseErr.Error() +
				"\nReset error: " + resetErr.Error())
		}

		return rebaseErr
	}

	if hadChanges {
		if resetErr := g.softReset(); resetErr != nil {
			return resetErr
		}
	}

	return nil
}

func (g *Git) Push() error {
	_, e := g.execute("git push " + g.remote + " " + g.branch)
	return e
}

func (g *Git) execute(cmd string) (string, error) {
	out, e := g.shell.Execute(cmd)
	if e != nil {
		return out, errors.New("Error: " + e.Error() + "\nCommand: " + cmd + "\nOutput: " + out)
	}

	return out, nil
}

func (g *Git) hasUncommitedChanges() (bool, error) {
	out, e := g.execute("git status --short")

	if e != nil {
		return false, e
	}

	out = strings.ReplaceAll(out, "\n", "")
	return len(out) != 0, nil
}
