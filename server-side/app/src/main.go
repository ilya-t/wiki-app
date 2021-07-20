package main

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"os/exec"
	"strings"
)

const (
	CWD = "/app/repo-store/repo"
)

func executeAt(cmd string, cwd string) (string, error) {
	args := strings.Split(cmd, " ")
	// command := exec.Command("cd " + CWD + " && " + cmd)
	command := exec.Command(args[0], args[1:]...)
	command.Dir = cwd
	out, err := command.Output()

	return string(out), err
}

func execute(cmd string) (string, error) {
	return executeAt(cmd, CWD)
}

func LastRevision() (string, error) {
	result, err := execute("git rev-parse HEAD~0")

	if err != nil {
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

func writeError(w http.ResponseWriter, msg string, err error, extra string) {
	fmt.Fprintf(w, "{\"error\": \"%s - %v\noutput:%v\" }", msg, err, extra)
}

func getHealth(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "OK!")
}

func postCommit(w http.ResponseWriter, req *http.Request) {

}

func headers(w http.ResponseWriter, req *http.Request) {
	for name, headers := range req.Header {
		for _, h := range headers {
			fmt.Fprintf(w, "%v: %v\n", name, h)
		}
	}
}

func main() {
	http.HandleFunc("/api/1/revision/latest", getLastRevision)
	http.HandleFunc("/api/1/commit", postCommit)
	http.HandleFunc("/api/health", getHealth)

	http.ListenAndServe(":80", nil)
}
