REPO_DIR=./test_repo

rm -rf $REPO_DIR
set -e

mkdir $REPO_DIR
cd $REPO_DIR
echo "# Sample Repo for Tests" > README.md

git init
git config --local user.name "tester"
git config --local user.email "test@mail.com"

git add README.md
git commit -m "Initial Commit"

echo "Test repo initialized at "$REPO_DIR" with head commit:"
git log -1
cd ..