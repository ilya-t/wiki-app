# Quick start
Prepare app data directory with structure.
- `/your_app_dir/repo-store`
- `/your_app_dir/config/config.json`:
```json
[
    {
        "name": "notes",
        "repo_url": "git@github.com:username/my-notes.git"
    }
]
```

run: `./localrun.sh /your_app_dir`
app will use ssh keys from host machine.
