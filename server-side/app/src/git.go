package main

import (
	"encoding/base64"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
)

const (
	DEBUG_MESSAGES  = true
	StatusNew       = "new"
	StatusModified  = "modified"
	StatusUntracked = "untracked"
)

type FileRollback struct {
	Path string `json:"path"`
}
type RollbackSpec struct {
	Files []*FileRollback `json:"files"`
}

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

type RevisionSpec struct {
	Revision string `json:"revision"`
}

type RevisionInfo struct {
	Revision string `json:"revision"`
	Date     string `json:"date"`
	Message  string `json:"message"`
}

type Git struct {
	branch  string
	remote  string
	repoDir string
	repoUrl string
	shell   *Shell
}

func NewGit(dir string, url string) *Git {
	return &Git{
		branch:  "master",
		remote:  "origin",
		repoDir: dir,
		repoUrl: url,
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
		return "", errors.New(err.Error() + "\nstderr: " + result)
	}

	return strings.Replace(result, "\n", "", -1), nil
}

func (g *Git) Rollback(f *FileRollback) error {
	filePath := strings.ReplaceAll(f.Path, "\"", "\\\"")
	fmt.Printf("Rolling back '%v'", filePath)
	g.shell.PrintOutput("git status")

	resetCmd := "git reset \"" + filePath + "\""
	resetOut, err := g.execute(resetCmd)
	if err != nil {
		if _, err := os.Stat(filePath); err == nil {
			fmt.Printf("-> Git reset fails: '%v' Will try to remove!\n", err)
			return os.Remove(filePath)
		} else if os.IsNotExist(err) {
			return nil
		} else {
			return err
		}
	}

	fmt.Printf("-> Git Reset: '%v' -> '%v'\n", resetCmd, resetOut)
	g.shell.PrintOutput("git status")

	checkoutCmd := "git checkout \"" + filePath + "\""
	checkoutOut, err := g.execute(checkoutCmd)

	if err != nil {
		if _, err := os.Stat(filePath); err == nil {
			fmt.Printf("-> Git checkout fails: '%v' Will try to remove!\n", err)
			return os.Remove(filePath)
		} else if os.IsNotExist(err) {
			return nil
		} else {
			return err
		}
	}
	fmt.Printf("-> Git Checkout: '%v' -> '%v'\n", checkoutCmd, checkoutOut)
	g.shell.PrintOutput("git status")
	return nil
}

func (g *Git) Stage(f *FileContent) error {
	decoded, _ := base64.StdEncoding.DecodeString(f.Content)
	filePath := g.repoDir + "/" + f.Path
	e := ioutil.WriteFile(filePath, []byte(decoded), os.ModePerm)

	if e != nil {
		return e
	}

	filePath = strings.ReplaceAll(filePath, "\"", "\\\"")
	_, err := g.execute("git add \"" + filePath + "\"")

	if err != nil {
		return err
	}

	return nil
}

func (g *Git) ShowRevision(revision string) (*RevisionInfo, error) {
	date, err := g.execute("git show " + revision + " -s --format=%cd")

	if err != nil {
		return nil, err
	}

	message, err := g.execute("git show " + revision + " -s --format=%B")

	if err != nil {
		return nil, err
	}

	raw_revision, err := g.execute("git rev-parse " + revision)

	if err != nil {
		return nil, err
	}

	return &RevisionInfo{
		Revision: raw_revision,
		Date:     date,
		Message:  message,
	}, nil
}

func (g *Git) Commit(commitment *Commitment) error {
	if commitment.Message == "" {
		return errors.New("No commit message specified")
	}

	if output, commitErr := g.execute("git commit --message=\"" + commitment.Message + "\""); commitErr != nil {
		return g.maybeIncludeDebugInfo(errors.New(commitErr.Error() + "\nstderr: " + output))
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

func (g *Git) Pull() error {
	hadChanges, changesErr := g.hasUncommitedChanges()

	if changesErr != nil {
		return changesErr
	}

	if hadChanges {
		return errors.New("got changes, pull declined")
	}

	_, pullErr := g.execute("git pull --rebase " + g.remote + " " + g.branch)

	if pullErr != nil {
		return errors.New("Pull failed! " + pullErr.Error())
	}

	return nil
}

func (g *Git) AbortRebase() {
	g.execute("git rebase --abort")
}

func (g *Git) Rebase() error {
	hadChanges, changesErr := g.hasUncommitedChanges()

	if changesErr != nil {
		return changesErr
	}

	if hadChanges {
		// save against untracked and not staged files
		g.shell.StrictExecute("git add *")
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

func (g *Git) TryClone() {
	if _, err := os.Stat(g.repoDir + "/.git"); err == nil {
		fmt.Println("Repo already cloned: '" + g.repoDir + "'")
		return
	}

	if e := os.MkdirAll(g.repoDir, 0755); e != nil {
		panic(e)
	}

	fmt.Println("Cloning " + g.repoUrl + " to: " + g.repoDir)
	g.shell.StrictExecute("git clone " + g.repoUrl + " \"" + g.repoDir + "\"")
}

func (g *Git) Status() (*Status, error) {
	output, e := g.execute("git status --short")
	if e != nil {
		return nil, e
	}

	files := make([]*FileStatus, 0)
	for _, line := range strings.Split(output, "\n") {
		trimmed := strings.Trim(line, " ")
		if len(trimmed) == 0 {
			continue
		}

		status, fileName, e := toStatusAndFile(trimmed)

		if e != nil {
			return nil, e
		}

		diff := ""
		if status == StatusModified {
			d, e := g.execute("git diff --staged \"" + fileName + "\"")

			if e != nil {
				return nil, e
			}

			diff = d
		}

		files = append(files, &FileStatus{
			Path:   fileName,
			Status: status,
			Diff:   diff,
		})

	}
	return &Status{
		Files: files,
	}, nil
}

func toStatusAndFile(line string) (string, string, error) {
	trimFile := func(fileName string) string {
		fileName = strings.Trim(fileName, " ")
		if fileName[0] == '"' && fileName[len(fileName)-1] == '"' {
			fileName = fileName[1 : len(fileName)-1]
		}

		return fileName
	}
	if line[0] == 'A' && line[1] == ' ' {
		return StatusNew, trimFile(line[2:]), nil
	}

	if line[0] == 'M' && line[1] == ' ' {
		return StatusModified, trimFile(line[2:]), nil
	}

	if line[:2] == "??" && line[2] == ' ' {
		return StatusUntracked, trimFile(line[3:]), nil
	}

	return "", "", errors.New("Failed to parse git file status from '" + line + "'")
}

type FileStatus struct {
	Path   string `json:"path"`
	Status string `json:"status"`
	Diff   string `json:"diff"`
}
type Status struct {
	Files []*FileStatus `json:"files"`
}
