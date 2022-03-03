package main

import (
	"fmt"
	"net/http"
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

	if len(configs) == 0 {
		panic("No configurations could be loaded from '" + CONFIG_FILE + "'")
	}

	for _, c := range configs {
		p := NewHttpProject(toConfiguration(c))
		p.Start()
	}

	projectApi := &ProjectApi{
		Configs: configs,
	}
	projectApi.Start()

	fmt.Println("Starting server...")
	http.ListenAndServe(":80", nil)
}
