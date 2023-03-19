package main

import (
	"bytes"
	"fmt"
	"os/exec"
)

type Shell struct {
	Cwd string
}

func (s *Shell) Execute(cmd string) (string, error) {
	command := exec.Command("/bin/sh", "-c", cmd)
	command.Dir = s.Cwd
	var stderr bytes.Buffer
	command.Stderr = &stderr
	out, err := command.Output()

	if err != nil {
		return string(out) + stderr.String(), err
	}

	return string(out), err
}

func (s *Shell) PrintOutput(cmd string) {
	out, err := s.Execute(cmd)
	if err != nil {
		fmt.Println("cmd: ", cmd, "\noutput: ", "ERROR("+err.Error()+"): "+out)
		return
	}

	fmt.Println("cmd: ", cmd, "\noutput: ", out)
}

func (s *Shell) StrictExecute(cmd string) {
	out, err := s.Execute(cmd)

	if err != nil {
		panic("Error executing command: \n" + cmd + "\nError: \n" + err.Error() + "\nOutput:\n" + out)
	}
}
