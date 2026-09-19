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

	// CONFLICT_BRANCH_PREFIX names the remote branches that hold work which
	// could not be rebased. The suffix is the short sha of the preserved commit.
	CONFLICT_BRANCH_PREFIX = "note_conflict_"

	conflictAmendMessage = "wiki-app: local changes preserved on conflict"
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

	output, commitErr := g.execute("git commit --message=\"" + commitment.Message + "\"")
	if commitErr != nil {
		return output, g.maybeIncludeDebugInfo(errors.New(commitErr.Error() + "\nstderr: " + output))
	}

	return output, nil
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
		cause := errors.New("Pull failed! " + rebaseErr.Error() + "\nCurrent status:\n" + out)
		return g.preserveConflict(cause, "")
	}

	return nil
}

// ConflictError reports that a rebase conflict was preserved at a remote branch
// instead of being thrown away. The local repo is back in sync with the remote,
// so it is safe to keep serving requests.
type ConflictError struct {
	Branch string
	Cause  error
}

func (e *ConflictError) Error() string {
	return "Conflict preserved at remote branch '" + e.Branch + "'\nCause: " + e.Cause.Error()
}

// ConflictBranchOf returns the branch a conflict was preserved at, or "" when
// the error is not a conflict.
func ConflictBranchOf(e error) string {
	var conflict *ConflictError
	if errors.As(e, &conflict) {
		return conflict.Branch
	}
	return ""
}

func (g *Git) isRebaseInProgress() bool {
	for _, dir := range []string{"rebase-merge", "rebase-apply"} {
		if _, err := os.Stat(filepath.Join(g.repoDir, ".git", dir)); err == nil {
			return true
		}
	}
	return false
}

// preserveConflict pushes the local work that failed to rebase to a dedicated
// remote branch and then hard-aligns the local branch with the remote, so that
// nothing is lost and the next sync is not blocked by the same conflict.
//
// amendMessage, when not empty, rewrites the message of the commit being
// preserved - used to replace the throwaway "temporary commit for rebasement".
func (g *Git) preserveConflict(cause error, amendMessage string) error {
	if !g.isRebaseInProgress() {
		// Not a conflict: leave the previous behaviour to the caller.
		return cause
	}

	if _, abortErr := g.execute("git rebase --abort"); abortErr != nil {
		return errors.New("Both rebase and abort failed!" +
			"\nRebase error: " + cause.Error() +
			"\nAbort error: " + abortErr.Error())
	}

	if amendMessage != "" {
		if _, amendErr := g.execute("git commit --amend --message=\"" + amendMessage + "\""); amendErr != nil {
			return errors.New("Conflict preservation failed at amend!" +
				"\nRebase error: " + cause.Error() +
				"\nAmend error: " + amendErr.Error())
		}
	}

	sha, shaErr := g.execute("git rev-parse --short HEAD")
	if shaErr != nil {
		return errors.New("Conflict preservation failed at revision resolve!" +
			"\nRebase error: " + cause.Error() +
			"\nRevision error: " + shaErr.Error())
	}

	branch := CONFLICT_BRANCH_PREFIX + strings.TrimSpace(sha)
	fmt.Println("Conflict detected. Preserving local work at '" + branch + "'")

	if _, pushErr := g.execute("git push " + g.remote + " HEAD:refs/heads/" + branch); pushErr != nil {
		// Nothing is confirmed at the remote yet: never discard local work here.
		if resetErr := g.softReset(); resetErr != nil {
			return errors.New("Rebase, conflict push and reset failed!" +
				"\nRebase error: " + cause.Error() +
				"\nPush error: " + pushErr.Error() +
				"\nReset error: " + resetErr.Error())
		}
		return errors.New("Conflict preservation failed at push!" +
			"\nRebase error: " + cause.Error() +
			"\nPush error: " + pushErr.Error())
	}

	if _, resetErr := g.execute("git reset --hard " + g.remote + "/" + g.branch); resetErr != nil {
		return errors.New("Conflict preservation failed at reset!" +
			"\nLocal work is safe at '" + branch + "'" +
			"\nRebase error: " + cause.Error() +
			"\nReset error: " + resetErr.Error())
	}

	return &ConflictError{Branch: branch, Cause: cause}
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
		amendMessage := ""
		if hadChanges {
			amendMessage = conflictAmendMessage
		}
		return g.preserveConflict(rebaseErr, amendMessage)
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
	// A remote that cannot be reached must not take the whole status down:
	// local file status is still useful offline.
	conflictBranches, e := g.ConflictBranches()
	if e != nil {
		fmt.Printf("Could not list conflict branches: %v\n", e)
		conflictBranches = []string{}
	}

	return &Status{
		Files:            files,
		ConflictBranches: conflictBranches,
	}, nil
}

// ConflictBranches asks the remote which conflict branches exist right now.
func (g *Git) ConflictBranches() ([]string, error) {
	out, e := g.execute("git ls-remote --heads " + g.remote + " \"" + CONFLICT_BRANCH_PREFIX + "*\"")
	if e != nil {
		return nil, e
	}

	branches := make([]string, 0)
	for _, line := range strings.Split(out, "\n") {
		// "<sha>\trefs/heads/<branch>"
		tab := strings.Index(line, "\t")
		if tab < 0 {
			continue
		}
		ref := strings.TrimSpace(line[tab+1:])
		branch := strings.TrimPrefix(ref, "refs/heads/")
		if strings.HasPrefix(branch, CONFLICT_BRANCH_PREFIX) {
			branches = append(branches, branch)
		}
	}

	return branches, nil
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
	// ConflictBranches lists the remote branches holding work that could not be
	// rebased. Read straight from the remote, so a branch the user has merged
	// and deleted stops being reported without any local bookkeeping.
	ConflictBranches []string `json:"conflict_branches"`
}
