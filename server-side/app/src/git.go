package main

import (
	"encoding/base64"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
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
	relPath := normalizeGitPath(f.Path)
	absFilePath := filepath.Join(g.repoDir, relPath)
	quotedPath := quoteShellPath(relPath)
	fmt.Printf("===> Rolling back '%v'\n", relPath)
	g.shell.PrintOutput("git status")

	resetCmd := "git reset HEAD -- " + quotedPath
	resetOut, resetErr := g.execute(resetCmd)
	if resetErr != nil {
		fmt.Printf("-> Git reset fails: '%v'\n", resetErr)
	} else {
		fmt.Printf("-> Successful Git Reset: '%v' -> '%v'\n", resetCmd, resetOut)
	}

	checkoutCmd := "git checkout HEAD -- " + quotedPath
	checkoutOut, checkoutErr := g.execute(checkoutCmd)
	if checkoutErr == nil {
		fmt.Printf("-> Successful Git Checkout: '%v' -> '%v'\n", checkoutCmd, checkoutOut)
		g.shell.PrintOutput("git status")
		return nil
	}

	fmt.Printf("-> Git checkout fails: '%v'\n", checkoutErr)
	_, statErr := os.Stat(absFilePath)
	if statErr == nil {
		fmt.Printf("-> Removing untracked file '%v'\n", absFilePath)
		return os.Remove(absFilePath)
	}
	if os.IsNotExist(statErr) {
		fmt.Printf("-> File already absent: '%v'\n", absFilePath)
		return nil
	}
	return statErr
}

