package main

import (
	"archive/zip"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"strings"
)

type Zipper struct {
	shell *Shell
	cwd   string
}

func (z *Zipper) ZipRepo(dst string) error {
	files := make([]*File, 0)
	walker := func(path string, info fs.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if info.IsDir() {
			return nil
		}

		if strings.Index(path, z.cwd+"/.git") == 0 {
			return nil
		}

		fileName := path[len(z.cwd)+1:]
		f := NewFile(fileName, fileName)
		fmt.Printf("%s -> %+v", fileName, f)
		files = append(files, f)
		return nil
	}

	if e := filepath.Walk(z.cwd, walker); e != nil {
		return e
	}

	return z.ZipSelective(dst, files)
}

func (z *Zipper) ZipSelective(dst string, files []*File) error {
	f, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer f.Close()

	writer := zip.NewWriter(f)
	defer writer.Close()

	for _, f := range files {
		e := z.addToArchive(z.cwd, f, writer)
		if e != nil {
			return e
		}
	}
	return nil
}

func (z *Zipper) addToArchive(fileRoot string, file *File, writer *zip.Writer) error {
	path := fileRoot + "/" + file.Name

	if file.Files != nil || len(file.Files) > 0 {
		for _, f := range file.Files {
			e := z.addToArchive(path, f, writer)

			if e != nil {
				return e
			}
		}
		return nil
	}

	info, e := os.Lstat(path)

	if e != nil {
		return e
	}

	header, err := zip.FileInfoHeader(info)
	if err != nil {
		return err
	}

	// set compression
	header.Method = zip.Deflate

	// let all files lie in separate directory inside archive so we could add something else in future.
	relativePath := "repo/" + path[len(z.cwd)+1:]
	header.Name = relativePath
	if err != nil {
		return err
	}

	// Create writer for the file header and save content of the file
	headerWriter, err := writer.CreateHeader(header)
	if err != nil {
		return err
	}

	if info.IsDir() {
		return nil
	}

	f, err := os.Open(path)
	if err != nil {
		return err
	}
	defer f.Close()

	_, err = io.Copy(headerWriter, f)
	return err
}
