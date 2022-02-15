package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
)

const (
	CWD    = "/app/repo-store/repo"
	BRANCH = "master"
)

var (
	git   = NewGit(CWD)
	shell = &Shell{
		Cwd: CWD}
	zipper = &Zipper{
		shell: shell,
		cwd:   CWD}
	diffProvider = &DiffProvider{
		repoDir: CWD}
)

func getLastRevision(w http.ResponseWriter, req *http.Request) {
	e := git.Rebase()
	if e != nil {
		writeError(w, "Rebase failed", e)
		return
	}

	revision, e := git.LastRevision()

	if e != nil {
		writeError(w, "revision resolve failed", e)
		return
	}

	revision_zip := "/tmp/" + revision + ".zip"
	zipErr := zipper.ZipRepo(revision_zip, nil)
	if zipErr != nil {
		writeError(w, "zipping failed", e)
		return
	}

	writeFile(revision+".zip", revision_zip, w, req)
}

func getOutdatedAtLastRevision(w http.ResponseWriter, req *http.Request) {
	e := git.Rebase()
	if e != nil {
		writeError(w, "Rebase failed", e)
		return
	}

	revision, e := git.LastRevision()

	if e != nil {
		writeError(w, "revision resolve failed", e)
		return
	}

	bodyBytes, bodyErr := ioutil.ReadAll(req.Body)

	if bodyErr != nil {
		writeError(w, "Body read error: ", e)
		return
	}

	var files []*File
	if err := json.Unmarshal(bodyBytes, &files); err != nil {
		writeError(w, "Unexpected body: ", err)
		return
	}

	outdated, diffErr := diffProvider.ShowOutdated(files)

	if diffErr != nil {
		writeError(w, "Error getting outdated files: ", diffErr)
		return
	}

	revision_zip := "/tmp/" + revision + ".zip"
	zipErr := zipper.ZipRepo(revision_zip, outdated)
	if zipErr != nil {
		writeError(w, "zipping failed", e)
		return
	}

	writeFile(revision+".zip", revision_zip, w, req)
}

func writeFile(filename string, file string, w http.ResponseWriter, req *http.Request) {
	// grab the generated receipt.pdf file and stream it to browser
	streamFileBytes, err := ioutil.ReadFile(file)

	if err != nil {
		writeError(w, "file writing failed", err)
		return
	}

	b := bytes.NewBuffer(streamFileBytes)

	w.Header().Set("Content-type", "application/zip")
	w.Header().Set("Content-Disposition", "attachment; filename="+filename)

	if _, err := b.WriteTo(w); err != nil {
		fmt.Fprintf(w, "%s", err)
	}
}

func writeError(w http.ResponseWriter, stage string, err error) {
	e := fmt.Sprintf("{\"error\": \"\nStage: %s\n%v\" }", stage, err)
	http.Error(w, e, 500)
}

func getHealth(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "OK!")
}

func join(e error, extra string) error {
	return errors.New(e.Error() + "\nExtra: " + extra)
}

func postCommit(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		writeError(w, "unexpected request body", join(e, string(r)))
		return
	}

	var commitment *Commitment
	err := json.Unmarshal(r, &commitment)

	if err != nil {
		writeError(w, "request body parsing", join(err, string(r)))
		return
	}

	commitErr := git.Commit(commitment)
	if commitErr != nil {
		writeError(w, "commit", join(commitErr, string(r)))
		return
	}

	rebaseErr := git.Rebase()

	if rebaseErr != nil {
		writeError(w, "rebasing", rebaseErr)
		return
	}

	if pushErr := git.Push(); pushErr != nil {
		writeError(w, "pushing", pushErr)
		return
	}
	fmt.Fprint(w, "{ \"result\": \"true\"")
}

func stageFiles(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		panic(e)
	}

	var staging *Staging
	err := json.Unmarshal(r, &staging)

	if err != nil {
		panic(err)
	}

	for _, f := range staging.Files {
		git.Stage(f)
	}
}

func headers(w http.ResponseWriter, req *http.Request) {
	for name, headers := range req.Header {
		for _, h := range headers {
			fmt.Fprintf(w, "%v: %v\n", name, h)
		}
	}
}

func main() {
	git.TryClone()

	fmt.Println("Preparing repo!")
	shell.StrictExecute("git config --local user.email \"wiki-app@tsourcecode.com\"")
	shell.StrictExecute("git config --local user.name \"Wiki Committer\"")
	shell.StrictExecute("git checkout " + BRANCH)

	http.HandleFunc("/api/1/revision/latest", getLastRevision)
	http.HandleFunc("/api/1/revision/sync", getOutdatedAtLastRevision)
	http.HandleFunc("/api/1/commit", postCommit)
	http.HandleFunc("/api/1/stage", stageFiles)
	http.HandleFunc("/api/health", getHealth)

	fmt.Println("Starting server...")
	http.ListenAndServe(":80", nil)
}
