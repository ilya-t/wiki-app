REPO_DIR=./test_repo
REMOTE_REPO=/tmp/test_repo.git
rm -rf $REPO_DIR
rm -rf $REMOTE_REPO
set -e

mkdir $REPO_DIR
cd $REPO_DIR
echo "# Sample Repo for Tests" > README.md

git init --initial-branch=master
git config --local user.name "tester"
git config --local user.email "test@mail.com"

git add README.md
git commit -m "Initial Commit"

mkdir content
echo "Some Content (1)" > content/file1
echo "Some Content (2)" > content/file2
git add content
git commit -m "add content dir"

mkdir status
echo "# Git status tests dir" > status/README.md
git add status
git commit -m "add status dir"

echo "do not modify this file! is's sha1 is used under tests " > file_with_fixed_sha1 # 0758fe8844f102aaa616c30c94ea4f8eb9326b06
git add file_with_fixed_sha1
git commit -m "add file_with_fixed_sha1"

echo "Test repo initialized at "$REPO_DIR" with head commit:"
git log -1
git clone .git $REMOTE_REPO --bare
git remote add origin $REMOTE_REPO
cd ..