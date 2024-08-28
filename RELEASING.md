- Edit `project/Build` to name the new version
- Edit README.md to name the new version
- Edit CHANGELOG.md
- git commit, git push
- `git tag` and `git push --tags`
- make sure `~/.gnupg/gpg.conf` lists the proper key as the default one (check with `gpg --list-key`)
- `export GPG_TTY=$(tty)` (Mac only)
- publish using various Java versions (the code uses sdkman):
```
sdk use java 8.0.392-amzn
sbt publishSigned
sbt slack-scala-client-models/publishSigned

sdk use java 11.0.23-amzn
sbt ++2.13.14 publishSigned
sbt ++2.13.14 slack-scala-client-models/publishSigned
sbt ++3.5.0 publishSigned
sbt ++3.5.0 slack-scala-client-models/publishSigned
```
- In Sonatype Close the staging repository, and Release it.
