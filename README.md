# ispengestopp

ispengestopp is a microservice application that notifies when a Søknad should be
removed from automatic processing.

The no automatic processing state is set for a person for each work place, stored
in a database and posted on a kafka queue.

## Technologies used
* Kotlin
* Ktor
* Gradle
* JDK 13
* Spek
* Gson

#### Requirements
* Adopt-OpenJDK 13

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

## Contact us
### Code/project related questions can be sent to
* June Henriksen, `june.henriksen2@nav.no`
* Mathias Rørvik, `mathias.fris.rorvik@nav.no`
* John Martin Lindseth `john.martin.lindseth@nav.no`
* The following channel on slack --> #isyfo
