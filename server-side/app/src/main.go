package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os/exec"
)

const (
	CWD    = "/app/repo-store/repo"
	BRANCH = "master"
)

var (
	git   = NewGit(CWD)
	shell = &Shell{
		Cwd: CWD}
)

func getLastRevision(w http.ResponseWriter, req *http.Request) {
	revision, e := git.LastRevision()

	if e != nil {
		writeError(w, "revision resolve failed", e, revision)
		return
	}

	revision_zip := "/tmp/" + revision + ".zip"
	shell.Execute("rm " + revision_zip)
	command := "zip -r " + revision_zip + " repo --exclude \"repo/.git/*\""
	// executing "zip" through  "/bin/sh -c" cause somehow same "zip" command
	// cannot match excluded files correctly (command.Dir = CWD break everything)
	r, e := exec.Command("/bin/sh", "-c", "cd "+CWD+"/.. && "+command).Output()

	if e != nil {
		writeError(w, "zipping failed", e, "\nzip command: "+command+"\ncommand output: "+string(r))
		return
	}

	// o, _ := execute("zip -sf " + revision_zip)
	// fmt.Println("cmd: ", command, "out:", string(o))

	fmt.Println("cmd out: ", command, string(r))
	writeFile(revision+".zip", revision_zip, w, req)
}

func writeFile(filename string, file string, w http.ResponseWriter, req *http.Request) {
	// grab the generated receipt.pdf file and stream it to browser
	streamFileBytes, err := ioutil.ReadFile(file)

	if err != nil {
		writeError(w, "file writing failed", err, "")
		return
	}

	b := bytes.NewBuffer(streamFileBytes)

	w.Header().Set("Content-type", "application/zip")
	w.Header().Set("Content-Disposition", "attachment; filename="+filename)

	if _, err := b.WriteTo(w); err != nil {
		fmt.Fprintf(w, "%s", err)
	}
}

func writeError(w http.ResponseWriter, stage string, err error, extra string) {
	e := fmt.Sprintf("{\"error\": \"%s - %v\noutput:%v\" }", stage, err, extra)
	http.Error(w, e, 500)
}

func getHealth(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "OK!")
}

type Commitment struct {
	Message string `json:"message"`
}

func postCommit(w http.ResponseWriter, req *http.Request) {
	r, e := ioutil.ReadAll(req.Body)

	if e != nil {
		writeError(w, "unexpected request body", e, string(r))
		return
	}

	var commitment *Commitment
	err := json.Unmarshal(r, &commitment)

	if err != nil {
		writeError(w, "request body parsing", err, string(r))
		return
	}

	if commitment.Message == "" {
		writeError(w, "no commit message specified", nil, string(r))
		return
	}

	commitOut, commitErr := shell.Execute("git commit --message=\"" + commitment.Message + "\"")
	if commitErr != nil {
		writeError(w, "committing", commitErr, commitOut)
		return
	}

	pullOut, pullErr := shell.Execute("git pull --rebase origin " + BRANCH)
	if pullErr != nil {
		writeError(w, "pulling", pullErr, pullOut)
		return
	}

	pushOut, pushErr := shell.Execute("git push origin " + BRANCH)
	if pushErr != nil {
		writeError(w, "pushing", pullErr, pushOut)
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
	_, e := shell.Execute("git config --local user.email \"wiki-app@tsourcecode.com\"")
	if e != nil {
		panic(e)
	}

	_, err := shell.Execute("git config --local user.name \"Wiki Committer\"")
	if err != nil {
		panic(err)
	}

	_, checkoutErr := shell.Execute("git checkout " + BRANCH)
	if checkoutErr != nil {
		panic(err)
	}

	http.HandleFunc("/api/1/revision/latest", getLastRevision)
	http.HandleFunc("/api/1/commit", postCommit)
	http.HandleFunc("/api/1/stage", stageFiles)
	http.HandleFunc("/api/health", getHealth)

	http.ListenAndServe(":80", nil)
}
