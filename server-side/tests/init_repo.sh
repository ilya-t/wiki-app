REPO_DIR=./test_repo
REMOTE_REPO=/tmp/test_repo.git
rm -rf $REPO_DIR
rm -rf $REMOTE_REPO
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
git clone .git $REMOTE_REPO --bare
git remote add origin $REMOTE_REPO
cd ..