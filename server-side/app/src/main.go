package main

import (
	"fmt"
	"net/http"
	"os/exec"
	"strings"
)

const (
	cwd = "/app/repo-store/repo"
)

func execute(cmd string) (string, error) {
	args := strings.Split(cmd, " ")
	// command := exec.Command("cd " + cwd + " && " + cmd)
	command := exec.Command(args[0], args[1:]...)
	command.Dir = cwd
	out, err := command.Output()

	return string(out), err
}

func LastRevision() (string, error) {
	result, err := execute("git rev-parse HEAD~0")

	if err != nil {
		return result, err
	}

	return strings.Replace(result, "\n", "", -1), nil
}

func getLatest(w http.ResponseWriter, req *http.Request) {
	r, e := LastRevision()

	if e != nil {
		fmt.Fprintf(w, "Error: %v\nOutput: %v", e, r)
		return
	}

	fmt.Fprintf(w, "{\"revision\": \""+r+"\" }")
}

func getRevision(w http.ResponseWriter, req *http.Request) {

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
	http.HandleFunc("/api/1/get_latest", getLatest)
	http.HandleFunc("/api/1/revision/", getRevision)
	http.HandleFunc("/api/1/commit", postCommit)
	http.HandleFunc("/api/health", getHealth)

	http.ListenAndServe(":80", nil)
}
