package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
)

const (
	BRANCH = "master"
)

type ProjectHttpApi struct {
	config       *Configuration
	git          *Git
	shell        *Shell
	zipper       *Zipper
	diffProvider *DiffProvider
}

type Configuration struct {
	id      string
	repoDir string
	repoUrl string
}

func NewHttpProject(config *Configuration) *ProjectHttpApi {
	shell := &Shell{
		Cwd: config.repoDir}
	result := &ProjectHttpApi{
		config: config,
		git:    NewGit(config.repoDir, config.repoUrl),
		shell:  shell,
		zipper: &Zipper{
			shell: shell,
			cwd:   config.repoDir},
		diffProvider: &DiffProvider{
			repoDir: config.repoDir},
	}
	return result
}

func (p *ProjectHttpApi) Start() {
	fmt.Printf("Staring configuration: %+v!\n", p.config)

	p.git.TryClone()
	p.shell.StrictExecute("git config --local user.email \"wiki-app@tsourcecode.com\"")
	p.shell.StrictExecute("git config --local user.name \"Wiki Committer\"")
	p.shell.StrictExecute("git status && git checkout " + BRANCH)

	prefix := "/" + p.config.id
	if p.config.id == "" {
		prefix = ""
	}

	http.HandleFunc(prefix+"/api/1/revision/latest", p.getLastRevision)
	http.HandleFunc(prefix+"/api/1/revision/sync", p.getOutdatedAtLastRevision)
	http.HandleFunc(prefix+"/api/1/revision/show", p.postRevisionShow)
	http.HandleFunc(prefix+"/api/1/show_not_staged", p.postShowNotStaged)
	http.HandleFunc(prefix+"/api/1/status", p.getStatus)
	http.HandleFunc(prefix+"/api/1/commit", p.postCommit)
	http.HandleFunc(prefix+"/api/1/stage", p.stageFiles)
	http.HandleFunc(prefix+"/api/1/pull", p.pullChanges)
	http.HandleFunc(prefix+"/api/1/rollback", p.rollbackChanges)
}

func (p *ProjectHttpApi) getOutdatedAtLastRevision(w http.ResponseWriter, req *http.Request) {
	p.tryRebase() //TODO: log error somewhere
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

func (p *ProjectHttpApi) postRevisionShow(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		writeError(w, "unexpected request body", join(e, string(r)))
		return
	}

	var revisionSpec *RevisionSpec
	err := json.Unmarshal(r, &revisionSpec)

	if err != nil {
		writeError(w, "request body parsing", join(err, string(r)))
		return
	}

	revisionInfo, err := p.git.ShowRevision(revisionSpec.Revision)
	if err != nil {
		writeError(w, "revision show", join(err, string(r)))
		return
	}

	writeJsonStruct(revisionInfo, w, req)
}

func (p *ProjectHttpApi) postShowNotStaged(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		writeError(w, "unexpected request body", join(e, string(r)))
		return
	}

	var localStatus *LocalStatus
	err := json.Unmarshal(r, &localStatus)

	if err != nil {
		writeError(w, "request body parsing", join(err, string(r)))
		return
	}

	notStaged, err := p.diffProvider.ShowNotStaged(localStatus)
	if err != nil {
		writeError(w, "not staged detection failed", join(err, string(r)))
		return
	}

	writeJsonStruct(notStaged, w, req)
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

func (p *ProjectHttpApi) pullChanges(w http.ResponseWriter, req *http.Request) {
	if e := p.git.Pull(); e != nil {
		p.git.AbortRebase()
		writeError(w, "Pull failed", e)
		return
	}

	revision, e := p.git.ShowRevision("HEAD~0")

	if e != nil {
		writeError(w, "Revision check", e)
		return
	}

	writeJsonStruct(revision, w, req)
}

func (p *ProjectHttpApi) rollbackChanges(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		panic(e)
	}

	var rollback *RollbackSpec
	err := json.Unmarshal(r, &rollback)

	if err != nil {
		writeError(w, "Parsing", err)
		return
	}

	for _, f := range rollback.Files {
		e := p.git.Rollback(f)
		if e != nil {
			writeError(w, "Reset "+f.Path, e)
			return
		}
	}
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

func (p *ProjectHttpApi) tryRebase() error {
	changes, e := p.git.hasUncommitedChanges()

	if e != nil || changes {
		return nil
	}

	if e := p.git.Rebase(); e != nil {
		return e
	}

	return nil
}

func (p *ProjectHttpApi) getLastRevision(w http.ResponseWriter, req *http.Request) {
	p.tryRebase() //TODO: log error somewhere

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

type ProjectApi struct {
	Configs []*ProjectConfig `json:"configs"`
}

func (c ProjectApi) changeProject(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		writeError(w, "unexpected request body", join(e, string(r)))
		return
	}

	var updatedConfig *ProjectConfig
	e = json.Unmarshal(r, &updatedConfig)

	if e != nil {
		writeError(w, "request body parsing", join(e, string(r)))
		return
	}

	for i, config := range c.Configs {
		if config.Name == updatedConfig.Name {
			c.Configs[i] = updatedConfig
			jBytes, e := json.Marshal(c.Configs)

			if e != nil {
				writeError(w, "serializing config failed", join(e, string(r)))
				return
			}
			e = ioutil.WriteFile(CONFIG_FILE, jBytes, 0755)

			if e != nil {
				writeError(w, "saving config failed", join(e, string(r)))
				return
			}

			break
		}
	}

	writeJsonStruct(c, w, req)
}

func (c ProjectApi) getProjects(w http.ResponseWriter, req *http.Request) {
	writeJsonStruct(c, w, req)
}

func (c ProjectApi) Start() {
	for _, config := range c.Configs {
		if config.Name == "" {
			pathParts := strings.Split(config.Url, "/")
			repoName := pathParts[len(pathParts)-1]
			config.Name = strings.TrimRight(repoName, ".git")
		}
	}
	http.HandleFunc("/api/1/projects", c.getProjects)
	for _, config := range c.Configs {
		http.HandleFunc("/api/1/project/"+config.Name, c.changeProject)
	}
}
