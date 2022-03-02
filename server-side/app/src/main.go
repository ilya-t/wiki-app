package main

import (
	"fmt"
	"net/http"
	"os"
)

const (
	CWD = "/app/repo-store/repo"
)

func getHealth(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "OK!")
}

func main() {
	repoLink := os.Getenv(REPO_LINK_VAR)

	if repoLink == "" {
		panic("Env.variable not defined: " + REPO_LINK_VAR + ". Pass repo link for cloing")
	}

	p := NewHttpProject(CWD, repoLink)
	p.Start()
	http.HandleFunc("/api/health", getHealth)

	fmt.Println("Starting server...")
	http.ListenAndServe(":80", nil)
}
