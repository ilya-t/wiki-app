package main

import (
	"crypto/sha1"
	"encoding/hex"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"sort"
	"strings"
)

type File struct {
	Name     string  `json:"name"`
	Hash     string  `json:"hash"`
	Files    []*File `json:"files"`
	filesMap map[string]*File
}

func NewDir(name string, hash string, files []*File) *File {
	return &File{
		Name:     name,
		Hash:     hash,
		Files:    files,
		filesMap: nil,
	}
}

func NewFile(name string, hash string) *File {
	return &File{
		Name:     name,
		Hash:     hash,
		Files:    nil,
		filesMap: nil,
	}
}

func (f *File) get(name string) *File {
	if f.filesMap == nil {
		f.filesMap = make(map[string]*File)
		for _, child := range f.Files {
			f.filesMap[child.Name] = child
		}
	}
	return f.filesMap[name]
}

func (f *File) IsDir() bool {
	return f.Files != nil && len(f.Files) > 0
}

func ShowOutdated(reference *File, subject *File) (*File, error) {
	if reference.Name != subject.Name {
		return nil, errors.New("Comparing different structures. Reference has: " + reference.Name + " Subject has: " + subject.Name)
	}

	if reference.Hash == subject.Hash {
		return nil, nil
	}

	files := make([]*File, 0)
	outdated := NewDir(reference.Name, reference.Hash, files)

	for _, ref := range reference.Files {
		subj := subject.get(ref.Name)

		if subj == nil {
			files = append(files, ref)
			continue
		}

		if subj.Hash == ref.Hash {
			continue
		}

		if !subj.IsDir() {
			files = append(files, ref)
			continue
		}

		o, err := ShowOutdated(ref, subj)
		if err != nil {
			return nil, err
		}

		files = append(files, o)
	}

	outdated.Files = files
	return outdated, nil
}

func GenerateStructure(rootDir string) (*File, error) {
	f, e := scan(rootDir)

	if e != nil {
		return nil, e
	}
	return NewDir(".", hashOfDir(f), f), nil
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
				Name:     info.Name(),
				Hash:     hashOfDir(dirFiles),
				Files:    dirFiles,
				filesMap: nil,
			})
		} else {
			hashSum, hashErr := hashOfFile(rootDir + "/" + info.Name())

			if hashErr != nil {
				return nil, hashErr
			}

			results = append(results, &File{
				Name:     info.Name(),
				Hash:     hashSum,
				Files:    nil,
				filesMap: nil,
			})
		}
	}

	return results, e
}

func hashOfDir(dirFiles []*File) string {
	hashes := make([]string, 0)
	for _, f := range dirFiles {
		hashes = append(hashes, f.Hash)
	}
	sort.Strings(hashes)

	h := sha1.New()
	h.Write([]byte(strings.Join(hashes, "")))

	return hex.EncodeToString(h.Sum(nil))
}

type FileHash struct {
	Path string `json:"path"`
	Hash string `json:"hash"`
}

type LocalStatus struct {
	Revision string      `json:"revision"`
	Files    []*FileHash `json:"files"`
}

type NotStaged struct {
	Files []string `json:"files"`
}

func (p *DiffProvider) ShowNotStaged(status *LocalStatus) (*NotStaged, error) {
	notStagedFiles := []string{}

	for _, fileStatus := range status.Files {
		filePath := p.repoDir + "/" + fileStatus.Path

		if _, err := os.Stat(filePath); os.IsNotExist(err) {
			notStagedFiles = append(notStagedFiles, fileStatus.Path)
			continue
		} else if err != nil {
			return nil, fmt.Errorf("error checking file existence %s: %w", filePath, err)
		}

		currentHash, err := hashOfFile(filePath)
		if err != nil {
			return nil, fmt.Errorf("error hashing file %s: %w", filePath, err)
		}

		if currentHash != fileStatus.Hash {
			notStagedFiles = append(notStagedFiles, fileStatus.Path)
		}
	}

	return &NotStaged{Files: notStagedFiles}, nil
}

func hashOfFile(filePath string) (string, error) {
	s, err := ioutil.ReadFile(filePath)

	if err != nil {
		return "", err
	}

	h := sha1.New()
	h.Write(s)

	sha1 := hex.EncodeToString(h.Sum(nil))
	return sha1, nil
}

type DiffProvider struct {
	repoDir   string
	reference *File
}

func (p *DiffProvider) ShowOutdated(files []*File) ([]*File, error) {
	subject := NewDir(".", "", files)

	//TODO: do not update each time
	reference, err := GenerateStructure(p.repoDir)

	if err != nil {
		return nil, err
	}

	p.reference = reference
	result, e := ShowOutdated(p.reference, subject)

	if e != nil {
		return nil, e
	}

	if result == nil {
		return nil, nil
	}
	return result.Files, nil
}
