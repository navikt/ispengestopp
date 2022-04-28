# ispengestopp

ispengestopp is a microservice application that notifies when a Søknad should be
removed from automatic processing.

The no automatic processing state is set for a person for each work place, stored
in a database and posted on a kafka queue.

## Technologies used
* Kotlin
* Ktor
* Gradle
* JDK 17
* Spek

#### Build and run tests
To build locally and run the integration tests you can simply run `./gradlew test`

#### Lint (Ktlint)
##### Command line
Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`
##### Git Hooks
Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`

#### Running locally
`docker compose up`

## Contact

### For NAV employees

We are available at the Slack channel `#isyfo`.
