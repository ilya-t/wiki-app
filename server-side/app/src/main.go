package main

import (
	"fmt"
	"net/http"
)

const (
	CWD = "/app/repo-store/repo"
)

func getHealth(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "OK!")
}

func main() {
	http.HandleFunc("/api/health", getHealth)

	configs, e := loadConfigurations()
	if e != nil {
		panic(e)
	}

	for _, c := range configs {
		p := NewHttpProject(c)
		p.Start()
	}

	fmt.Println("Starting server...")
	http.ListenAndServe(":80", nil)
}
