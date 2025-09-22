set -e
PROJECT_DIR=$1
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

    git init --initial-branch=master
    git config --local user.name "tester"
    git config --local user.email "test@mail.com"

    git add README.md
    git commit -m "Initial Commit"

    git clone .git $REMOTE_REPO --bare
    git remote add origin $REMOTE_REPO
}

mkdir -p "$PROJECT_DIR/config"
mkdir -p "$PROJECT_DIR/repo-store"


echo "[{\"repo_url\":\"/app/remote-repos/test_repo.git\"}]" > $PROJECT_DIR/config/config.json

prepare_repo > $PROJECT_DIR/server.log 2>&1

echo "--> Launching server"
cd $CWD
./localrun.sh $PROJECT_DIR --no-detach >> $PROJECT_DIR/server.log 2>&1