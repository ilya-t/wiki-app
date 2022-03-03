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

	configurations := toConfigurations(configs)

	if len(configurations) == 0 {
		defaultConfig, e := loadDefaultConfiguration()
		if e != nil {
			panic(e)
		}
		configurations = append(configurations, defaultConfig)
	}

	for _, c := range configurations {
		p := NewHttpProject(c)
		p.Start()
	}

	projectApi := &ProjectApi{
		Configs: configs,
	}
	projectApi.Start()

	fmt.Println("Starting server...")
	http.ListenAndServe(":80", nil)
}
