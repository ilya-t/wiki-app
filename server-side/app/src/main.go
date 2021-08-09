package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"os/exec"
	"strings"
)

const (
	CWD    = "/app/repo-store/repo"
	BRANCH = "master"
)

func execute(cmd string) (string, error) {
	command := exec.Command("/bin/sh", "-c", cmd)
	command.Dir = CWD
	var stderr bytes.Buffer
	command.Stderr = &stderr
	out, err := command.Output()

	if err != nil {
		return stderr.String(), err
	}

	return string(out), err
}

func printOutput(cmd string) {
	out, err := execute(cmd)
	if err != nil {
		fmt.Println("cmd: ", cmd, "\noutput: ", "ERROR("+err.Error()+"): "+out)
		return
	}

	fmt.Println("cmd: ", cmd, "\noutput: ", out)
}

func LastRevision() (string, error) {
	if _, err := os.Stat(CWD + "/.git"); os.IsNotExist(err) {
		return "", errors.New("repo's .git not found!")
	}

	result, err := execute("git rev-parse HEAD~0")

	if err != nil {
		printOutput("pwd")
		printOutput("ls -l")
		printOutput("git log -1")
		return result, err
	}

	return strings.Replace(result, "\n", "", -1), nil
}

func getLastRevision(w http.ResponseWriter, req *http.Request) {
	revision, e := LastRevision()

	if e != nil {
		writeError(w, "revision resolve failed", e, revision)
		return
	}

	revision_zip := "/tmp/" + revision + ".zip"
	execute("rm " + revision_zip)
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

	commitOut, commitErr := execute("git commit --message=\"" + commitment.Message + "\"")
	if commitErr != nil {
		writeError(w, "committing", commitErr, commitOut)
		return
	}

	pullOut, pullErr := execute("git pull --rebase origin " + BRANCH)
	if pullErr != nil {
		writeError(w, "pulling", pullErr, pullOut)
		return
	}

	pushOut, pushErr := execute("git push origin " + BRANCH)
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
		stage(f)
	}
}

func stage(f *FileContent) {
	decoded, _ := base64.StdEncoding.DecodeString(f.Content)
	filePath := CWD + "/" + f.Path
	e := ioutil.WriteFile(filePath, []byte(decoded), os.ModePerm)

	if e != nil {
		panic(e)
	}

	_, err := execute("git add " + filePath)

	if err != nil {
		panic(e)
	}

}

type Staging struct {
	Files []*FileContent `json:"files"`
}

type FileContent struct {
	Path    string `json:"path"`
	Content string `json:"content"`
}

func headers(w http.ResponseWriter, req *http.Request) {
	for name, headers := range req.Header {
		for _, h := range headers {
			fmt.Fprintf(w, "%v: %v\n", name, h)
		}
	}
}

func main() {
	_, e := execute("git config --local user.email \"wiki-app@tsourcecode.com\"")
	if e != nil {
		panic(e)
	}

	_, err := execute("git config --local user.name \"Wiki Committer\"")
	if err != nil {
		panic(err)
	}

	_, checkoutErr := execute("git checkout " + BRANCH)
	if checkoutErr != nil {
		panic(err)
	}

	http.HandleFunc("/api/1/revision/latest", getLastRevision)
	http.HandleFunc("/api/1/commit", postCommit)
	http.HandleFunc("/api/1/stage", stageFiles)
	http.HandleFunc("/api/health", getHealth)

	http.ListenAndServe(":80", nil)
}