func (g *Git) Stage(f *FileContent) error {
	decoded, _ := base64.StdEncoding.DecodeString(f.Content)
	relPath := normalizeGitPath(f.Path)
	absFilePath := filepath.Join(g.repoDir, relPath)
	if e := os.MkdirAll(filepath.Dir(absFilePath), 0755); e != nil {
		return e
	}
	if e := ioutil.WriteFile(absFilePath, []byte(decoded), os.ModePerm); e != nil {
		return e
	}

	_, err := g.execute("git add -- " + quoteShellPath(relPath))
	return err
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

func (g *Git) Commit(commitment *Commitment) (string, error) {
	if commitment.Message == "" {
		return "", errors.New("No commit message specified")
	}

	// ===== HOOK DIAGNOSTICS (before commit) =====
	beforeBuf := &strings.Builder{}
	beforeBuf.WriteString("\n===== HOOK DIAGNOSTICS (before commit) =====\n")

	// 1. Check core.hooksPath git config
	if hp, err := g.execute("git config core.hooksPath"); err == nil {
		line := fmt.Sprintf("git config core.hooksPath = '%s'", strings.TrimSpace(hp))
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := "git config core.hooksPath is NOT set (using default .git/hooks/)"
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 2. List .git/hooks/ directory
	if hooksList, err := g.execute("ls -la .git/hooks/ 2>&1"); err == nil {
		line := fmt.Sprintf(".git/hooks/ contents:\n%s", hooksList)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := fmt.Sprintf("Failed to list .git/hooks/: %s", hooksList)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 3. Check .githooks/pre-commit existence and executable bit
	if ghInfo, err := g.execute("ls -la .githooks/pre-commit 2>&1"); err == nil {
		line := fmt.Sprintf(".githooks/pre-commit exists:\n%s", ghInfo)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := fmt.Sprintf(".githooks/pre-commit does NOT exist or is inaccessible: %s", ghInfo)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 4. Check .githooks/bin/lua -v
	if luaOut, err := g.execute(".githooks/bin/lua -v 2>&1"); err == nil {
		line := fmt.Sprintf(".githooks/bin/lua -v output:\n%s", luaOut)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := fmt.Sprintf(".githooks/bin/lua -v failed: %s", luaOut)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 5. Current shell info
	if shellOut, err := g.execute("echo $SHELL"); err == nil {
		line := fmt.Sprintf("SHELL env var = '%s'", strings.TrimSpace(shellOut))
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}
	if shOut, err := g.execute("/bin/sh --version 2>&1"); err == nil {
		line := fmt.Sprintf("/bin/sh --version:\n%s", shOut)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := fmt.Sprintf("/bin/sh --version failed: %s", shOut)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 6. Check if the standard pre-commit hook is executable
	if pcInfo, err := g.execute("ls -la .git/hooks/pre-commit 2>&1"); err == nil {
		line := fmt.Sprintf(".git/hooks/pre-commit details:\n%s", pcInfo)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := fmt.Sprintf(".git/hooks/pre-commit does NOT exist: %s", pcInfo)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 7. Check git version
	if gvOut, err := g.execute("git --version"); err == nil {
		line := fmt.Sprintf("git --version: %s", strings.TrimSpace(gvOut))
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 8. Check if hooks are globally disabled (core.hooksPath pointing to /dev/null or similar)
	if hpOut, err := g.execute("git config --global core.hooksPath 2>&1"); err == nil {
		line := fmt.Sprintf("git config --global core.hooksPath = '%s'", strings.TrimSpace(hpOut))
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	} else {
		line := "git config --global core.hooksPath is NOT set"
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	// 9. git diff and git diff --staged before commit
	if diffOut, err := g.execute("git diff 2>&1"); err == nil {
		line := fmt.Sprintf("git diff (before commit):\n%s", diffOut)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}
	if diffStagedOut, err := g.execute("git diff --staged 2>&1"); err == nil {
		line := fmt.Sprintf("git diff --staged (before commit):\n%s", diffStagedOut)
		fmt.Println(line)
		beforeBuf.WriteString(line + "\n")
	}

	beforeBuf.WriteString("===== END HOOK DIAGNOSTICS (before commit) =====\n")
	fmt.Print(beforeBuf.String())
	// ===== END HOOK DIAGNOSTICS (before commit) =====

	cmd := "git commit --message=\"" + commitment.Message + "\""
	fmt.Printf("Executing: %s\n", cmd)
	output, commitErr := g.execute(cmd)

	// ===== HOOK DIAGNOSTICS (after commit) =====
	afterBuf := &strings.Builder{}
	afterBuf.WriteString("\n===== HOOK DIAGNOSTICS (after commit) =====\n")

	// git diff and git diff --staged after commit (to detect hook-induced changes)
	if diffOut, err := g.execute("git diff 2>&1"); err == nil {
		line := fmt.Sprintf("git diff (after commit):\n%s", diffOut)
		fmt.Println(line)
		afterBuf.WriteString(line + "\n")
	}
	if diffStagedOut, err := g.execute("git diff --staged 2>&1"); err == nil {
		line := fmt.Sprintf("git diff --staged (after commit):\n%s", diffStagedOut)
		fmt.Println(line)
		afterBuf.WriteString(line + "\n")
	}

	// Check if hook modified README.md (common hook pattern)
	if readmeOut, err := g.execute("git status --short 2>&1"); err == nil {
		line := fmt.Sprintf("git status --short (after commit):\n%s", readmeOut)
		fmt.Println(line)
		afterBuf.WriteString(line + "\n")
	}

	afterBuf.WriteString("===== END HOOK DIAGNOSTICS (after commit) =====\n")
	fmt.Print(afterBuf.String())
	// ===== END HOOK DIAGNOSTICS (after commit) =====

	if commitErr != nil {
		outputWithDebug := output + beforeBuf.String() + afterBuf.String()
		return outputWithDebug, g.maybeIncludeDebugInfo(errors.New(commitErr.Error() + "\nstderr: " + outputWithDebug))
	}

	return output + beforeBuf.String() + afterBuf.String(), nil
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

	_, fetchErr := g.execute("git fetch " + g.remote + " " + g.branch)

	if fetchErr != nil {
		return fetchErr
	}

	_, rebaseErr := g.execute("git rebase " + g.remote + "/" + g.branch)

	if rebaseErr != nil {
		out, _ := g.shell.Execute("git status")
		return errors.New("Pull failed! " + rebaseErr.Error() + "\nCurrent status:\n" + out)
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
		if _, err := g.Commit(&Commitment{Message: "temporary commit for rebasement"}); err != nil {
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

func (g *Git) isValidGitRepo() bool {
	if _, err := os.Stat(g.repoDir); os.IsNotExist(err) {
		return false
	}
	_, err := g.shell.Execute("git rev-parse --git-dir")
	return err == nil
}

func (g *Git) TryClone(cmdAfterClone string) {
	if g.isValidGitRepo() {
		fmt.Println("Repo already cloned: '" + g.repoDir + "'")
	} else {
		g.doClone()
	}

	if err := runAfterCloneIfNeeded(g.repoDir, cmdAfterClone); err != nil {
		panic(err)
	}
}

func (g *Git) doClone() {
	if _, err := os.Stat(g.repoDir); err == nil {
		fmt.Printf("Removing invalid repo directory: '%s'\n", g.repoDir)
		if e := os.RemoveAll(g.repoDir); e != nil {
			panic(e)
		}
	}

	if e := os.MkdirAll(g.repoDir, 0755); e != nil {
		panic(e)
	}

	fmt.Println("Cloning " + g.repoUrl + " to: " + g.repoDir)
	g.shell.StrictExecute("git clone " + g.repoUrl + " \"" + g.repoDir + "\"")
}

func (g *Git) Status() (*Status, error) {
	output, e := g.execute("git status -z --porcelain")
	if e != nil {
		return nil, e
	}

	entries, e := parsePorcelain(output)
	if e != nil {
		return nil, e
	}

	files := make([]*FileStatus, 0)
	for _, entry := range entries {
		status, e := porcelainXYToStatus(entry.xy)
		if e != nil {
			return nil, e
		}

		diff := ""
		if status == StatusModified {
			diffCmd := "git diff --staged -- "
			if entry.xy[0] == ' ' {
				diffCmd = "git diff -- "
			}
			d, e := g.execute(diffCmd + quoteShellPath(entry.path))
			if e != nil {
				return nil, e
			}
			diff = d
		}

		files = append(files, &FileStatus{
			Path:   entry.path,
			Status: status,
			Diff:   diff,
		})
	}
	return &Status{
		Files: files,
	}, nil
}

type porcelainEntry struct {
	xy   string
	path string
}

func parsePorcelain(output string) ([]porcelainEntry, error) {
	if len(output) == 0 {
		return nil, nil
	}

	data := []byte(output)
	entries := make([]porcelainEntry, 0)
	i := 0
	for i < len(data) {
		if i+3 > len(data) {
			return nil, fmt.Errorf("invalid git status -z output at byte %d", i)
		}
		if data[i+2] != ' ' {
			return nil, fmt.Errorf("expected space after status at byte %d", i)
		}

		xy := string(data[i : i+2])
		pathStart := i + 3
		pathEnd := pathStart
		for pathEnd < len(data) && data[pathEnd] != 0 {
			pathEnd++
		}
		if pathEnd >= len(data) {
			return nil, fmt.Errorf("unterminated path at byte %d", pathStart)
		}

		path := string(data[pathStart:pathEnd])
		i = pathEnd + 1

		if len(xy) > 0 && (xy[0] == 'R' || xy[0] == 'C') {
			path2Start := i
			path2End := path2Start
			for path2End < len(data) && data[path2End] != 0 {
				path2End++
			}
			if path2End >= len(data) {
				return nil, fmt.Errorf("unterminated rename path at byte %d", path2Start)
			}
			path = string(data[path2Start:path2End])
			i = path2End + 1
		}

		entries = append(entries, porcelainEntry{xy: xy, path: path})
	}
	return entries, nil
}

func porcelainXYToStatus(xy string) (string, error) {
	if xy == "??" {
		return StatusUntracked, nil
	}
	if len(xy) < 2 {
		return "", errors.New("invalid git status code '" + xy + "'")
	}
	if xy[0] == 'A' && xy[1] == ' ' {
		return StatusNew, nil
	}
	if xy[0] == 'M' && xy[1] == ' ' {
		return StatusModified, nil
	}
	if xy[0] == ' ' && xy[1] == 'M' {
		return StatusModified, nil
	}
	return "", errors.New("unsupported git status code '" + xy + "'")
}

func normalizeGitPath(path string) string {
	path = strings.TrimSpace(path)
	if len(path) >= 2 && path[0] == '"' && path[len(path)-1] == '"' {
		path = path[1 : len(path)-1]
	}
	if strings.Contains(path, `\`) {
		return unquoteGitPath(path)
	}
	return path
}

func unquoteGitPath(path string) string {
	var result []byte
	for i := 0; i < len(path); i++ {
		if path[i] == '\\' && i+3 < len(path) &&
			path[i+1] >= '0' && path[i+1] <= '7' &&
			path[i+2] >= '0' && path[i+2] <= '7' &&
			path[i+3] >= '0' && path[i+3] <= '7' {
			val := (path[i+1]-'0')<<6 | (path[i+2]-'0')<<3 | (path[i+3] - '0')
			result = append(result, val)
			i += 3
			continue
		}
		result = append(result, path[i])
	}
	return string(result)
}

func quoteShellPath(path string) string {
	return "\"" + strings.ReplaceAll(path, "\"", "\\\"") + "\""
}

type FileStatus struct {
	Path   string `json:"path"`
	Status string `json:"status"`
	Diff   string `json:"diff"`
}
type Status struct {
	Files []*FileStatus `json:"files"`
}
