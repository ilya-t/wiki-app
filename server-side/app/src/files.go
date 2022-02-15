package main

import (
	"crypto/sha1"
	"errors"
	"fmt"
	"io/ioutil"
	"sort"
	"strings"
)

type File struct {
	name     string  `json:"name"`
	hash     string  `json:"hash"`
	files    []*File `json:"files"`
	filesMap map[string]*File
}

func NewFile(name string, hash string, files []*File) *File {
	return &File{
		name:     name,
		hash:     hash,
		files:    files,
		filesMap: nil,
	}
}

func (f *File) get(name string) *File {
	if f.filesMap == nil {
		m := make(map[string]*File)
		for _, f := range f.files {
			m[f.name] = f
		}
		f.filesMap = m
	}
	return f.filesMap[name]
}

func ShowOutdated(reference *File, subject *File) (*File, error) {
	if reference.name != subject.name {
		return nil, errors.New("Comparing different structures. Reference has: " + reference.name + " Subject has: " + subject.name)
	}

	if reference.hash == subject.hash {
		return nil, nil
	}

	files := make([]*File, 0)
	outdated := &File{
		name:  reference.name,
		hash:  reference.hash,
		files: files,
	}

	for _, ref := range reference.files {
		subj := subject.get(ref.name)

		if subj == nil {
			files = append(files, ref)
			continue
		}

		if subj.hash == ref.hash {
			continue
		}

		o, err := ShowOutdated(ref, subj)
		if err != nil {
			return nil, err
		}

		files = append(files, o)
	}

	fmt.Println("files:", files)
	outdated.files = files
	return outdated, nil
}

func GenerateStructure(rootDir string) (*File, error) {
	f, e := scan(rootDir)

	if e != nil {
		return nil, e
	}
	return &File{
		name:  ".",
		hash:  "",
		files: f,
	}, nil
}

func scan(rootDir string) ([]*File, error) {
	results := make([]*File, 0)

	f, e := ioutil.ReadDir(rootDir)
	if e != nil {
		return nil, e
	}

	for _, info := range f {
		if strings.ToLower(info.Name()) == ".git" {
			continue
		}

		if info.IsDir() {
			dirFiles, err := scan(rootDir + "/" + info.Name())

			if err != nil {
				return nil, err
			}

			results = append(results, &File{
				name:     info.Name(),
				hash:     hashOfDir(dirFiles),
				files:    dirFiles,
				filesMap: nil,
			})
		} else {
			hashSum, hashErr := hashOfFile(rootDir + "/" + info.Name())

			if hashErr != nil {
				return nil, hashErr
			}

			results = append(results, &File{
				name:     info.Name(),
				hash:     hashSum,
				files:    nil,
				filesMap: nil,
			})
		}
	}

	return results, e
}

func hashOfDir(dirFiles []*File) string {
	hashes := make([]string, 0)
	for _, f := range dirFiles {
		hashes = append(hashes, f.hash)
	}
	sort.Strings(hashes)

	h := sha1.New()
	h.Write([]byte(strings.Join(hashes, "")))

	return string(h.Sum(nil))
}

func hashOfFile(filePath string) (string, error) {
	s, err := ioutil.ReadFile(filePath)

	if err != nil {
		return "", err
	}

	h := sha1.New()
	h.Write(s)
	return string(h.Sum(nil)), nil
}

type DiffProvider struct {
	repoDir   string
	reference *File
}

func (p *DiffProvider) ShowOutdated(files []*File) ([]*File, error) {
	subject := &File{
		name:  ".",
		hash:  "",
		files: files,
	}

	//TODO: do not update each time
	reference, err := GenerateStructure(p.repoDir)

	if err != nil {
		return nil, err
	}

	p.reference = reference
	result, e := ShowOutdated(p.reference, subject)
	return result.files, e
}
