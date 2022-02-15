package main

import (
	"errors"
	"fmt"
	"os/exec"
)

type Zipper struct {
	shell *Shell
	cwd   string
}

func (z *Zipper) ZipRepo(dst string, files []*File) error {
	z.shell.Execute("rm " + dst)
	selectiveOrEmpty := ""
	if files != nil {
		selectiveOrEmpty = " -@ "
	}

	command := "zip " + selectiveOrEmpty + "-r " + dst + " repo --exclude \"repo/.git/*\""
	// executing "zip" through  "/bin/sh -c" cause somehow same "zip" command
	// cannot match excluded files correctly (command.Dir = CWD break everything)
	r, e := exec.Command("/bin/sh", "-c", "cd "+z.cwd+"/.. && "+command).Output()
	fmt.Println("cmd out: ", command, string(r))

	if e != nil {
		extra := "\nzip command: " + command + "\ncommand output: " + string(r)
		return errors.New(e.Error() + "\nExtra: " + extra)
	}
	return nil
}
