package main

import (
	"encoding/base64"
	"errors"
	"io/ioutil"
	"os"
	"strings"
)

type Staging struct {
	Files []*FileContent `json:"files"`
}

type FileContent struct {
	Path    string `json:"path"`
	Content string `json:"content"`
}

type Git struct {
	repoDir string
	shell   *Shell
}

func NewGit(dir string) *Git {
	return &Git{
		repoDir: dir,
		shell:   &Shell{dir}}
}

func (g *Git) LastRevision() (string, error) {
	if _, err := os.Stat(g.repoDir + "/.git"); os.IsNotExist(err) {
		return "", errors.New("repo's .git not found!")
	}

	result, err := g.shell.Execute("git rev-parse HEAD~0")

	if err != nil {
		g.shell.PrintOutput("pwd")
		g.shell.PrintOutput("ls -l")
		g.shell.PrintOutput("git log -1")
		return result, err
	}

	return strings.Replace(result, "\n", "", -1), nil
}

func (g *Git) Stage(f *FileContent) {
	decoded, _ := base64.StdEncoding.DecodeString(f.Content)
	filePath := g.repoDir + "/" + f.Path
	e := ioutil.WriteFile(filePath, []byte(decoded), os.ModePerm)

	if e != nil {
		panic(e)
	}

	_, err := g.shell.Execute("git add " + filePath)

	if err != nil {
		panic(e)
	}

}
