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
	BRANCH = "master"
)

type ProjectHttpApi struct {
	git          *Git
	shell        *Shell
	zipper       *Zipper
	diffProvider *DiffProvider
}

func NewHttpProject(repoDir string, repoUrl string) *ProjectHttpApi {
	shell := &Shell{
		Cwd: repoDir}
	result := &ProjectHttpApi{
		git:   NewGit(repoDir, repoUrl),
		shell: shell,
		zipper: &Zipper{
			shell: shell,
			cwd:   repoDir},
		diffProvider: &DiffProvider{
			repoDir: repoDir},
	}
	return result
}

func (p *ProjectHttpApi) Start() {
	p.git.TryClone()
	fmt.Println("Preparing repo!")
	p.shell.StrictExecute("git config --local user.email \"wiki-app@tsourcecode.com\"")
	p.shell.StrictExecute("git config --local user.name \"Wiki Committer\"")
	p.shell.StrictExecute("git checkout " + BRANCH)

	http.HandleFunc("/api/1/revision/latest", p.getLastRevision)
	http.HandleFunc("/api/1/revision/sync", p.getOutdatedAtLastRevision)
	http.HandleFunc("/api/1/status", p.getStatus)
	http.HandleFunc("/api/1/commit", p.postCommit)
	http.HandleFunc("/api/1/stage", p.stageFiles)

}

func (p *ProjectHttpApi) getOutdatedAtLastRevision(w http.ResponseWriter, req *http.Request) {
	e := p.git.Rebase()
	if e != nil {
		writeError(w, "Rebase failed", e)
		return
	}

	revision, e := p.git.LastRevision()

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
		fmt.Println("Unexpected body at selective sync call error: ", err, " raw body: '", string(bodyBytes), "'")
		return
	}

	outdated, diffErr := p.diffProvider.ShowOutdated(files)

	if diffErr != nil {
		writeError(w, "Error getting outdated files: ", diffErr)
		return
	}

	seed := RandStringRunes(10)
	revision_zip := "/tmp/partial_" + seed + "_of_" + revision + ".zip"
	zipErr := p.zipper.ZipSelective(revision_zip, outdated)
	if zipErr != nil {
		writeError(w, "zipping failed", zipErr)
		return
	}

	writeFile(revision+".zip", revision_zip, w, req)
}

func join(e error, extra string) error {
	return errors.New(e.Error() + "\nExtra: " + extra)
}

func (p *ProjectHttpApi) getStatus(w http.ResponseWriter, req *http.Request) {
	status, e := p.git.Status()

	if e != nil {
		writeError(w, "Status check", e)
		return
	}

	writeJsonStruct(status, w, req)
}

func (p *ProjectHttpApi) postCommit(w http.ResponseWriter, req *http.Request) {
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

	commitErr := p.git.Commit(commitment)
	if commitErr != nil {
		writeError(w, "commit", join(commitErr, string(r)))
		return
	}

	rebaseErr := p.git.Rebase()

	if rebaseErr != nil {
		writeError(w, "rebasing", rebaseErr)
		return
	}

	if pushErr := p.git.Push(); pushErr != nil {
		writeError(w, "pushing", pushErr)
		return
	}
	fmt.Fprint(w, "{ \"result\": \"true\"")
}

func (p *ProjectHttpApi) stageFiles(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		panic(e)
	}

	var staging *Staging
	err := json.Unmarshal(r, &staging)

	if err != nil {
		writeError(w, "Parsing", err)
		return
	}

	for _, f := range staging.Files {
		e := p.git.Stage(f)
		if e != nil {
			writeError(w, "Staging "+f.Path, e)
			return
		}

	}
}

func (p *ProjectHttpApi) headers(w http.ResponseWriter, req *http.Request) {
	for name, headers := range req.Header {
		for _, h := range headers {
			fmt.Fprintf(w, "%v: %v\n", name, h)
		}
	}
}

func (p *ProjectHttpApi) getLastRevision(w http.ResponseWriter, req *http.Request) {
	e := p.git.Rebase()
	if e != nil {
		writeError(w, "Rebase failed", e)
		return
	}

	revision, e := p.git.LastRevision()

	if e != nil {
		writeError(w, "revision resolve failed", e)
		return
	}

	revision_zip := "/tmp/" + revision + ".zip"
	zipErr := p.zipper.ZipRepo(revision_zip)
	if zipErr != nil {
		writeError(w, "zipping failed", zipErr)
		return
	}

	writeFile(revision+".zip", revision_zip, w, req)
}

func writeError(w http.ResponseWriter, stage string, err error) {
	e := fmt.Sprintf("{\"error\": \"\nStage: %s\n%v\" }", stage, err)
	http.Error(w, e, 500)
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

func writeJsonStruct(jsonStruct interface{}, w http.ResponseWriter, req *http.Request) {
	jsonBytes, err := json.Marshal(jsonStruct)

	if err != nil {
		writeError(w, "Json marshalling failed", err)
		return
	}

	b := bytes.NewBuffer(jsonBytes)

	w.Header().Set("Content-type", "application/json")

	if _, err := b.WriteTo(w); err != nil {
		fmt.Fprintf(w, "%s", err)
	}
}
