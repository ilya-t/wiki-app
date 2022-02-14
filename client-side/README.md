# Android client

## Selective sync api
POST from client:
```json
[
    {
        "name": "incubator",
        "hash": "j32fj0s93", // sha1 from hashes of files
        "files": [
            {
                "name": "README.md",
                "hash": "393jf0w0w" // sha1 from content
            }
        ]
    },
    {
        "name": "README.md",
        "hash": "38320fj202sa"
    }
]
```
By default server will put in response everything that was outdated and not specified 
(folder `inbox` was not specified but it exists in root)

### Quick mode for future:
Server will return zip of actually changed or new files in scope defined by client.
(e.g. folder `inbox` in root will not be synced)
