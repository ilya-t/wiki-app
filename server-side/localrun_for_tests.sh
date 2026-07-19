set -e
PROJECT_DIR=$1
ALIAS=$2
CWD=$(pwd)

if [ "$PROJECT_DIR" == "" ]; then
    echo "Specify path to volumes as first arg: ./localrun_for_tests.sh /some/path "
    exit 1
fi


REPO_DIR=$PROJECT_DIR/test_repo
REMOTE_REPOS_DIR=$PROJECT_DIR/remote-repos
REMOTE_REPO=$REMOTE_REPOS_DIR/test_repo.git

prepare_repo() {
    echo "--> Preparing repository in $REPO_DIR"

    mkdir -p "$REMOTE_REPOS_DIR"
    mkdir -p $REPO_DIR
    cd $REPO_DIR
    echo "# Sample Repo for Tests" > $REPO_DIR/README.md

    # Create install-hook.sh script that will be invoked by cmdAfterClone
    # The script installs a pre-commit hook that appends a line to README.md on every commit.
    echo '#!/bin/sh' > $REPO_DIR/install-hook.sh
    echo "printf '#!/bin/sh\necho \"appended by hook\" >> README.md\ngit add README.md\n' > .git/hooks/pre-commit" >> $REPO_DIR/install-hook.sh
    echo 'chmod +x .git/hooks/pre-commit' >> $REPO_DIR/install-hook.sh
    chmod +x $REPO_DIR/install-hook.sh

    # git init --initial-branch is unavailable on Git in the client-side CI image (Ubuntu 20.04).
    git init
    git checkout -b master
    git config --local user.name "tester"
    git config --local user.email "test@mail.com"

    git add README.md install-hook.sh
    git commit -m "Initial Commit"

    git clone .git $REMOTE_REPO --bare
    git remote add origin $REMOTE_REPO
}

mkdir -p "$PROJECT_DIR/config"
mkdir -p "$PROJECT_DIR/repo-store"


echo "[{\"repo_url\":\"/app/remote-repos/test_repo.git\",\"repo_cmd_after_clone\":\"sh install-hook.sh\"}]" > $PROJECT_DIR/config/config.json

prepare_repo > $PROJECT_DIR/server.log 2>&1

echo "--> Launching server"
cd $CWD
./localrun.sh $PROJECT_DIR --detach "$ALIAS" >> $PROJECT_DIR/server.log 2>&1
